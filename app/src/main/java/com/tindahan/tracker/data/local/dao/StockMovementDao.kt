package com.tindahan.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tindahan.tracker.data.local.entities.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(movement: StockMovement): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC LIMIT :limit")
    fun observeForProduct(productId: Long, limit: Int = 50): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<StockMovement>
}
