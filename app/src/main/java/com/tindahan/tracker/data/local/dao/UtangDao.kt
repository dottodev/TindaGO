package com.tindahan.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tindahan.tracker.data.local.entities.Utang
import kotlinx.coroutines.flow.Flow

@Dao
interface UtangDao {
    @Query("SELECT * FROM utang ORDER BY isPaid ASC, timestamp DESC")
    fun observeAll(): Flow<List<Utang>>

    @Query("SELECT * FROM utang WHERE customerName LIKE '%' || :query || '%' ORDER BY isPaid ASC, timestamp DESC")
    fun search(query: String): Flow<List<Utang>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM utang WHERE isPaid = 0")
    fun observeUnpaidTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM utang WHERE isPaid = 1")
    fun observePaidTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM utang")
    fun observeGrandTotal(): Flow<Long>

    @Query("SELECT * FROM utang WHERE isPaid = 0 ORDER BY timestamp DESC")
    fun observeUnpaid(): Flow<List<Utang>>

    @Insert
    suspend fun insert(utang: Utang): Long

    @Update
    suspend fun update(utang: Utang)

    @Delete
    suspend fun delete(utang: Utang)

    @Query("DELETE FROM utang WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE utang SET isPaid = :paid WHERE id = :id")
    suspend fun setPaid(id: Long, paid: Boolean)

    @Query("SELECT * FROM utang ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Utang>
}
