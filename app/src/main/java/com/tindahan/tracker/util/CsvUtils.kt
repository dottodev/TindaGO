package com.tindahan.tracker.util

import com.tindahan.tracker.data.local.entities.Expense
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.data.local.entities.Sale
import com.tindahan.tracker.data.local.entities.StockMovement
import com.tindahan.tracker.data.local.entities.Utang

object CsvUtils {
    private fun esc(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuote) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }

    fun productsCsv(products: List<Product>): String {
        val sb = StringBuilder("id,name,selling_price_cents,cost_price_cents,quantity,low_stock_threshold,created_at,updated_at\n")
        for (p in products) {
            sb.append(p.id).append(',')
                .append(esc(p.name)).append(',')
                .append(p.sellingPriceCents).append(',')
                .append(p.costPriceCents?.toString() ?: "").append(',')
                .append(p.quantity).append(',')
                .append(p.lowStockThreshold).append(',')
                .append(p.createdAt).append(',')
                .append(p.updatedAt).append('\n')
        }
        return sb.toString()
    }

    fun salesCsv(sales: List<Sale>): String {
        val sb = StringBuilder("id,product_id,product_name,quantity,unit_price_cents,total_cents,timestamp\n")
        for (s in sales) {
            sb.append(s.id).append(',')
                .append(s.productId?.toString() ?: "").append(',')
                .append(esc(s.productName)).append(',')
                .append(s.quantity).append(',')
                .append(s.unitPriceCents).append(',')
                .append(s.totalCents).append(',')
                .append(s.timestamp).append('\n')
        }
        return sb.toString()
    }

    fun utangCsv(items: List<Utang>): String {
        val sb = StringBuilder("id,customer_name,description,amount_cents,timestamp,due_date,is_paid,notes\n")
        for (u in items) {
            sb.append(u.id).append(',')
                .append(esc(u.customerName)).append(',')
                .append(esc(u.description)).append(',')
                .append(u.amountCents).append(',')
                .append(u.timestamp).append(',')
                .append(u.dueDate?.toString() ?: "").append(',')
                .append(if (u.isPaid) 1 else 0).append(',')
                .append(esc(u.notes ?: "")).append('\n')
        }
        return sb.toString()
    }

    fun expensesCsv(items: List<Expense>): String {
        val sb = StringBuilder("id,description,amount_cents,timestamp,category,notes\n")
        for (e in items) {
            sb.append(e.id).append(',')
                .append(esc(e.description)).append(',')
                .append(e.amountCents).append(',')
                .append(e.timestamp).append(',')
                .append(esc(e.category)).append(',')
                .append(esc(e.notes ?: "")).append('\n')
        }
        return sb.toString()
    }

    fun stockMovementsCsv(items: List<StockMovement>): String {
        val sb = StringBuilder("id,product_id,product_name,type,delta,quantity_after,timestamp,note\n")
        for (m in items) {
            sb.append(m.id).append(',')
                .append(m.productId?.toString() ?: "").append(',')
                .append(esc(m.productName)).append(',')
                .append(esc(m.type)).append(',')
                .append(m.delta).append(',')
                .append(m.quantityAfter).append(',')
                .append(m.timestamp).append(',')
                .append(esc(m.note ?: "")).append('\n')
        }
        return sb.toString()
    }
}
