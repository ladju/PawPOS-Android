package com.petshop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petshop.pos.data.local.dao.TopSellerSummary
import com.petshop.pos.data.local.entity.ProductEntity
import com.petshop.pos.data.local.entity.TransactionEntity
import com.petshop.pos.data.local.entity.TransactionItemEntity
import com.petshop.pos.data.repository.PeriodStats
import com.petshop.pos.data.repository.PetShopRepository
import com.petshop.pos.domain.PeriodBounds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val lowStockItems: List<ProductEntity> = emptyList(),
    val selectedTransaction: TransactionEntity? = null,
    val selectedTransactionItems: List<TransactionItemEntity> = emptyList(),
    val isReceiptDetailOpen: Boolean = false
)

/**
 * Everything on the Recap screen is a live query over the selected period, so a sale made on the
 * cashier tab shows up here instantly. No numbers are ever invented: an empty period shows zeros.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: PetShopRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val current = MutableStateFlow<PeriodBounds?>(null)
    private val previous = MutableStateFlow<PeriodBounds?>(null)

    /** Selected period (and the one before it, for the "vs previous" badge). */
    fun setPeriod(current: PeriodBounds, previous: PeriodBounds) {
        this.current.value = current
        this.previous.value = previous
    }

    val periodStats: StateFlow<PeriodStats> = current.filterNotNull()
        .flatMapLatest { repository.observePeriodStats(it.start, it.end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodStats())

    val previousStats: StateFlow<PeriodStats> = previous.filterNotNull()
        .flatMapLatest { repository.observePeriodStats(it.start, it.end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodStats())

    val periodTransactions: StateFlow<List<TransactionEntity>> = current.filterNotNull()
        .flatMapLatest { repository.observeTransactions(it.start, it.end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All products sold in the period (not just the top few) so the screen can split revenue by category. */
    val soldProducts: StateFlow<List<TopSellerSummary>> = current.filterNotNull()
        .flatMapLatest { repository.observeTopSellers(it.start, it.end, limit = 1000) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.lowStockProducts.collect { lowItems ->
                _uiState.update { it.copy(lowStockItems = lowItems) }
            }
        }
    }

    fun viewTransactionDetails(transaction: TransactionEntity) {
        viewModelScope.launch {
            val items = repository.getTransactionItems(transaction.id)
            _uiState.update {
                it.copy(
                    selectedTransaction = transaction,
                    selectedTransactionItems = items,
                    isReceiptDetailOpen = true
                )
            }
        }
    }

    fun closeReceiptDetail() {
        _uiState.update {
            it.copy(
                isReceiptDetailOpen = false,
                selectedTransaction = null,
                selectedTransactionItems = emptyList()
            )
        }
    }
}
