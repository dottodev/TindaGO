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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(private val repo: TindahanRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val items = _query.flatMapLatest { repo.searchExpenses(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotal = repo.observeExpenseTotalBetween(DateUtils.startOfToday(), DateUtils.endOfToday())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val monthTotal = repo.observeExpenseTotalBetween(DateUtils.startOfMonth(), System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val grandTotal = repo.observeExpenseGrandTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setQuery(q: String) { _query.value = q }
    fun delete(id: Long) {
        viewModelScope.launch {
            repo.getAllExpensesOnce().find { it.id == id }?.let { repo.deleteExpense(it) }
        }
    }

    fun add(desc: String, amount: Long, ts: Long, cat: String, notes: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(repo.addExpense(desc, amount, ts, cat, notes).isSuccess)
        }
    }

    class Factory(private val repo: TindahanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExpenseViewModel(repo) as T
    }
}
