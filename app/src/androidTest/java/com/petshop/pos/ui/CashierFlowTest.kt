package com.petshop.pos.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.petshop.pos.data.SettingsStore
import com.petshop.pos.data.local.PetShopDatabase
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.ProductType
import com.petshop.pos.data.repository.PetShopRepository
import com.petshop.pos.ui.screens.PosScreen
import com.petshop.pos.ui.theme.EnglishStrings
import com.petshop.pos.ui.theme.LocalAppStrings
import com.petshop.pos.ui.theme.PetShopPOSTheme
import com.petshop.pos.ui.viewmodel.PosViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end cashier flow on the real PosScreen with an in-memory database:
 * add item -> review cart -> checkout -> cash validation -> complete -> receipt.
 */
@RunWith(AndroidJUnit4::class)
class CashierFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: PetShopDatabase
    private lateinit var repo: PetShopRepository
    private lateinit var viewModel: PosViewModel
    private var productId = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetShopDatabase::class.java).allowMainThreadQueries().build()
        repo = PetShopRepository(db)
        productId = runBlocking {
            repo.saveProduct(
                ProductEntity(
                    barcode = "UI-1", name = "UI Test Kibble", category = PetCategory.DOGS,
                    productType = ProductType.DRY_CAT_FOOD, costPrice = 10_000L, sellingPrice = 15_000L, currentStock = 5
                )
            )
        }
        viewModel = PosViewModel(repo, SettingsStore(context))
        composeRule.setContent {
            PetShopPOSTheme {
                CompositionLocalProvider(LocalAppStrings provides EnglishStrings) {
                    PosScreen(viewModel = viewModel, onNavigateToScanner = {})
                }
            }
        }
    }

    @After
    fun tearDown() = db.close()

    private fun waitForReceipt() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("new_sale").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("new_sale").assertIsEnabled()
    }

    @Test
    fun cashSaleBlocksShortPaymentThenCompletes() {
        composeRule.onNodeWithTag("product_$productId").performClick()
        composeRule.onNodeWithTag("cart_pill").performClick()
        composeRule.onNodeWithTag("proceed_to_payment").performClick()

        // Cash starts pre-filled with the exact total, so the sale could be completed right away...
        composeRule.onNodeWithTag("complete_sale").assertIsEnabled()

        // ...but a payment below the total (Rp 15.000) must block it.
        composeRule.onNodeWithTag("cash_input").performTextReplacement("1")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("complete_sale").assertIsNotEnabled()

        // A sufficient payment completes the sale and shows the receipt.
        composeRule.onNodeWithTag("cash_input").performTextReplacement("20000")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("complete_sale").assertIsEnabled().performClick()
        waitForReceipt()
    }

    @Test
    fun qrisSaleRequiresExplicitPaymentConfirmation() {
        composeRule.onNodeWithTag("product_$productId").performClick()
        composeRule.onNodeWithTag("cart_pill").performClick()
        composeRule.onNodeWithTag("proceed_to_payment").performClick()

        composeRule.onNodeWithText("📲 QRIS").performClick()
        composeRule.onNodeWithTag("complete_sale").assertIsNotEnabled()

        composeRule.onNodeWithText("I have seen the payment in my merchant app").performClick()
        composeRule.onNodeWithTag("complete_sale").assertIsEnabled().performClick()
        waitForReceipt()
    }
}
