package com.tindahan.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Money is stored as Long centavos (e.g. ₱100.00 -> 10000) to avoid
 * floating-point precision problems.
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
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean get() = quantity <= lowStockThreshold
    val inventoryValueCents: Long get() = sellingPriceCents * quantity.toLong()
    val profitPerItemCents: Long? get() =
        if (costPriceCents != null) sellingPriceCents - costPriceCents else null
}
