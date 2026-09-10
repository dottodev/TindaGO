package com.tindahan.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tindahan.tracker.data.local.entities.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(sale: Sale): Long

    @Query("SELECT * FROM sales ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<Sale>>

    @Query("SELECT COALESCE(SUM(totalCents), 0) FROM sales WHERE timestamp BETWEEN :from AND :to")
    fun observeTotalBetween(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalCents), 0) FROM sales WHERE timestamp BETWEEN :from AND :to")
    suspend fun totalBetween(from: Long, to: Long): Long

    @Query("SELECT * FROM sales WHERE productId = :productId ORDER BY timestamp DESC LIMIT :limit")
    fun observeForProduct(productId: Long, limit: Int = 50): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Sale>

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteById(id: Long)
}
