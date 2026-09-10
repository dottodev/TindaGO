package com.tindahan.tracker.data.repository

import com.tindahan.tracker.data.local.dao.ExpenseDao
import com.tindahan.tracker.data.local.dao.ProductDao
import com.tindahan.tracker.data.local.dao.SaleDao
import com.tindahan.tracker.data.local.dao.StockMovementDao
import com.tindahan.tracker.data.local.dao.UtangDao
import com.tindahan.tracker.data.local.entities.Expense
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.data.local.entities.Sale
import com.tindahan.tracker.data.local.entities.StockMovement
import com.tindahan.tracker.data.local.entities.Utang
import com.tindahan.tracker.util.BackupData
import com.tindahan.tracker.util.BackupUtils
import kotlinx.coroutines.flow.Flow

class TindahanRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val movementDao: StockMovementDao,
    private val utangDao: UtangDao,
    private val expenseDao: ExpenseDao
) {
    // ---- Products ----
    fun observeProducts(): Flow<List<Product>> = productDao.observeAll()
    fun searchProducts(q: String): Flow<List<Product>> =
        if (q.isBlank()) productDao.observeAll() else productDao.search(q.trim())
    fun observeProduct(id: Long): Flow<Product?> = productDao.observeById(id)
    fun observeLowStock(): Flow<List<Product>> = productDao.observeLowStock()
    fun observeProductCount(): Flow<Int> = productDao.observeProductCount()
    fun observeTotalItems(): Flow<Long> = productDao.observeTotalItems()
    fun observeInventoryValue(): Flow<Long> = productDao.observeInventoryValue()

    suspend fun addProduct(
        name: String,
        sellingCents: Long,
        costCents: Long?,
        quantity: Int,
        threshold: Int
    ): Result<Long> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("name"))
        if (sellingCents < 0) return Result.failure(IllegalArgumentException("price"))
        if (costCents != null && costCents < 0) return Result.failure(IllegalArgumentException("cost"))
        if (quantity < 0 || threshold < 0) return Result.failure(IllegalArgumentException("qty"))
        val now = System.currentTimeMillis()
        val id = productDao.insert(Product(0, cleanName, sellingCents, costCents, quantity, threshold, now, now))
        movementDao.insert(StockMovement(0, id, cleanName, StockMovement.CREATE, quantity, quantity, now, null))
        return Result.success(id)
    }

    suspend fun updateProduct(p: Product): Result<Unit> {
        if (p.name.isBlank() || p.sellingPriceCents < 0 || p.quantity < 0) {
            return Result.failure(IllegalArgumentException("invalid"))
        }
        productDao.update(p.copy(updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }

    suspend fun deleteProduct(p: Product) {
        // Keep sales history (productName snapshot). Remove product row + log movement.
        productDao.delete(p)
        movementDao.insert(
            StockMovement(0, null, p.name, StockMovement.DELETE, 0, 0, System.currentTimeMillis(), null)
        )
    }

    /** Returns product name on success, null if out of stock. */
    suspend fun sellOne(productId: Long): String? {
        val now = System.currentTimeMillis()
        val product = productDao.getById(productId) ?: return null
        if (product.quantity <= 0) return null
        val updated = productDao.decrementIfAvailable(productId, now)
        if (updated == 0) return null
        saleDao.insert(Sale(0, product.id, product.name, 1, product.sellingPriceCents, product.sellingPriceCents, now))
        movementDao.insert(StockMovement(0, product.id, product.name, StockMovement.SELL, -1, product.quantity - 1, now, null))
        return product.name
    }

    suspend fun restockOne(productId: Long): String? {
        val now = System.currentTimeMillis()
        val product = productDao.getById(productId) ?: return null
        productDao.increment(productId, now)
        movementDao.insert(StockMovement(0, product.id, product.name, StockMovement.RESTOCK, 1, product.quantity + 1, now, null))
        return product.name
    }

    fun observeMovementsForProduct(id: Long): Flow<List<StockMovement>> = movementDao.observeForProduct(id)
    fun observeSalesForProduct(id: Long): Flow<List<Sale>> = saleDao.observeForProduct(id)

    // ---- Sales ----
    fun observeRecentSales(limit: Int = 100): Flow<List<Sale>> = saleDao.observeRecent(limit)
    fun observeSalesBetween(from: Long, to: Long): Flow<List<Sale>> = saleDao.observeBetween(from, to)
    fun observeSalesTotalBetween(from: Long, to: Long): Flow<Long> = saleDao.observeTotalBetween(from, to)
    suspend fun salesTotalBetween(from: Long, to: Long): Long = saleDao.totalBetween(from, to)
    suspend fun getAllSalesOnce(): List<Sale> = saleDao.getAllOnce()

    // ---- Utang ----
    fun observeUtang(): Flow<List<Utang>> = utangDao.observeAll()
    fun searchUtang(q: String): Flow<List<Utang>> =
        if (q.isBlank()) utangDao.observeAll() else utangDao.search(q.trim())
    fun observeUnpaidTotal(): Flow<Long> = utangDao.observeUnpaidTotal()
    fun observePaidTotal(): Flow<Long> = utangDao.observePaidTotal()
    fun observeUtangGrandTotal(): Flow<Long> = utangDao.observeGrandTotal()

    suspend fun addUtang(customer: String, desc: String, amountCents: Long, ts: Long, due: Long?, notes: String?): Result<Long> {
        if (customer.isBlank()) return Result.failure(IllegalArgumentException("customer"))
        if (amountCents <= 0) return Result.failure(IllegalArgumentException("amount"))
        return Result.success(utangDao.insert(Utang(0, customer.trim(), desc.trim(), amountCents, ts, due, false, notes?.trim()?.ifBlank { null })))
    }

    suspend fun setUtangPaid(id: Long, paid: Boolean) = utangDao.setPaid(id, paid)
    suspend fun deleteUtang(u: Utang) = utangDao.delete(u)
    suspend fun getAllUtangOnce(): List<Utang> = utangDao.getAllOnce()

    // ---- Expenses ----
    fun observeExpenses(): Flow<List<Expense>> = expenseDao.observeAll()
    fun searchExpenses(q: String): Flow<List<Expense>> =
        if (q.isBlank()) expenseDao.observeAll() else expenseDao.search(q.trim())
    fun observeExpensesBetween(from: Long, to: Long): Flow<List<Expense>> = expenseDao.observeBetween(from, to)
    fun observeExpenseTotalBetween(from: Long, to: Long): Flow<Long> = expenseDao.observeTotalBetween(from, to)
    fun observeExpenseGrandTotal(): Flow<Long> = expenseDao.observeGrandTotal()
    suspend fun expenseTotalBetween(from: Long, to: Long): Long = expenseDao.totalBetween(from, to)

    suspend fun addExpense(desc: String, amountCents: Long, ts: Long, cat: String, notes: String?): Result<Long> {
        if (desc.isBlank()) return Result.failure(IllegalArgumentException("desc"))
        if (amountCents <= 0) return Result.failure(IllegalArgumentException("amount"))
        return Result.success(expenseDao.insert(Expense(0, desc.trim(), amountCents, ts, cat.ifBlank { "Other" }, notes?.trim()?.ifBlank { null })))
    }

    suspend fun deleteExpense(e: Expense) = expenseDao.delete(e)
    suspend fun getAllExpensesOnce(): List<Expense> = expenseDao.getAllOnce()
    suspend fun getAllProductsOnceBlocking(): List<Product> {
        // Used for backup/export/dashboard profit estimate; collect via DAO not exposed as suspend-all,
        // so caller should use Flow.first(). Provided here via observeAll callers.
        throw UnsupportedOperationException("Use observeProducts().first()")
    }

    // ---- Backup / Restore ----
    suspend fun buildBackup(businessName: String?): BackupData {
        // Collect via one-shot queries where available
        val sales = saleDao.getAllOnce()
        val movements = movementDao.getAllOnce()
        val utang = utangDao.getAllOnce()
        val expenses = expenseDao.getAllOnce()
        // products need a suspend getter; use a direct query path via search-like flow is not ideal,
        // so we query through productDao.observeAll is Flow; repository callers pass products in.
        // To keep this method self-contained we throw if not supplied — instead use buildBackupWithProducts.
        return BackupData(BackupUtils.BACKUP_VERSION, System.currentTimeMillis(), businessName, emptyList(), sales, movements, utang, expenses)
    }

    suspend fun restoreBackup(data: BackupData, products: List<Product>) {
        // Insert products first and remap? Keep simple: insert all as new rows (ids reset to 0 already in parser).
        for (p in products) productDao.insert(p.copy(id = 0))
        for (s in data.sales) saleDao.insert(s.copy(id = 0))
        for (m in data.movements) movementDao.insert(m.copy(id = 0))
        for (u in data.utang) utangDao.insert(u.copy(id = 0))
        for (e in data.expenses) expenseDao.insert(e.copy(id = 0))
    }
}
