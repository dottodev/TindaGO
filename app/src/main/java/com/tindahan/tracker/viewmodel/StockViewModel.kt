package com.tindahan.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.data.repository.TindahanRepository
import com.tindahan.tracker.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(private val repo: TindahanRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val products: StateFlow<List<Product>> = _query
        .flatMapLatest { q -> repo.searchProducts(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryValue: StateFlow<Long> = repo.observeInventoryValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val productCount: StateFlow<Int> = repo.observeProductCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalItems: StateFlow<Long> = repo.observeTotalItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val todaySales: StateFlow<Long> = repo.observeSalesTotalBetween(
        DateUtils.startOfToday(), DateUtils.endOfToday()
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setQuery(q: String) { _query.value = q }
    fun consumeMessage() { _message.value = null }

    fun sell(id: Long, soldLabelTemplate: String) {
        viewModelScope.launch {
            val name = repo.sellOne(id)
            _message.value = if (name != null) soldLabelTemplate.format(name) else null
        }
    }

    fun restock(id: Long, restockedTemplate: String) {
        viewModelScope.launch {
            val name = repo.restockOne(id)
            _message.value = if (name != null) restockedTemplate.format(name) else null
        }
    }

    fun addProduct(name: String, selling: Long, cost: Long?, qty: Int, threshold: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val r = repo.addProduct(name, selling, cost, qty, threshold)
            onDone(r.isSuccess)
        }
    }

    class Factory(private val repo: TindahanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StockViewModel(repo) as T
    }
}
