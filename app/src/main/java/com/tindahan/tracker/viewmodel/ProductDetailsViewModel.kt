package com.tindahan.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tindahan.tracker.data.repository.TindahanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductDetailsViewModel(private val repo: TindahanRepository, private val productId: Long) : ViewModel() {
    val product = repo.observeProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val movements = repo.observeMovementsForProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales = repo.observeSalesForProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sell() { viewModelScope.launch { repo.sellOne(productId) } }
    fun restock() { viewModelScope.launch { repo.restockOne(productId) } }
    fun sellCustom(qty: Int, discountCents: Long, discountLabel: String?, note: String?, onDone: (Long?) -> Unit) {
        viewModelScope.launch { onDone(repo.sellCustom(productId, qty, discountCents, discountLabel, note)) }
    }
    fun restockCustom(qty: Int, onDone: (Int?) -> Unit) {
        viewModelScope.launch { onDone(repo.restockCustom(productId, qty)) }
    }
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            product.value?.let { repo.deleteProduct(it) }
            onDone()
        }
    }
    fun saveEdit(name: String, selling: Long, cost: Long?, qty: Int, thr: Int, imagePath: String?, notes: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cur = product.value ?: run { onDone(false); return@launch }
            val oldImage = cur.imagePath
            val r = repo.updateProduct(
                cur.copy(
                    name = name.trim(), sellingPriceCents = selling, costPriceCents = cost,
                    quantity = qty, lowStockThreshold = thr, imagePath = imagePath,
                    notes = notes?.trim()?.ifBlank { null }
                )
            )
            if (r.isSuccess) repo.pruneImage(oldImage, imagePath)
            onDone(r.isSuccess)
        }
    }

    class Factory(private val repo: TindahanRepository, private val productId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductDetailsViewModel(repo, productId) as T
    }
}
