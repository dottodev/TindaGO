package com.tindahan.tracker.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/** All money stored as Long centavos. */
object MoneyUtils {
    private val phLocale = Locale("en", "PH")

    fun formatCents(cents: Long, currencySymbol: String = "₱"): String {
        val negative = cents < 0
        val abs = kotlin.math.abs(cents)
        val pesos = abs / 100
        val centPart = (abs % 100).toString().padStart(2, '0')
        // Simple thousands separator
        val pesosStr = NumberFormat.getNumberInstance(Locale.US).format(pesos)
        return (if (negative) "-" else "") + currencySymbol + pesosStr + "." + centPart
    }

    /** Parse user input like "100", "100.5", "₱100.00" into centavos. Returns null if invalid. */
    fun parseToCents(input: String): Long? {
        val cleaned = input.trim()
            .replace("₱", "")
            .replace("PHP", "", ignoreCase = true)
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty()) return null
        return try {
            val d = cleaned.toDouble()
            if (d < 0 || d > 999_999_999.0) return null
            // Round to nearest centavo
            (d * 100).roundToLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun parseQuantity(input: String): Int? {
        val t = input.trim()
        if (t.isEmpty()) return null
        return try {
            val v = t.toInt()
            if (v < 0 || v > 1_000_000) null else v
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun isValidPrice(cents: Long?): Boolean = cents != null && cents >= 0

    /** Share text: "UTANG LIST\n\nJuan - ₱250.00\n...\n\nTOTAL: ₱870.00" */
    fun buildUtangShareText(
        businessName: String?,
        unpaid: List<Pair<String, Long>>,
        currencySymbol: String = "₱"
    ): String {
        val sb = StringBuilder()
        sb.append("UTANG LIST")
        if (!businessName.isNullOrBlank()) sb.append(" - ").append(businessName)
        sb.append("\n\n")
        var total = 0L
        for ((name, amount) in unpaid) {
            sb.append(name).append(" - ").append(formatCents(amount, currencySymbol)).append("\n")
            total += amount
        }
        sb.append("\nTOTAL: ").append(formatCents(total, currencySymbol))
        return sb.toString()
    }
}
