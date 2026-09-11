package com.tindahan.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tindahan.tracker.data.local.entities.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<Note>>

    @Query("SELECT COUNT(*) FROM notes")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Note>
}
