package com.petshop.pos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petshop.pos.data.local.PetShopDatabase
import com.petshop.pos.data.local.entity.PaymentMethod
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.ProductType
import com.petshop.pos.data.repository.CheckoutException
import com.petshop.pos.data.repository.CheckoutLine
import com.petshop.pos.data.repository.PetShopRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real checkout transaction against an in-memory Room database (no seed data). */
@RunWith(AndroidJUnit4::class)
class CheckoutRepositoryTest {

    private lateinit var db: PetShopDatabase
    private lateinit var repo: PetShopRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetShopDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PetShopRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun addKibble(barcode: String = "111", stock: Int = 10, price: Long = 15_000L): Long =
        repo.saveProduct(
            ProductEntity(
                barcode = barcode, name = "Test Kibble $barcode", category = PetCategory.DOGS,
                productType = ProductType.DRY_CAT_FOOD, costPrice = 10_000L, sellingPrice = price, currentStock = stock
            )
        )

    private suspend fun stockOf(id: Long) = db.productDao().getProductById(id)!!.currentStock
    private suspend fun saleCount() = db.transactionDao().countBetween(0L, Long.MAX_VALUE)

    @Test
    fun cashSaleDeductsStockAndReturnsExactChange() = runBlocking {
        val id = addKibble(stock = 10)
        val receipt = repo.processCheckout(listOf(CheckoutLine(id, 3)), PaymentMethod.CASH, cashTendered = 50_000L).getOrThrow()

        assertEquals(45_000L, receipt.transaction.grandTotal)
        assertEquals(5_000L, receipt.transaction.changeGiven)
        assertEquals(1, receipt.items.size)
        assertEquals(7, stockOf(id))
    }

    @Test
    fun oversellIsRejectedAndNothingIsWritten() = runBlocking {
        val id = addKibble(stock = 2)
        val result = repo.processCheckout(listOf(CheckoutLine(id, 5)), PaymentMethod.CASH, cashTendered = 999_999L)

        assertTrue(result.exceptionOrNull() is CheckoutException)
        assertEquals(2, stockOf(id))
        assertEquals(0, saleCount())
    }

    @Test
    fun underpaidCashIsRejectedAndStockIsUntouched() = runBlocking {
        val id = addKibble(stock = 10)
        val result = repo.processCheckout(listOf(CheckoutLine(id, 2)), PaymentMethod.CASH, cashTendered = 29_999L)

        assertTrue(result.exceptionOrNull() is CheckoutException)
        assertEquals(10, stockOf(id))
        assertEquals(0, saleCount())
    }

    @Test
    fun qrisNeedsNoTenderAndGivesNoChange() = runBlocking {
        val id = addKibble()
        val receipt = repo.processCheckout(listOf(CheckoutLine(id, 1)), PaymentMethod.QRIS, cashTendered = 0L).getOrThrow()

        assertEquals(0L, receipt.transaction.changeGiven)
        assertEquals(15_000L, receipt.transaction.grandTotal)
    }

    @Test
    fun discountIsCappedAndTaxIsAppliedAfterDiscount() = runBlocking {
        val id = addKibble(price = 100_000L)
        val taxed = repo.processCheckout(
            listOf(CheckoutLine(id, 1)), PaymentMethod.QRIS, discountAmount = 10_000L, taxPercent = 11
        ).getOrThrow()
        assertEquals(99_900L, taxed.transaction.grandTotal) // (100_000 - 10_000) * 1.11

        val huge = repo.processCheckout(
            listOf(CheckoutLine(id, 1)), PaymentMethod.QRIS, discountAmount = 9_999_999L
        ).getOrThrow()
        assertEquals(0L, huge.transaction.grandTotal)
        assertEquals(100_000L, huge.transaction.discountAmount)
    }

    @Test
    fun priceChangedAfterAddingToCartIsUsedAtCheckout() = runBlocking {
        val id = addKibble(price = 15_000L)
        val product = db.productDao().getProductById(id)!!
        repo.saveProduct(product.copy(sellingPrice = 20_000L)) // price edited while item sat in the cart
        val receipt = repo.processCheckout(listOf(CheckoutLine(id, 1)), PaymentMethod.QRIS).getOrThrow()

        assertEquals(20_000L, receipt.transaction.grandTotal)
    }

    @Test
    fun invoiceNumbersAreUniqueEvenWithinTheSameSecond() = runBlocking {
        val id = addKibble(stock = 50)
        val invoices = (1..6).map {
            repo.processCheckout(listOf(CheckoutLine(id, 1)), PaymentMethod.QRIS).getOrThrow().transaction.invoiceNumber
        }
        assertEquals(invoices.size, invoices.toSet().size)
    }

    @Test
    fun serviceItemsNeverNeedStock() = runBlocking {
        val id = repo.saveProduct(
            ProductEntity(
                barcode = "SVC1", name = "Grooming", category = PetCategory.SERVICES,
                productType = ProductType.GROOMING, costPrice = 0L, sellingPrice = 50_000L, currentStock = 0
            )
        )
        val receipt = repo.processCheckout(listOf(CheckoutLine(id, 2)), PaymentMethod.QRIS).getOrThrow()
        assertEquals(100_000L, receipt.transaction.grandTotal)
    }

    @Test
    fun stockAdjustmentCannotGoNegative() = runBlocking {
        val id = addKibble(stock = 3)
        val ok = repo.adjustStock(id, -99, com.petshop.pos.data.local.entity.StockChangeReason.AUDIT, "test")
        assertFalse(ok)
        assertEquals(3, stockOf(id))
    }

    @Test
    fun duplicateBarcodeIsRejected() = runBlocking {
        addKibble(barcode = "DUP")
        val result = runCatching { addKibble(barcode = "DUP") }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun deletingAProductKeepsSalesHistory() = runBlocking {
        val id = addKibble()
        val receipt = repo.processCheckout(listOf(CheckoutLine(id, 1)), PaymentMethod.QRIS).getOrThrow()
        repo.deleteProduct(db.productDao().getProductById(id)!!)

        assertNotNull(db.transactionDao().getTransactionById(receipt.transaction.id))
        assertEquals(1, db.transactionDao().getItemsForTransaction(receipt.transaction.id).size)
        assertFalse(db.productDao().getProductById(id)!!.isActive)
    }
}
