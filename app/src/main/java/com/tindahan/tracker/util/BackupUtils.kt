package com.tindahan.tracker.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tindahan.tracker.data.local.entities.Expense
import com.tindahan.tracker.data.local.entities.Product
import com.tindahan.tracker.data.local.entities.Sale
import com.tindahan.tracker.data.local.entities.StockMovement
import com.tindahan.tracker.data.local.entities.Utang

data class BackupProduct(
    val product: Product,
    /** Base64 JPEG bytes; images are local-only so they travel inside the backup file. */
    val imageBase64: String? = null
)

data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val businessName: String?,
    val products: List<BackupProduct>,
    val sales: List<Sale>,
    val movements: List<StockMovement>,
    val utang: List<Utang>,
    val expenses: List<Expense>
)

object BackupUtils {
    const val BACKUP_VERSION = 1
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun optLongOrNull(o: JsonObject, k: String): Long? {
        if (!o.has(k) || o.get(k).isJsonNull) return null
        return try { o.get(k).asLong } catch (e: Exception) { null }
    }
    private fun optString(o: JsonObject, k: String, def: String = ""): String {
        if (!o.has(k) || o.get(k).isJsonNull) return def
        return try { o.get(k).asString } catch (e: Exception) { def }
    }
    private fun optInt(o: JsonObject, k: String, def: Int = 0): Int {
        if (!o.has(k) || o.get(k).isJsonNull) return def
        return try { o.get(k).asInt } catch (e: Exception) { def }
    }
    private fun optLong(o: JsonObject, k: String, def: Long = 0L): Long {
        if (!o.has(k) || o.get(k).isJsonNull) return def
        return try { o.get(k).asLong } catch (e: Exception) { def }
    }
    private fun optBoolean(o: JsonObject, k: String, def: Boolean = false): Boolean {
        if (!o.has(k) || o.get(k).isJsonNull) return def
        return try { o.get(k).asBoolean } catch (e: Exception) { def }
    }
    private fun optArray(root: JsonObject, k: String): JsonArray {
        if (!root.has(k) || !root.get(k).isJsonArray) return JsonArray()
        return root.getAsJsonArray(k)
    }

    fun toJson(data: BackupData): String {
        val root = JsonObject()
        root.addProperty("version", data.version)
        root.addProperty("exportedAt", data.exportedAt)
        if (data.businessName != null) root.addProperty("businessName", data.businessName)
        else root.add("businessName", null)

        val pj = JsonArray()
        for (bp in data.products) {
            val p = bp.product
            val o = JsonObject()
            o.addProperty("id", p.id)
            o.addProperty("name", p.name)
            o.addProperty("sellingPriceCents", p.sellingPriceCents)
            if (p.costPriceCents != null) o.addProperty("costPriceCents", p.costPriceCents) else o.add("costPriceCents", null)
            o.addProperty("quantity", p.quantity)
            o.addProperty("lowStockThreshold", p.lowStockThreshold)
            o.addProperty("createdAt", p.createdAt)
            o.addProperty("updatedAt", p.updatedAt)
            if (p.notes != null) o.addProperty("notes", p.notes) else o.add("notes", null)
            if (bp.imageBase64 != null) o.addProperty("imageBase64", bp.imageBase64) else o.add("imageBase64", null)
            pj.add(o)
        }
        root.add("products", pj)

        val sj = JsonArray()
        for (s in data.sales) {
            val o = JsonObject()
            o.addProperty("id", s.id)
            if (s.productId != null) o.addProperty("productId", s.productId) else o.add("productId", null)
            o.addProperty("productName", s.productName)
            o.addProperty("quantity", s.quantity)
            o.addProperty("unitPriceCents", s.unitPriceCents)
            o.addProperty("totalCents", s.totalCents)
            o.addProperty("timestamp", s.timestamp)
            o.addProperty("discountCents", s.discountCents)
            if (s.discountLabel != null) o.addProperty("discountLabel", s.discountLabel) else o.add("discountLabel", null)
            if (s.note != null) o.addProperty("note", s.note) else o.add("note", null)
            sj.add(o)
        }
        root.add("sales", sj)

        val mj = JsonArray()
        for (m in data.movements) {
            val o = JsonObject()
            o.addProperty("id", m.id)
            if (m.productId != null) o.addProperty("productId", m.productId) else o.add("productId", null)
            o.addProperty("productName", m.productName)
            o.addProperty("type", m.type)
            o.addProperty("delta", m.delta)
            o.addProperty("quantityAfter", m.quantityAfter)
            o.addProperty("timestamp", m.timestamp)
            if (m.note != null) o.addProperty("note", m.note) else o.add("note", null)
            mj.add(o)
        }
        root.add("movements", mj)

        val uj = JsonArray()
        for (u in data.utang) {
            val o = JsonObject()
            o.addProperty("id", u.id)
            o.addProperty("customerName", u.customerName)
            o.addProperty("description", u.description)
            o.addProperty("amountCents", u.amountCents)
            o.addProperty("timestamp", u.timestamp)
            if (u.dueDate != null) o.addProperty("dueDate", u.dueDate) else o.add("dueDate", null)
            o.addProperty("isPaid", u.isPaid)
            if (u.notes != null) o.addProperty("notes", u.notes) else o.add("notes", null)
            uj.add(o)
        }
        root.add("utang", uj)

        val ej = JsonArray()
        for (e in data.expenses) {
            val o = JsonObject()
            o.addProperty("id", e.id)
            o.addProperty("description", e.description)
            o.addProperty("amountCents", e.amountCents)
            o.addProperty("timestamp", e.timestamp)
            o.addProperty("category", e.category)
            if (e.notes != null) o.addProperty("notes", e.notes) else o.add("notes", null)
            ej.add(o)
        }
        root.add("expenses", ej)
        return gson.toJson(root)
    }

    sealed interface ParseResult {
        data class Success(val data: BackupData) : ParseResult
        data class Failure(val reason: String) : ParseResult
    }

    fun parseJson(raw: String): ParseResult {
        return try {
            if (raw.isBlank() || raw.length > 50_000_000) return ParseResult.Failure("Invalid file")
            val root = try { JsonParser.parseString(raw).asJsonObject } catch (e: Exception) { return ParseResult.Failure("Corrupted data") }
            val version = optInt(root, "version", -1)
            if (version != BACKUP_VERSION) return ParseResult.Failure("Wrong version")
            val exportedAt = optLong(root, "exportedAt", System.currentTimeMillis())
            val businessName = if (!root.has("businessName") || root.get("businessName").isJsonNull) {
                null
            } else {
                try { root.get("businessName").asString.ifBlank { null } } catch (e: Exception) { null }
            }

            val products = mutableListOf<BackupProduct>()
            for (el in optArray(root, "products")) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val name = optString(o, "name", "").trim()
                if (name.isEmpty()) continue
                val selling = try {
                    if (!o.has("sellingPriceCents") || o.get("sellingPriceCents").isJsonNull) continue
                    o.get("sellingPriceCents").asLong
                } catch (e: Exception) { continue }
                if (selling < 0) continue
                val cost = optLongOrNull(o, "costPriceCents")?.takeIf { it >= 0 }
                val qty = optInt(o, "quantity", 0).coerceIn(0, 1_000_000)
                val thr = optInt(o, "lowStockThreshold", 5).coerceIn(0, 1_000_000)
                val notes = if (!o.has("notes") || o.get("notes").isJsonNull) null else try { o.get("notes").asString } catch (e: Exception) { null }
                val img = if (!o.has("imageBase64") || o.get("imageBase64").isJsonNull) null else try { o.get("imageBase64").asString.takeIf { it.length <= 12_000_000 } } catch (e: Exception) { null }
                products.add(
                    BackupProduct(
                        Product(0, name, selling, cost, qty, thr, optLong(o, "createdAt", System.currentTimeMillis()), optLong(o, "updatedAt", System.currentTimeMillis()), null, notes),
                        img
                    )
                )
            }

            val sales = mutableListOf<Sale>()
            for (el in optArray(root, "sales")) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val pname = optString(o, "productName", "Unknown")
                val qty = optInt(o, "quantity", 1).coerceIn(1, 1_000_000)
                val unit = try {
                    if (!o.has("unitPriceCents") || o.get("unitPriceCents").isJsonNull) continue
                    o.get("unitPriceCents").asLong
                } catch (e: Exception) { continue }
                if (unit < 0) continue
                val total = optLong(o, "totalCents", unit * qty).coerceAtLeast(0)
                val disc = optLong(o, "discountCents", 0).coerceIn(0, total)
                val discLabel = if (!o.has("discountLabel") || o.get("discountLabel").isJsonNull) null else try { o.get("discountLabel").asString } catch (e: Exception) { null }
                val sNote = if (!o.has("note") || o.get("note").isJsonNull) null else try { o.get("note").asString } catch (e: Exception) { null }
                sales.add(Sale(0, optLongOrNull(o, "productId"), pname, qty, unit, total, optLong(o, "timestamp", System.currentTimeMillis()), disc, discLabel, sNote))
            }

            val movements = mutableListOf<StockMovement>()
            for (el in optArray(root, "movements")) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val note = if (!o.has("note") || o.get("note").isJsonNull) null else try { o.get("note").asString } catch (e: Exception) { null }
                movements.add(StockMovement(0, optLongOrNull(o, "productId"), optString(o, "productName", "Unknown"), optString(o, "type", "ADJUST"), optInt(o, "delta", 0), optInt(o, "quantityAfter", 0), optLong(o, "timestamp", System.currentTimeMillis()), note))
            }

            val utangs = mutableListOf<Utang>()
            for (el in optArray(root, "utang")) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val cname = optString(o, "customerName", "").trim()
                if (cname.isEmpty()) continue
                val amt = try {
                    if (!o.has("amountCents") || o.get("amountCents").isJsonNull) continue
                    o.get("amountCents").asLong
                } catch (e: Exception) { continue }
                if (amt < 0) continue
                val notes = if (!o.has("notes") || o.get("notes").isJsonNull) null else try { o.get("notes").asString } catch (e: Exception) { null }
                utangs.add(Utang(0, cname, optString(o, "description", ""), amt, optLong(o, "timestamp", System.currentTimeMillis()), optLongOrNull(o, "dueDate"), optBoolean(o, "isPaid", false), notes))
            }

            val expenses = mutableListOf<Expense>()
            for (el in optArray(root, "expenses")) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val desc = optString(o, "description", "").trim()
                if (desc.isEmpty()) continue
                val amt = try {
                    if (!o.has("amountCents") || o.get("amountCents").isJsonNull) continue
                    o.get("amountCents").asLong
                } catch (e: Exception) { continue }
                if (amt < 0) continue
                val notes = if (!o.has("notes") || o.get("notes").isJsonNull) null else try { o.get("notes").asString } catch (e: Exception) { null }
                expenses.add(Expense(0, desc, amt, optLong(o, "timestamp", System.currentTimeMillis()), optString(o, "category", "Other").ifBlank { "Other" }, notes))
            }

            ParseResult.Success(BackupData(version, exportedAt, businessName, products, sales, movements, utangs, expenses))
        } catch (e: Exception) {
            ParseResult.Failure("Corrupted data")
        }
    }
}
