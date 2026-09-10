package com.tindahan.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tindahan.tracker.data.repository.TindahanRepository
import com.tindahan.tracker.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val todaySales: Long = 0,
    val todayExpenses: Long = 0,
    val outstandingUtang: Long = 0,
    val inventoryValue: Long = 0,
    val estimatedProfitToday: Long? = null,
    val profitAvailable: Boolean = false
)

class DashboardViewModel(repo: TindahanRepository) : ViewModel() {
    val state = combine(
        repo.observeSalesTotalBetween(DateUtils.startOfToday(), DateUtils.endOfToday()),
        repo.observeExpenseTotalBetween(DateUtils.startOfToday(), DateUtils.endOfToday()),
        repo.observeUnpaidTotal(),
        repo.observeInventoryValue(),
        repo.observeProducts()
    ) { sales, expenses, utang, inv, products ->
        // Profit estimate: only from products with cost info.
        // We estimate today's profit proportionally? Simple: if any product has cost, compute
        // average margin ratio and apply? Better: honest approach — set unavailable unless we track
        // cost per sale. Since sales store unit price but not cost at time of sale, we approximate
        // using current product margins weighted by today's sales is not exact.
        // So: profitAvailable = any product has cost; estimated = null (show unavailable) OR
        // compute inventory-based potential profit.
        val withCost = products.filter { it.costPriceCents != null }
        val potentialProfit = withCost.sumOf { (it.sellingPriceCents - (it.costPriceCents ?: 0)) * it.quantity.toLong() }
        DashboardState(
            todaySales = sales,
            todayExpenses = expenses,
            outstandingUtang = utang,
            inventoryValue = inv,
            estimatedProfitToday = if (withCost.isEmpty()) null else potentialProfit,
            profitAvailable = withCost.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    class Factory(private val repo: TindahanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repo) as T
    }
}
