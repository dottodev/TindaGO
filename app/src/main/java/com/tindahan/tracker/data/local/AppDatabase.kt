package com.tindahan.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tindahan.tracker.data.local.dao.ExpenseDao
import com.tindahan.tracker.data.local.dao.ProductDao
import com.tindahan.tracker.data.local.dao.SaleDao
import com.tindahan.tracker.data.local.dao.StockMovementDao
import com.tindahan.tracker.data.local.dao.UtangDao
import com.tindahan.tracker.data.local.entities.Expense
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.data.local.entities.Sale
import com.tindahan.tracker.data.local.entities.StockMovement
import com.tindahan.tracker.data.local.entities.Utang

@Database(
    entities = [Product::class, Sale::class, StockMovement::class, Utang::class, Expense::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun utangDao(): UtangDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val DB_NAME = "tinda_go.db"
    }
}
