package com.tindahan.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** General quick notes. Local-only, optional, searchable. */
@Entity(
    tableName = "notes",
    indices = [Index("timestamp")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
