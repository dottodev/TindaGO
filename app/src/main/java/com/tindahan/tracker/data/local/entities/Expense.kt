package com.tindahan.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index("timestamp"), Index("category")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Other",
    val notes: String? = null
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "Stock", "Electricity", "Supplies", "Transportation", "Food", "Other"
        )
    }
}
