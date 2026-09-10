package com.tindahan.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tindahan.tracker.data.repository.TindahanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UtangViewModel(private val repo: TindahanRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val items = _query.flatMapLatest { repo.searchUtang(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unpaidTotal = repo.observeUnpaidTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val paidTotal = repo.observePaidTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val grandTotal = repo.observeUtangGrandTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setQuery(q: String) { _query.value = q }
    fun setPaid(id: Long, paid: Boolean) { viewModelScope.launch { repo.setUtangPaid(id, paid) } }
    fun delete(id: Long) { viewModelScope.launch { repo.getAllUtangOnce().find { it.id == id }?.let { repo.deleteUtang(it) } } }
    fun deleteItem(customerId: Long, onDone: () -> Unit) = delete(customerId).also { onDone() }

    fun add(customer: String, desc: String, amount: Long, ts: Long, due: Long?, notes: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(repo.addUtang(customer, desc, amount, ts, due, notes).isSuccess)
        }
    }

    class Factory(private val repo: TindahanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UtangViewModel(repo) as T
    }
}
