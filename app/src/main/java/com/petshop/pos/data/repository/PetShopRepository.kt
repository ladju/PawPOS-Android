package com.petshop.pos.data.repository

import androidx.room.withTransaction
import com.petshop.pos.data.local.PetShopDatabase
import com.petshop.pos.data.local.dao.TopSellerSummary
import com.petshop.pos.data.local.entity.*
import com.petshop.pos.domain.CartCalculator
import com.petshop.pos.domain.PricedLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.*

/** A cart line resolved against the *live* product row. Never persisted. */
data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1
) {
    val subtotal: Long
        get() = product.sellingPrice * quantity
}

/** What the cashier is selling: ids and quantities only. Prices/stock are always re-read inside the transaction. */
data class CheckoutLine(val productId: Long, val quantity: Int)

data class CheckoutReceipt(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>
)

/** A problem the cashier can understand and fix (stock, tender, missing product). Message is user-facing. */
class CheckoutException(message: String) : Exception(message)

/** Live numbers for one reporting period. Money is whole rupiah. */
data class PeriodStats(
    val revenue: Long = 0,
    val grossProfit: Long = 0,
    val discountTotal: Long = 0,
    val transactionCount: Int = 0,
    val itemsSold: Int = 0
)

class PetShopRepository(
    private val database: PetShopDatabase
) {
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val stockMovementDao = database.stockMovementDao()

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val totalTransactionsCount: Flow<Int> = transactionDao.getTotalTransactionsCount()

    fun getProductsByCategory(category: PetCategory): Flow<List<ProductEntity>> {
        return if (category == PetCategory.ALL) {
            productDao.getAllProducts()
        } else {
            productDao.getProductsByCategory(category)
        }
    }

    fun getProductsByType(type: ProductType): Flow<List<ProductEntity>> {
        return if (type == ProductType.ALL) {
            productDao.getAllProducts()
        } else {
            productDao.getProductsByType(type)
        }
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun getProductById(id: Long): ProductEntity? {
        return productDao.getProductById(id)
    }

    /**
     * Creates or updates a product. Throws [IllegalArgumentException] (user-facing message) on invalid data
     * or a barcode that already belongs to another active product.
     */
    suspend fun saveProduct(product: ProductEntity): Long {
        require(product.name.isNotBlank()) { "Product name is required." }
        require(product.barcode.isNotBlank()) { "Barcode is required." }
        require(product.costPrice >= 0 && product.sellingPrice >= 0) { "Prices cannot be negative." }
        require(product.currentStock >= 0 && product.minStockAlert >= 0) { "Stock values cannot be negative." }

        return database.withTransaction {
            val clash = productDao.getProductByBarcode(product.barcode)
            require(clash == null || clash.id == product.id) {
                "Barcode ${product.barcode} already belongs to '${clash?.name}'."
            }
            if (product.id == 0L) {
                val id = productDao.insertProduct(product)
                if (product.currentStock > 0) {
                    stockMovementDao.insertMovement(
                        StockMovementEntity(
                            productId = id,
                            productName = product.name,
                            quantityChange = product.currentStock,
                            previousStock = 0,
                            resultingStock = product.currentStock,
                            reason = StockChangeReason.RESTOCK,
                            note = "Initial product catalog setup"
                        )
                    )
                }
                id
            } else {
                val before = productDao.getProductById(product.id)
                productDao.updateProduct(product)
                // A stock edited through the product form must still leave an audit trail.
                if (before != null && before.currentStock != product.currentStock) {
                    stockMovementDao.insertMovement(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            quantityChange = product.currentStock - before.currentStock,
                            previousStock = before.currentStock,
                            resultingStock = product.currentStock,
                            reason = StockChangeReason.AUDIT,
                            note = "Edited in product form"
                        )
                    )
                }
                product.id
            }
        }
    }

    /**
     * Atomically changes stock. Returns false (and changes nothing) if the product is missing or the
     * adjustment would take stock below zero, so the ledger always equals the real stock.
     */
    suspend fun adjustStock(
        productId: Long,
        delta: Int,
        reason: StockChangeReason,
        note: String
    ): Boolean = database.withTransaction {
        val product = productDao.getProductById(productId) ?: return@withTransaction false
        val newStock = product.currentStock + delta
        if (newStock < 0) return@withTransaction false

        productDao.updateStock(productId, newStock)
        stockMovementDao.insertMovement(
            StockMovementEntity(
                productId = productId,
                productName = product.name,
                quantityChange = delta,
                previousStock = product.currentStock,
                resultingStock = newStock,
                reason = reason,
                note = note
            )
        )
        true
    }

    /** Soft delete: sales history and the stock ledger stay intact. */
    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deactivate(product.id)
    }

    /**
     * Atomic POS checkout. Everything below happens in ONE database transaction; if anything fails
     * nothing is saved:
     *  1. every product is re-read from the database (live price + live stock, never a stale cart snapshot)
     *  2. stock and tender are validated
     *  3. totals come from [CartCalculator], the same code the cart UI uses
     *  4. sale + line items + stock deduction + stock ledger are written together
     */
    suspend fun processCheckout(
        lines: List<CheckoutLine>,
        paymentMethod: PaymentMethod,
        discountAmount: Long = 0,
        taxPercent: Int = 0,
        cashTendered: Long = 0,
        customerName: String? = null,
        customerPhone: String? = null,
        cashierName: String = "Staff",
        notes: String = ""
    ): Result<CheckoutReceipt> {
        if (lines.isEmpty()) return Result.failure(CheckoutException("Cart is empty."))

        return try {
            val receipt = database.withTransaction {
                val wanted = lines.groupBy { it.productId }.mapValues { (_, l) -> l.sumOf { it.quantity } }

                val resolved = wanted.map { (id, qty) ->
                    if (qty <= 0) throw CheckoutException("Invalid quantity in cart.")
                    val product = productDao.getProductById(id)?.takeIf { it.isActive }
                        ?: throw CheckoutException("A product in the cart is no longer available.")
                    if (!product.isService && product.currentStock < qty) {
                        throw CheckoutException(
                            "Not enough stock for ${product.name}: ${product.currentStock} left, $qty in cart."
                        )
                    }
                    product to qty
                }

                val isCash = paymentMethod == PaymentMethod.CASH
                val totals = CartCalculator.totals(
                    resolved.map { (p, q) -> PricedLine(p.sellingPrice, q) },
                    discountAmount,
                    taxPercent
                )
                if (!CartCalculator.isTenderSufficient(isCash, totals.grandTotal, cashTendered)) {
                    throw CheckoutException("Cash received is less than the total.")
                }

                val now = System.currentTimeMillis()
                val invoiceNumber = nextInvoiceNumber(now)

                val transaction = TransactionEntity(
                    invoiceNumber = invoiceNumber,
                    timestamp = now,
                    subtotal = totals.subtotal,
                    taxRate = totals.taxPercent / 100.0,
                    taxAmount = totals.taxAmount,
                    discountAmount = totals.discount,
                    grandTotal = totals.grandTotal,
                    paymentMethod = paymentMethod,
                    cashTendered = if (isCash) cashTendered else 0L,
                    changeGiven = CartCalculator.changeDue(isCash, totals.grandTotal, cashTendered),
                    customerName = customerName,
                    customerPhone = customerPhone,
                    cashierName = cashierName,
                    notes = notes
                )
                val transactionId = transactionDao.insertTransaction(transaction)

                transactionDao.insertTransactionItems(
                    resolved.map { (p, q) ->
                        TransactionItemEntity(
                            transactionId = transactionId,
                            productId = p.id,
                            productName = p.name,
                            productBarcode = p.barcode,
                            category = p.category,
                            unitPrice = p.sellingPrice,
                            costPrice = p.costPrice,
                            quantity = q,
                            subtotal = p.sellingPrice * q
                        )
                    }
                )

                for ((p, q) in resolved) {
                    if (p.isService) continue
                    val newStock = p.currentStock - q
                    productDao.updateStock(p.id, newStock)
                    stockMovementDao.insertMovement(
                        StockMovementEntity(
                            productId = p.id,
                            productName = p.name,
                            quantityChange = -q,
                            previousStock = p.currentStock,
                            resultingStock = newStock,
                            reason = StockChangeReason.SALE,
                            referenceInvoice = invoiceNumber,
                            note = "POS Checkout #$invoiceNumber"
                        )
                    )
                }

                CheckoutReceipt(
                    transaction = transaction.copy(id = transactionId),
                    items = transactionDao.getItemsForTransaction(transactionId)
                )
            }
            Result.success(receipt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: CheckoutException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CheckoutException("Checkout failed and nothing was saved. Please try again."))
        }
    }

    /** INV-yyyyMMdd-0001, 0002, ... per calendar day; guaranteed unique (also enforced by a unique index). */
    private suspend fun nextInvoiceNumber(now: Long): String {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = dayStart.timeInMillis
        dayStart.add(Calendar.DAY_OF_YEAR, 1)
        val end = dayStart.timeInMillis

        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
        var seq = transactionDao.countBetween(start, end) + 1
        var candidate: String
        do {
            candidate = "INV-$day-${seq.toString().padStart(4, '0')}"
            seq++
        } while (transactionDao.countByInvoice(candidate) > 0)
        return candidate
    }

    suspend fun getTransactionItems(transactionId: Long): List<TransactionItemEntity> {
        return transactionDao.getItemsForTransaction(transactionId)
    }

    /** Live stats for [since, until). Re-emits whenever a sale is recorded. */
    fun observePeriodStats(since: Long, until: Long): Flow<PeriodStats> = combine(
        transactionDao.observeBetween(since, until),
        transactionDao.observeItemProfitBetween(since, until),
        transactionDao.observeItemsSoldBetween(since, until)
    ) { transactions, itemProfit, itemsSold ->
        val discounts = transactions.sumOf { it.discountAmount }
        PeriodStats(
            revenue = transactions.sumOf { it.grandTotal },
            // Profit is what the shop actually keeps: item margin minus discounts given. It can be negative.
            grossProfit = itemProfit - discounts,
            discountTotal = discounts,
            transactionCount = transactions.size,
            itemsSold = itemsSold
        )
    }

    fun observeTransactions(since: Long, until: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(since, until)

    fun observeTopSellers(since: Long, until: Long, limit: Int = 5): Flow<List<TopSellerSummary>> =
        transactionDao.observeTopSellersBetween(since, until, limit)

    data class CsvImportSummary(
        val addedCount: Int,
        val updatedCount: Int
    )

    suspend fun upsertProductsFromCsv(products: List<ProductEntity>): CsvImportSummary {
        var added = 0
        var updated = 0

        database.withTransaction {
            for (incoming in products) {
                val existing = productDao.getProductByBarcode(incoming.barcode)
                if (existing != null) {
                    val updatedEntity = incoming.copy(
                        id = existing.id,
                        imageUri = existing.imageUri,
                        isActive = true
                    )
                    productDao.updateProduct(updatedEntity)
                    if (existing.currentStock != incoming.currentStock) {
                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                productId = existing.id,
                                productName = updatedEntity.name,
                                quantityChange = incoming.currentStock - existing.currentStock,
                                previousStock = existing.currentStock,
                                resultingStock = incoming.currentStock,
                                reason = StockChangeReason.AUDIT,
                                note = "CSV inventory import"
                            )
                        )
                    }
                    updated++
                } else {
                    val newId = productDao.insertProduct(incoming)
                    if (incoming.currentStock > 0) {
                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                productId = newId,
                                productName = incoming.name,
                                quantityChange = incoming.currentStock,
                                previousStock = 0,
                                resultingStock = incoming.currentStock,
                                reason = StockChangeReason.RESTOCK,
                                note = "CSV inventory import"
                            )
                        )
                    }
                    added++
                }
            }
        }

        return CsvImportSummary(addedCount = added, updatedCount = updated)
    }
}

