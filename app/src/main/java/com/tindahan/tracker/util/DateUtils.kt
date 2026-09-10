package com.tindahan.tracker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun endOfToday(): Long = startOfToday() + 24L * 60 * 60 * 1000 - 1

    fun startOfYesterday(): Long = startOfToday() - 24L * 60 * 60 * 1000
    fun endOfYesterday(): Long = startOfToday() - 1

    fun startOfWeek(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun startOfMonth(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun formatDateTime(ts: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    fun formatDate(ts: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    enum class SalesFilter { TODAY, YESTERDAY, WEEK, MONTH, ALL }

    fun rangeFor(filter: SalesFilter): Pair<Long, Long>? = when (filter) {
        SalesFilter.TODAY -> startOfToday() to endOfToday()
        SalesFilter.YESTERDAY -> startOfYesterday() to endOfYesterday()
        SalesFilter.WEEK -> startOfWeek() to System.currentTimeMillis()
        SalesFilter.MONTH -> startOfMonth() to System.currentTimeMillis()
        SalesFilter.ALL -> null
    }
}
