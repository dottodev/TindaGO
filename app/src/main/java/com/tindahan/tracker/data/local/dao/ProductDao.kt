package com.tindahan.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tindahan.tracker.data.local.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun search(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE quantity <= lowStockThreshold ORDER BY quantity ASC, name ASC")
    fun observeLowStock(): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    fun observeProductCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM products")
    fun observeTotalItems(): Flow<Long>

    @Query("SELECT COALESCE(SUM(quantity * sellingPriceCents), 0) FROM products")
    fun observeInventoryValue(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE products SET quantity = quantity - 1, updatedAt = :now WHERE id = :id AND quantity > 0")
    suspend fun decrementIfAvailable(id: Long, now: Long): Int

    @Query("UPDATE products SET quantity = quantity - :qty, updatedAt = :now WHERE id = :id AND quantity >= :qty")
    suspend fun decrementBy(id: Long, qty: Int, now: Long): Int

    @Query("UPDATE products SET quantity = quantity + :qty, updatedAt = :now WHERE id = :id")
    suspend fun incrementBy(id: Long, qty: Int, now: Long): Int

    @Query("UPDATE products SET quantity = quantity + 1, updatedAt = :now WHERE id = :id")
    suspend fun increment(id: Long, now: Long): Int
}
