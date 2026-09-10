package com.tindahan.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index("productId"), Index("timestamp")]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long? = null,
    val productName: String,
    val type: String, // SELL, RESTOCK, CREATE, ADJUST, DELETE
    val delta: Int,
    val quantityAfter: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
) {
    companion object {
        const val SELL = "SELL"
        const val RESTOCK = "RESTOCK"
        const val CREATE = "CREATE"
        const val ADJUST = "ADJUST"
        const val DELETE = "DELETE"
    }
}
