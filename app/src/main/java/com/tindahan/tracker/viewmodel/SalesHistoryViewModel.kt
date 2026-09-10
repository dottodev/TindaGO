package com.tindahan.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tindahan.tracker.data.repository.TindahanRepository
import com.tindahan.tracker.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class SalesHistoryViewModel(private val repo: TindahanRepository) : ViewModel() {
    private val _filter = MutableStateFlow(DateUtils.SalesFilter.TODAY)
    val filter = _filter.asStateFlow()

    val sales = _filter.flatMapLatest { f ->
        val range = DateUtils.rangeFor(f)
        if (range == null) repo.observeRecentSales(500)
        else repo.observeSalesBetween(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val total = _filter.flatMapLatest { f ->
        val range = DateUtils.rangeFor(f)
        if (range == null) flowOf(0L) else repo.observeSalesTotalBetween(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setFilter(f: DateUtils.SalesFilter) { _filter.value = f }

    class Factory(private val repo: TindahanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SalesHistoryViewModel(repo) as T
    }
}
