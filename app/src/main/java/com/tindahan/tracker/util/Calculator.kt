package com.tindahan.tracker.util

/**
 * Immediate-execution calculator logic (pure Kotlin, unit-testable).
 * No expression parsing: operators chain left-to-right like a pocket calculator.
 */
object Calculator {

    data class State(
        val display: String = "0",
        val acc: Double? = null,
        val op: Char? = null,
        val fresh: Boolean = true,
        val error: Boolean = false
    )

    sealed interface Key {
        data class Digit(val d: Char) : Key
        data object Dot : Key
        data class Op(val op: Char) : Key // '+', '-', '×', '÷'
        data object Equals : Key
        data object Clear : Key
        data object Back : Key
        data object Negate : Key
        data object Percent : Key
    }

    private const val MAX_LEN = 12

    fun reduce(s: State, key: Key): State {
        if (s.error && key != Key.Clear) {
            return when (key) {
                is Key.Digit -> s.copy(display = key.d.toString(), fresh = false, error = false)
                Key.Dot -> s.copy(display = "0.", fresh = false, error = false)
                else -> s
            }
        }
        return when (key) {
            Key.Clear -> State()
            is Key.Digit -> {
                if (s.fresh) s.copy(display = key.d.toString(), fresh = false)
                else {
                    val cur = s.display
                    val next = (if (cur == "0") "" else cur) + key.d
                    s.copy(display = next.take(MAX_LEN), fresh = false)
                }
            }
            Key.Dot -> {
                if (s.fresh) s.copy(display = "0.", fresh = false)
                else if ('.' in s.display) s
                else s.copy(display = (s.display + ".").take(MAX_LEN + 1), fresh = false)
            }
            Key.Back -> {
                if (s.fresh) s
                else {
                    val next = s.display.dropLast(1)
                    s.copy(display = next.ifEmpty { "0" }, fresh = next.isEmpty())
                }
            }
            Key.Negate -> {
                val v = s.display.toDoubleOrNull() ?: return s
                s.copy(display = format(-v), fresh = false)
            }
            Key.Percent -> {
                val v = s.display.toDoubleOrNull() ?: return s
                s.copy(display = format(v / 100.0), fresh = false)
            }
            is Key.Op -> {
                val v = s.display.toDoubleOrNull() ?: return s
                val newAcc = if (s.acc == null || s.fresh) {
                    if (s.acc == null) v else s.acc
                } else {
                    apply(s.acc, s.op, v) ?: return s.copy(display = "Error", acc = null, op = null, fresh = true, error = true)
                }
                s.copy(acc = newAcc, op = key.op, fresh = true)
            }
            Key.Equals -> {
                val a = s.acc
                val o = s.op
                if (a == null || o == null) return s
                val v = s.display.toDoubleOrNull() ?: return s
                val r = apply(a, o, v) ?: return State(display = "Error", error = true)
                State(display = format(r))
            }
        }
    }

    private fun apply(a: Double, op: Char?, b: Double): Double? = when (op) {
        '+' -> a + b
        '-' -> a - b
        '×' -> a * b
        '÷' -> if (b == 0.0) null else a / b
        else -> b
    }

    fun format(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return "Error"
        if (d == d.toLong().toDouble() && d.absoluteValue < 1e12) return d.toLong().toString()
        var s = String.format(java.util.Locale.US, "%.10G", d)
        s = s.replace("E+", "e").replace("E", "e")
        val eIdx = s.indexOf('e')
        val exp = if (eIdx >= 0) s.substring(eIdx) else ""
        var mant = if (eIdx >= 0) s.substring(0, eIdx) else s
        if ('.' in mant) {
            mant = mant.trimEnd('0')
            if (mant.endsWith('.')) mant += "0"
        }
        return (mant + exp).take(14)
    }

    private val Double.absoluteValue: Double get() = kotlin.math.abs(this)
}
