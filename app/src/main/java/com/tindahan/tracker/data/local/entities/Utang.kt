package com.tindahan.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "utang",
    indices = [Index("customerName"), Index("isPaid"), Index("timestamp")]
)
data class Utang(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val description: String,
    val amountCents: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val isPaid: Boolean = false,
    val notes: String? = null
)
