package com.tindahan.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Money is stored as Long centavos (e.g. ₱100.00 -> 10000) to avoid
 * floating-point precision problems.
 * imagePath stores only the file name inside the app-private product_images dir
 * (null = no image). Images are local-only.
 */
@Entity(
    tableName = "products",
    indices = [Index("name")]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sellingPriceCents: Long,
    val costPriceCents: Long? = null,
    val quantity: Int = 0,
    val lowStockThreshold: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val notes: String? = null
) {
    val isLowStock: Boolean get() = quantity in 1..lowStockThreshold
    val isOutOfStock: Boolean get() = quantity <= 0
    val stockState: StockState
        get() = when {
            quantity <= 0 -> StockState.OUT_OF_STOCK
            quantity <= lowStockThreshold -> StockState.LOW_STOCK
            else -> StockState.IN_STOCK
        }
    val inventoryValueCents: Long get() = sellingPriceCents * quantity.toLong()
    val profitPerItemCents: Long? get() =
        if (costPriceCents != null) sellingPriceCents - costPriceCents else null
}

enum class StockState { IN_STOCK, LOW_STOCK, OUT_OF_STOCK }
