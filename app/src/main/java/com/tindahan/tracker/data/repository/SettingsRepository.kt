package com.tindahan.tracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val BUSINESS_NAME = stringPreferencesKey("business_name")
        private val THEME = stringPreferencesKey("theme") // dark, light, system
        private val LANGUAGE = stringPreferencesKey("language") // en, tl, system
        private val CURRENCY = stringPreferencesKey("currency") // ₱
        private val DEFAULT_LOW_STOCK = intPreferencesKey("default_low_stock")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val businessName: Flow<String> = dataStore.data.map { it[BUSINESS_NAME] ?: "" }
    val theme: Flow<String> = dataStore.data.map { it[THEME] ?: "dark" }
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "en" }
    val currency: Flow<String> = dataStore.data.map { it[CURRENCY] ?: "₱" }
    val defaultLowStock: Flow<Int> = dataStore.data.map { it[DEFAULT_LOW_STOCK] ?: 5 }
    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setBusinessName(v: String) { dataStore.edit { it[BUSINESS_NAME] = v } }
    suspend fun setTheme(v: String) { dataStore.edit { it[THEME] = v } }
    suspend fun setLanguage(v: String) { dataStore.edit { it[LANGUAGE] = v } }
    suspend fun setCurrency(v: String) { dataStore.edit { it[CURRENCY] = v } }
    suspend fun setDefaultLowStock(v: Int) { dataStore.edit { it[DEFAULT_LOW_STOCK] = v.coerceIn(0, 100000) } }
    suspend fun setOnboardingDone(v: Boolean) { dataStore.edit { it[ONBOARDING_DONE] = v } }
}
