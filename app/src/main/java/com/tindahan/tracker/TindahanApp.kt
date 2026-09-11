package com.tindahan.tracker

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.tindahan.tracker.ads.AppOpenAdManager
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
    lateinit var adsManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        // SDK init is async; the first ad load is deferred until after the
        // first frame (see MainActivity) so cold start stays smooth.
        MobileAds.initialize(this) {}
        adsManager = AppOpenAdManager(this)
        registerActivityLifecycleCallbacks(adsManager)
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
        repository = TindahanRepository(
            database.productDao(),
            database.saleDao(),
            database.stockMovementDao(),
            database.utangDao(),
            database.expenseDao(),
            applicationContext
        )
        settingsRepository = SettingsRepository(applicationContext.dataStore)
    }
}
