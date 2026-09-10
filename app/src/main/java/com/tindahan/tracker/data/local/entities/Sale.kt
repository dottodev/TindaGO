package com.tindahan.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historical sale. productId is nullable and NOT a hard FK cascade so that
 * deleting a product preserves history (productName snapshot is kept).
 * Discounts are stored as a fixed centavo amount plus a display label
 * (e.g. "10%" or "₱5.00"); totalCents = quantity*unit - discountCents.
 */
@Entity(
    tableName = "sales",
    indices = [Index("productId"), Index("timestamp")]
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long? = null,
    val productName: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val totalCents: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val discountCents: Long = 0,
    val discountLabel: String? = null,
    val note: String? = null
) {
    val subtotalCents: Long get() = quantity * unitPriceCents
}
