package com.tindahan.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        /** v1 -> v2: product image + notes, sale discount + note. All additive, history preserved. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN imagePath TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE sales ADD COLUMN discountCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN discountLabel TEXT")
                db.execSQL("ALTER TABLE sales ADD COLUMN note TEXT")
            }
        }
    }
}
