package com.tindahan.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tindahan.tracker.data.local.entities.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE description LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE timestamp BETWEEN :from AND :to")
    fun observeTotalBetween(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE timestamp BETWEEN :from AND :to")
    suspend fun totalBetween(from: Long, to: Long): Long

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM expenses")
    fun observeGrandTotal(): Flow<Long>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Expense>
}
