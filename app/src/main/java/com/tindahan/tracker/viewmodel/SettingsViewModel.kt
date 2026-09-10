package com.tindahan.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tindahan.tracker.data.repository.SettingsRepository
import com.tindahan.tracker.data.repository.TindahanRepository
import com.tindahan.tracker.util.BackupData
import com.tindahan.tracker.util.BackupUtils
import com.tindahan.tracker.util.CsvUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: TindahanRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    val businessName = settings.businessName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val theme = settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")
    val language = settings.language.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")
    val currency = settings.currency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₱")
    val defaultLowStock = settings.defaultLowStock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val onboardingDone = settings.onboardingDone.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBusinessName(v: String) = viewModelScope.launch { settings.setBusinessName(v) }
    fun setTheme(v: String) = viewModelScope.launch { settings.setTheme(v) }
    fun setLanguage(v: String) = viewModelScope.launch { settings.setLanguage(v) }
    fun setCurrency(v: String) = viewModelScope.launch { settings.setCurrency(v) }
    fun setDefaultLowStock(v: Int) = viewModelScope.launch { settings.setDefaultLowStock(v) }
    fun setOnboardingDone(v: Boolean) = viewModelScope.launch { settings.setOnboardingDone(v) }

    suspend fun buildBackupJson(): String {
        val products = repo.observeProducts().first()
        val full = repo.buildFullBackup(businessName.value.ifBlank { null }, products)
        return BackupUtils.toJson(full)
    }

    suspend fun restoreFromJson(raw: String): BackupUtils.ParseResult {
        val parsed = BackupUtils.parseJson(raw)
        if (parsed is BackupUtils.ParseResult.Success) {
            repo.restoreBackup(parsed.data)
            parsed.data.businessName?.let { settings.setBusinessName(it) }
        }
        return parsed
    }

    suspend fun exportCsv(kind: String): Pair<String, String> {
        return when (kind) {
            "products" -> {
                val products = repo.observeProducts().first()
                "products.csv" to CsvUtils.productsCsv(products)
            }
            "sales" -> "sales.csv" to CsvUtils.salesCsv(repo.getAllSalesOnce())
            "utang" -> "utang.csv" to CsvUtils.utangCsv(repo.getAllUtangOnce())
            else -> "expenses.csv" to CsvUtils.expensesCsv(repo.getAllExpensesOnce())
        }
    }

    class Factory(private val repo: TindahanRepository, private val settings: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repo, settings) as T
    }
}
