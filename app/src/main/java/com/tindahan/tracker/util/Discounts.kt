package com.tindahan.tracker.util

/** Discount model for sales. Percent is 0..100; fixed is centavos. */
enum class DiscountType { NONE, PERCENT, FIXED }

data class DiscountResult(
    val subtotalCents: Long,
    val discountCents: Long,
    val totalCents: Long
)

object Discounts {
    /**
     * @param value for PERCENT: 0.0..100.0 ; for FIXED: centavos as Double (parsed from user input)
     * Returns discount clamped so total never goes below 0.
     */
    fun calculate(subtotalCents: Long, type: DiscountType, value: Double): DiscountResult {
        if (subtotalCents <= 0 || type == DiscountType.NONE) {
            return DiscountResult(subtotalCents, 0, subtotalCents)
        }
        val discount = when (type) {
            DiscountType.PERCENT -> {
                val pct = value.coerceIn(0.0, 100.0)
                ((subtotalCents * pct) / 100.0).toLong()
            }
            DiscountType.FIXED -> {
                val cents = (value * 100).toLong().coerceAtLeast(0)
                minOf(cents, subtotalCents)
            }
            DiscountType.NONE -> 0L
        }.coerceIn(0, subtotalCents)
        return DiscountResult(subtotalCents, discount, subtotalCents - discount)
    }

    /** Display label for a stored sale discount, e.g. "10%" or "₱5.00". */
    fun label(type: DiscountType, value: Double, currency: String = "₱"): String? = when (type) {
        DiscountType.NONE -> null
        DiscountType.PERCENT -> {
            val v = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
            "$v%"
        }
        DiscountType.FIXED -> MoneyUtils.formatCents((value * 100).toLong(), currency)
    }
}
