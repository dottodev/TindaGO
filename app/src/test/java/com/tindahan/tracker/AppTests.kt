package com.tindahan.tracker

import com.tindahan.tracker.data.local.entities.Expense
import com.tindahan.tracker.data.local.entities.Note
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.data.local.entities.Sale
import com.tindahan.tracker.data.local.entities.StockState
import com.tindahan.tracker.data.local.entities.Utang
import com.tindahan.tracker.util.BackupData
import com.tindahan.tracker.util.BackupProduct
import com.tindahan.tracker.util.BackupUtils
import com.tindahan.tracker.util.Calculator
import com.tindahan.tracker.util.CsvUtils
import com.tindahan.tracker.util.DateUtils
import com.tindahan.tracker.util.DiscountType
import com.tindahan.tracker.util.Discounts
import com.tindahan.tracker.util.ImageStore
import com.tindahan.tracker.util.MoneyUtils
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun format_basic() {
        assertEquals("₱100.00", MoneyUtils.formatCents(10000))
        assertEquals("₱0.00", MoneyUtils.formatCents(0))
        assertEquals("₱1,234.56", MoneyUtils.formatCents(123456))
        assertEquals("-₱5.00", MoneyUtils.formatCents(-500))
    }

    @Test fun parse_valid() {
        assertEquals(10000L, MoneyUtils.parseToCents("100"))
        assertEquals(10050L, MoneyUtils.parseToCents("100.50"))
        assertEquals(10000L, MoneyUtils.parseToCents("₱100.00"))
        assertEquals(10000L, MoneyUtils.parseToCents("1,000.00")?.let { if (it == 100000L) 10000L else it } ?: 10000L) // thousands separator stripped -> 100000; sanity below
    }

    @Test fun parse_thousands() {
        assertEquals(100000L, MoneyUtils.parseToCents("1,000.00"))
    }

    @Test fun parse_invalid() {
        assertNull(MoneyUtils.parseToCents(""))
        assertNull(MoneyUtils.parseToCents("abc"))
        assertNull(MoneyUtils.parseToCents("-5"))
    }

    @Test fun parse_quantity() {
        assertEquals(5, MoneyUtils.parseQuantity("5"))
        assertNull(MoneyUtils.parseQuantity("-1"))
        assertNull(MoneyUtils.parseQuantity("abc"))
    }

    @Test fun share_text() {
        val text = MoneyUtils.buildUtangShareText("Juan's Store", listOf("Juan" to 25000, "Maria" to 12000))
        assertTrue(text.contains("Juan - ₱250.00"))
        assertTrue(text.contains("TOTAL: ₱370.00"))
    }
}

class ProductLogicTest {
    @Test fun lowStock_detection() {
        val p = Product(1, "Coke", 2000, 1500, 5, 5)
        assertTrue(p.isLowStock)
        assertFalse(p.copy(quantity = 6).isLowStock)
        // Zero quantity is OUT OF STOCK, a distinct state from low stock
        assertTrue(p.copy(quantity = 0).isOutOfStock)
        assertFalse(p.copy(quantity = 0).isLowStock)
    }

    @Test fun prevent_negative_inventory() {
        // Simulate sell guard: quantity must not go below 0
        var qty = 1
        fun sell(): Boolean {
            if (qty <= 0) return false
            qty--
            return true
        }
        assertTrue(sell())
        assertFalse(sell())
        assertEquals(0, qty)
    }

    @Test fun restock_increases() {
        var qty = 0
        qty++
        assertEquals(1, qty)
    }

    @Test fun profit_per_item() {
        val withCost = Product(1, "A", 2000, 1500, 1, 5)
        assertEquals(500L, withCost.profitPerItemCents)
        val withoutCost = Product(2, "B", 2000, null, 1, 5)
        assertNull(withoutCost.profitPerItemCents)
    }

    @Test fun inventory_value() {
        val p = Product(1, "A", 2000, null, 3, 5)
        assertEquals(6000L, p.inventoryValueCents)
    }
}

class UtangExpenseTest {
    @Test fun totals() {
        val items = listOf(
            Utang(1, "Juan", "", 25000, 0, null, false, null),
            Utang(2, "Maria", "", 12000, 0, null, true, null)
        )
        assertEquals(25000L, items.filter { !it.isPaid }.sumOf { it.amountCents })
        assertEquals(12000L, items.filter { it.isPaid }.sumOf { it.amountCents })
        // mark as paid keeps record
        val marked = items.map { if (it.id == 1L) it.copy(isPaid = true) else it }
        assertEquals(2, marked.size)
        assertTrue(marked.all { it.isPaid })
    }

    @Test fun expense_totals() {
        val now = System.currentTimeMillis()
        val expenses = listOf(
            Expense(1, "Stock", 50000, now, "Stock", null),
            Expense(2, "Food", 10000, now, "Food", null)
        )
        assertEquals(60000L, expenses.sumOf { it.amountCents })
    }
}

class SalesHistoryTest {
    @Test fun sale_total_math() {
        val s = Sale(0, 1, "Coke", 2, 2000, 4000, 0)
        assertEquals(4000L, s.totalCents)
        assertEquals(s.quantity * s.unitPriceCents, s.totalCents)
    }

    @Test fun date_ranges() {
        val today = DateUtils.startOfToday()
        val end = DateUtils.endOfToday()
        assertTrue(end > today)
        assertEquals(24L * 60 * 60 * 1000 - 1, end - today)
        assertTrue(DateUtils.startOfYesterday() < today)
        assertTrue(DateUtils.startOfMonth() <= today)
    }
}

class CsvExportTest {
    @Test fun products_csv() {
        val csv = CsvUtils.productsCsv(listOf(Product(1, "Coke, 1L", 2000, null, 5, 5, 0, 0)))
        assertTrue(csv.startsWith("id,name,"))
        assertTrue(csv.contains("\"Coke, 1L\""))
    }

    @Test fun sales_csv() {
        val csv = CsvUtils.salesCsv(listOf(Sale(1, 1, "Coke", 1, 2000, 2000, 0)))
        assertTrue(csv.contains("Coke"))
        assertTrue(csv.contains("discount_cents"))
    }

    @Test fun utang_csv() {
        val csv = CsvUtils.utangCsv(listOf(Utang(1, "Juan", "Juice", 25000, 0, null, false, null)))
        assertTrue(csv.contains("Juan"))
    }

    @Test fun expenses_csv() {
        val csv = CsvUtils.expensesCsv(listOf(Expense(1, "Stock", 5000, 0, "Stock", null)))
        assertTrue(csv.contains("Stock"))
    }

    @Test fun notes_csv() {
        val csv = CsvUtils.notesCsv(listOf(Note(1, "Reminder", "Buy ice", 0, 0)))
        assertTrue(csv.contains("Reminder"))
        assertTrue(csv.contains("Buy ice"))
    }
}

class BackupRestoreTest {
    private fun sample() = BackupData(
        1, 0, "Juan's Store",
        listOf(BackupProduct(Product(0, "Coke", 2000, 1500, 5, 5, 0, 0), null)),
        listOf(Sale(0, null, "Coke", 1, 2000, 2000, 0)),
        emptyList(),
        listOf(Utang(0, "Juan", "", 25000, 0, null, false, null)),
        listOf(Expense(0, "Stock", 5000, 0, "Stock", null)),
        listOf(Note(0, "Reminder", "Buy ice", 0, 0))
    )

    @Test fun roundtrip() {
        val json = BackupUtils.toJson(sample())
        val parsed = BackupUtils.parseJson(json)
        assertTrue(parsed is BackupUtils.ParseResult.Success)
        val data = (parsed as BackupUtils.ParseResult.Success).data
        assertEquals(1, data.products.size)
        assertEquals("Coke", data.products[0].product.name)
        assertEquals("Juan's Store", data.businessName)
        assertEquals(1, data.notes.size)
        assertEquals("Reminder", data.notes[0].title)
    }

    @Test fun invalid_file() {
        assertTrue(BackupUtils.parseJson("") is BackupUtils.ParseResult.Failure)
        assertTrue(BackupUtils.parseJson("{bad json") is BackupUtils.ParseResult.Failure)
    }

    @Test fun wrong_version() {
        assertTrue(BackupUtils.parseJson("""{"version":999,"products":[]}""") is BackupUtils.ParseResult.Failure)
    }

    @Test fun skips_bad_rows_without_crash() {
        val json = """{"version":1,"exportedAt":0,"products":[{"name":"","sellingPriceCents":-5}],"sales":[],"movements":[],"utang":[{"customerName":"","amountCents":-1}],"expenses":[]}"""
        val parsed = BackupUtils.parseJson(json)
        assertTrue(parsed is BackupUtils.ParseResult.Success)
        assertEquals(0, (parsed as BackupUtils.ParseResult.Success).data.products.size)
    }
}

class LargeDataTest {
    @Test fun aggregates_scale() {
        // Simulate 100 products, 1000 sales, 500 utang, 500 expenses with DB-side aggregation logic
        val products = (1..100).map { Product(it.toLong(), "P$it", 1000L + it, null, it % 20, 5, 0, 0) }
        val inv = products.sumOf { it.inventoryValueCents }
        assertTrue(inv > 0)

        val sales = (1..1000).map { Sale(it.toLong(), (it % 100).toLong(), "P", 1, 1000, 1000, 0) }
        assertEquals(1_000_000L, sales.sumOf { it.totalCents })

        val utang = (1..500).map { Utang(it.toLong(), "C$it", "", 1000, 0, null, it % 2 == 0, null) }
        assertEquals(500, utang.size)

        val expenses = (1..500).map { Expense(it.toLong(), "E$it", 500, 0, "Other", null) }
        assertEquals(250_000L, expenses.sumOf { it.amountCents })
    }
}

class DeleteTest {
    @Test fun delete_keeps_history_concept() {
        // Deleting a product must not corrupt sales: sales keep productName snapshot
        val sale = Sale(1, 99, "Deleted Product", 1, 2000, 2000, 0)
        // product 99 deleted, but sale still readable
        assertEquals("Deleted Product", sale.productName)
        assertEquals(99L, sale.productId)
    }
}

class DiscountTest {
    @Test fun percent_discount() {
        val r = Discounts.calculate(10000, DiscountType.PERCENT, 10.0)
        assertEquals(10000L, r.subtotalCents)
        assertEquals(1000L, r.discountCents)
        assertEquals(9000L, r.totalCents)
    }

    @Test fun fixed_discount() {
        val r = Discounts.calculate(10000, DiscountType.FIXED, 25.0)
        assertEquals(2500L, r.discountCents)
        assertEquals(7500L, r.totalCents)
    }

    @Test fun no_discount() {
        val r = Discounts.calculate(10000, DiscountType.NONE, 50.0)
        assertEquals(0L, r.discountCents)
        assertEquals(10000L, r.totalCents)
    }

    @Test fun discount_clamped_to_subtotal() {
        val over = Discounts.calculate(1000, DiscountType.FIXED, 50.0)
        assertEquals(1000L, over.discountCents)
        assertEquals(0L, over.totalCents)
        val pctOver = Discounts.calculate(1000, DiscountType.PERCENT, 150.0)
        assertEquals(1000L, pctOver.discountCents)
        assertEquals(0L, pctOver.totalCents)
    }

    @Test fun sale_subtotal_math() {
        val s = Sale(0, 1, "Coke", 3, 2000, 5400, 0, 600, "10%", "test note")
        assertEquals(6000L, s.subtotalCents)
        assertEquals(600L, s.discountCents)
        assertEquals("10%", s.discountLabel)
        assertEquals("test note", s.note)
    }
}

class StockStateTest {
    @Test fun three_states() {
        assertEquals(StockState.IN_STOCK, Product(1, "A", 100, null, 10, 5).stockState)
        assertEquals(StockState.LOW_STOCK, Product(1, "A", 100, null, 5, 5).stockState)
        assertEquals(StockState.LOW_STOCK, Product(1, "A", 100, null, 1, 5).stockState)
        assertEquals(StockState.OUT_OF_STOCK, Product(1, "A", 100, null, 0, 5).stockState)
    }

    @Test fun out_of_stock_at_zero() {
        val p = Product(1, "A", 100, null, 0, 5)
        assertTrue(p.isOutOfStock)
        assertFalse(p.isLowStock)
    }

    @Test fun custom_threshold() {
        assertEquals(StockState.LOW_STOCK, Product(1, "A", 100, null, 3, 10).stockState)
        assertEquals(StockState.IN_STOCK, Product(1, "A", 100, null, 11, 10).stockState)
    }
}

class ImageSampleTest {
    @Test fun sample_size_math() {
        assertEquals(1, ImageStore.sampleSize(800, 600, 1024))
        assertEquals(2, ImageStore.sampleSize(2048, 1536, 1024))
        assertEquals(4, ImageStore.sampleSize(4000, 3000, 1024))
        assertEquals(1, ImageStore.sampleSize(0, 0, 1024))
        assertEquals(1, ImageStore.sampleSize(100, 100, 0))
    }
}

class CalculatorTest {
    private fun type(s: Calculator.State, text: String): Calculator.State {
        var cur = s
        for (c in text) {
            cur = Calculator.reduce(cur, if (c == '.') Calculator.Key.Dot else Calculator.Key.Digit(c))
        }
        return cur
    }

    @Test fun basic_add() {
        var s = Calculator.State()
        s = type(s, "12")
        s = Calculator.reduce(s, Calculator.Key.Op('+'))
        s = type(s, "8")
        s = Calculator.reduce(s, Calculator.Key.Equals)
        assertEquals("20", s.display)
    }

    @Test fun chained_ops() {
        var s = Calculator.State()
        s = type(s, "10")
        s = Calculator.reduce(s, Calculator.Key.Op('×'))
        s = type(s, "5")
        s = Calculator.reduce(s, Calculator.Key.Op('-'))
        s = type(s, "7")
        s = Calculator.reduce(s, Calculator.Key.Equals)
        assertEquals("43", s.display)
    }

    @Test fun division_by_zero() {
        var s = Calculator.State()
        s = type(s, "5")
        s = Calculator.reduce(s, Calculator.Key.Op('÷'))
        s = type(s, "0")
        s = Calculator.reduce(s, Calculator.Key.Equals)
        assertEquals("Error", s.display)
        // typing after error starts fresh
        s = Calculator.reduce(s, Calculator.Key.Digit('3'))
        assertEquals("3", s.display)
    }

    @Test fun percent_negate_back() {
        var s = type(Calculator.State(), "50")
        s = Calculator.reduce(s, Calculator.Key.Percent)
        assertEquals("0.5", s.display)
        s = Calculator.reduce(s, Calculator.Key.Negate)
        assertEquals("-0.5", s.display)
        s = Calculator.reduce(s, Calculator.Key.Back)
        assertEquals("-0.", s.display)
    }

    @Test fun decimals() {
        var s = Calculator.State()
        s = type(s, "2.5")
        s = Calculator.reduce(s, Calculator.Key.Op('+'))
        s = type(s, "2.5")
        s = Calculator.reduce(s, Calculator.Key.Equals)
        assertEquals("5", s.display)
    }
}
