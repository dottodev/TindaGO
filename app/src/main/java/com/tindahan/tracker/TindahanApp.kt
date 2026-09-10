package com.tindahan.tracker

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.tindahan.tracker.data.local.AppDatabase
import com.tindahan.tracker.data.repository.SettingsRepository
import com.tindahan.tracker.data.repository.TindahanRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TindahanApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: TindahanRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        ).build()
        repository = TindahanRepository(
            database.productDao(),
            database.saleDao(),
            database.stockMovementDao(),
            database.utangDao(),
            database.expenseDao()
        )
        settingsRepository = SettingsRepository(applicationContext.dataStore)
    }
}
