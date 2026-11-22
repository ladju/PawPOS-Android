package com.petshop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petshop.pos.data.SettingsStore
import com.petshop.pos.data.local.entity.PaymentMethod
import com.petshop.pos.data.local.entity.PetCategory
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.ProductType
import com.petshop.pos.data.local.entity.TransactionEntity
import com.petshop.pos.data.local.entity.TransactionItemEntity
import com.petshop.pos.data.repository.CartItem
import com.petshop.pos.data.repository.CheckoutLine
import com.petshop.pos.data.repository.PetShopRepository
import com.petshop.pos.domain.CartCalculator
import com.petshop.pos.domain.CartTotals
import com.petshop.pos.domain.Money
import com.petshop.pos.domain.PricedLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One immutable snapshot of everything the cashier UI shows.
 *
 * [cartQuantities] (productId -> qty) is the ONLY stored cart data. [cartItems] and every total are derived
 * from it plus the live product table and persisted settings, so stock edits, price edits and settings
 * changes made on other screens show up in the cart immediately.
 */
data class PosUiState(
    val selectedCategory: PetCategory = PetCategory.ALL,
    val selectedType: ProductType = ProductType.ALL,
    val searchQuery: String = "",
    val cartQuantities: Map<Long, Int> = emptyMap(),
    val cartItems: List<CartItem> = emptyList(),
    val discountAmount: Long = 0,
    val taxPercent: Int = 0, // 0 = tax not included (small business / UMKM default)
    val qrisMdrRate: Double = 0.003,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val cashTendered: Long = 0,
    /** Optional bank/e-wallet reference typed by the cashier after checking their merchant app. Never generated. */
    val qrisReference: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val isCheckoutDialogOpen: Boolean = false,
    val isReceiptDialogOpen: Boolean = false,
    val isProcessing: Boolean = false,
    val lastTransaction: TransactionEntity? = null,
    val lastTransactionItems: List<TransactionItemEntity> = emptyList(),
    val errorMessage: String? = null
) {
    val totals: CartTotals = CartCalculator.totals(
        cartItems.map { PricedLine(it.product.sellingPrice, it.quantity) },
        discountAmount,
        taxPercent
    )

    val taxRate: Double get() = taxPercent / 100.0
    val subtotal: Long get() = totals.subtotal
    /** What the discount actually removed (clamped to the subtotal). */
    val appliedDiscount: Long get() = totals.discount
    val discountedSubtotal: Long get() = totals.subtotal - totals.discount
    val taxAmount: Long get() = totals.taxAmount
    val grandTotal: Long get() = totals.grandTotal
    val totalItemsCount: Int get() = cartItems.sumOf { it.quantity }

    val isCash: Boolean get() = paymentMethod == PaymentMethod.CASH
    val changeDue: Long get() = CartCalculator.changeDue(isCash, grandTotal, cashTendered)
    val isTenderSufficient: Boolean get() = CartCalculator.isTenderSufficient(isCash, grandTotal, cashTendered)
    val canCompleteCheckout: Boolean get() = cartItems.isNotEmpty() && isTenderSufficient && !isProcessing
}

/** Result of one barcode scan, shown by the scanner screen as feedback. */
data class ScanEvent(val message: String, val isError: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModel(
    private val repository: PetShopRepository,
    private val settings: SettingsStore
) : ViewModel() {

    /** UI-owned state (cart contents, dialogs, typed input). Derived values are added in [uiState]. */
    private val local = MutableStateFlow(PosUiState())

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(PetCategory.ALL)
    private val _selectedType = MutableStateFlow(ProductType.ALL)

    private val _scanEvents = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)
    val scanEvents: SharedFlow<ScanEvent> = _scanEvents.asSharedFlow()

    /** Hard guard against double-tap / re-entrant checkout. Independent of any async UI state. */
    private val checkoutInFlight = AtomicBoolean(false)

    private val productIndex: StateFlow<Map<Long, ProductEntity>> = repository.allProducts
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val uiState: StateFlow<PosUiState> = combine(
        local,
        productIndex,
        settings.taxPercent,
        settings.qrisMdrRate
    ) { state, index, taxPercent, mdr ->
        state.copy(
            cartItems = resolveCart(state.cartQuantities, index),
            taxPercent = taxPercent,
            qrisMdrRate = mdr
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PosUiState())

    val products: StateFlow<List<ProductEntity>> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedType
    ) { query, category, type ->
        Triple(query, category, type)
    }.flatMapLatest { (query, category, type) ->
        if (query.isNotBlank()) {
            repository.searchProducts(query.trim())
        } else if (type != ProductType.ALL) {
            repository.getProductsByType(type)
        } else {
            repository.getProductsByCategory(category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Joins stored quantities with LIVE product rows. Sold-out / deleted products drop out of the cart and
     * quantities never exceed what is actually on the shelf (services are unlimited).
     */
    private fun resolveCart(quantities: Map<Long, Int>, index: Map<Long, ProductEntity>): List<CartItem> =
        quantities.mapNotNull { (id, qty) ->
            val product = index[id] ?: return@mapNotNull null
            val allowed = if (product.isService) qty else qty.coerceAtMost(product.currentStock)
            if (allowed <= 0) null else CartItem(product, allowed)
        }

    // ---- browsing -------------------------------------------------------------------------------

    fun selectCategory(category: PetCategory) {
        _selectedCategory.value = category
        local.update { it.copy(selectedCategory = category) }
    }

    fun selectType(type: ProductType) {
        _selectedType.value = type
        local.update { it.copy(selectedType = type) }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        local.update { it.copy(searchQuery = query) }
    }

    // ---- cart -----------------------------------------------------------------------------------

    fun addToCart(product: ProductEntity) {
        val live = productIndex.value[product.id] ?: product
        local.update { state ->
            val current = visibleQuantity(state, live)
            val next = current + 1
            if (!live.isService && next > live.currentStock) {
                val msg = if (live.currentStock <= 0) "${live.name} is out of stock."
                else "Only ${live.currentStock} ${live.name} in stock."
                state.copy(errorMessage = msg)
            } else {
                state.copy(cartQuantities = state.cartQuantities + (live.id to next), errorMessage = null)
            }
        }
    }

    /** Quantity the cashier currently sees for [product] (stored qty clamped to live stock). */
    private fun visibleQuantity(state: PosUiState, product: ProductEntity): Int {
        val stored = state.cartQuantities[product.id] ?: 0
        return if (product.isService) stored else stored.coerceAtMost(product.currentStock.coerceAtLeast(0))
    }

    fun scanBarcode(barcode: String) {
        val code = barcode.trim()
        if (code.isEmpty()) return
        viewModelScope.launch {
            val product = repository.getProductByBarcode(code)
            if (product == null) {
                val message = "Barcode '$code' not found in inventory!"
                local.update { it.copy(errorMessage = message) }
                _scanEvents.tryEmit(ScanEvent(message, isError = true))
                return@launch
            }
            val before = local.value.cartQuantities[product.id] ?: 0
            addToCart(product)
            val after = local.value.cartQuantities[product.id] ?: 0
            if (after > before) {
                _scanEvents.tryEmit(ScanEvent("Added: ${product.name}", isError = false))
            } else {
                // addToCart refused (out of stock); it already set a readable errorMessage.
                _scanEvents.tryEmit(ScanEvent(local.value.errorMessage ?: "Could not add ${product.name}.", isError = true))
            }
        }
    }

    fun updateQuantity(productId: Long, newQuantity: Int) {
        val live = productIndex.value[productId]
        local.update { state ->
            if (newQuantity <= 0 || live == null) {
                state.copy(cartQuantities = state.cartQuantities - productId)
            } else {
                val allowed = if (live.isService) newQuantity else newQuantity.coerceAtMost(live.currentStock)
                val notice = if (allowed < newQuantity) "Only ${live.currentStock} ${live.name} in stock." else null
                if (allowed <= 0) state.copy(cartQuantities = state.cartQuantities - productId, errorMessage = notice)
                else state.copy(cartQuantities = state.cartQuantities + (productId to allowed), errorMessage = notice)
            }
        }
    }

    fun removeFromCart(productId: Long) {
        local.update { it.copy(cartQuantities = it.cartQuantities - productId) }
    }

    fun clearCart() {
        local.update { it.copy(cartQuantities = emptyMap(), discountAmount = 0) }
    }

    fun setDiscount(amount: Long) {
        local.update { it.copy(discountAmount = amount.coerceIn(0L, Money.MAX_INPUT_AMOUNT)) }
    }

    // ---- settings that affect the cart ----------------------------------------------------------

    fun setTaxPercent(percent: Int) = settings.setTaxPercent(percent)

    /** Quick toggle from the cart sheet; writes the persisted setting so Settings and Cart always agree. */
    fun toggleTax(enabled: Boolean, defaultPercent: Int = 11) {
        settings.setTaxPercent(if (enabled) defaultPercent else 0)
    }

    fun setQrisMdrRate(rate: Double) = settings.setQrisMdrRate(rate)

    // ---- checkout -------------------------------------------------------------------------------

    fun setPaymentMethod(method: PaymentMethod) {
        local.update { it.copy(paymentMethod = method) }
    }

    fun setCashTendered(amount: Long) {
        local.update { it.copy(cashTendered = amount.coerceIn(0L, Money.MAX_INPUT_AMOUNT)) }
    }

    fun setQrisReference(reference: String) {
        local.update { it.copy(qrisReference = reference.filter { c -> c.isLetterOrDigit() }.take(24).uppercase()) }
    }

    fun setCustomerDetails(name: String, phone: String) {
        local.update { it.copy(customerName = name.take(60), customerPhone = phone.filter { c -> c.isDigit() || c == '+' }.take(20)) }
    }

    fun openCheckoutDialog() {
        val snapshot = uiState.value
        if (snapshot.cartItems.isEmpty() || snapshot.isProcessing) return
        local.update {
            it.copy(
                isCheckoutDialogOpen = true,
                paymentMethod = PaymentMethod.CASH,
                cashTendered = snapshot.grandTotal, // default to exact amount
                qrisReference = "",
                errorMessage = null
            )
        }
    }

    fun closeCheckoutDialog() {
        if (checkoutInFlight.get()) return // can't dismiss while the sale is being written
        local.update { it.copy(isCheckoutDialogOpen = false) }
    }

    fun completeCheckout() {
        if (!checkoutInFlight.compareAndSet(false, true)) return // second tap is ignored

        val state = uiState.value
        val quantities = local.value.cartQuantities
        if (state.cartItems.isEmpty()) {
            checkoutInFlight.set(false)
            return
        }
        if (!state.isTenderSufficient) {
            checkoutInFlight.set(false)
            local.update { it.copy(errorMessage = "Cash received is less than the total.") }
            return
        }

        local.update { it.copy(isProcessing = true, errorMessage = null) }

        // Cashier-typed reference only; nothing is fabricated. MDR is stored for the settlement estimate.
        val notes = if (state.paymentMethod == PaymentMethod.QRIS) {
            buildString {
                append("QRIS|MDR:").append(state.qrisMdrRate)
                if (state.qrisReference.isNotBlank()) append("|RRN:").append(state.qrisReference)
            }
        } else ""

        viewModelScope.launch {
            try {
                val result = repository.processCheckout(
                    lines = quantities.map { (id, qty) -> CheckoutLine(id, qty) },
                    paymentMethod = state.paymentMethod,
                    discountAmount = state.discountAmount,
                    taxPercent = state.taxPercent,
                    cashTendered = state.cashTendered,
                    customerName = state.customerName.trim().ifBlank { null },
                    customerPhone = state.customerPhone.ifBlank { null },
                    cashierName = "Cashier #1",
                    notes = notes
                )

                result.onSuccess { receipt ->
                    local.update {
                        it.copy(
                            cartQuantities = emptyMap(),
                            discountAmount = 0,
                            isCheckoutDialogOpen = false,
                            isReceiptDialogOpen = true,
                            isProcessing = false,
                            lastTransaction = receipt.transaction,
                            lastTransactionItems = receipt.items,
                            qrisReference = "",
                            customerName = "",
                            customerPhone = "",
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    local.update {
                        it.copy(isProcessing = false, errorMessage = error.message ?: "Checkout failed.")
                    }
                }
            } finally {
                checkoutInFlight.set(false)
                // Covers cancellation too: never leave the dialog stuck in "processing".
                local.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun dismissReceipt() {
        local.update { it.copy(isReceiptDialogOpen = false) }
    }

    fun dismissError() {
        local.update { it.copy(errorMessage = null) }
    }
}
