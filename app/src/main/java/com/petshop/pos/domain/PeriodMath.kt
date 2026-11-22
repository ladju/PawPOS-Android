package com.petshop.pos.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class PeriodUnit { DAY, WEEK, MONTH, YEAR }

/** Half-open time range [start, end) in epoch millis. */
data class PeriodBounds(val start: Long, val end: Long) {
    operator fun contains(timestamp: Long): Boolean = timestamp >= start && timestamp < end
}

data class TrendBucket(val label: String, val amount: Long)

/**
 * Real calendar math for the Recap screen: weeks start on Monday, [offsetBack] = 0 is the current
 * period, 1 the one before it, and so on.
 */
object PeriodMath {

    private fun startOfDay(now: Long, tz: TimeZone): Calendar = Calendar.getInstance(tz).apply {
        timeInMillis = now
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    fun bounds(unit: PeriodUnit, offsetBack: Int, now: Long, tz: TimeZone = TimeZone.getDefault()): PeriodBounds {
        val cal = startOfDay(now, tz)
        val back = offsetBack.coerceAtLeast(0)
        when (unit) {
            PeriodUnit.DAY -> cal.add(Calendar.DAY_OF_YEAR, -back)
            PeriodUnit.WEEK -> {
                val sinceMonday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                cal.add(Calendar.DAY_OF_YEAR, -sinceMonday)
                cal.add(Calendar.WEEK_OF_YEAR, -back)
            }
            PeriodUnit.MONTH -> { cal.set(Calendar.DAY_OF_MONTH, 1); cal.add(Calendar.MONTH, -back) }
            PeriodUnit.YEAR -> { cal.set(Calendar.DAY_OF_YEAR, 1); cal.add(Calendar.YEAR, -back) }
        }
        val start = cal.timeInMillis
        when (unit) {
            PeriodUnit.DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            PeriodUnit.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            PeriodUnit.MONTH -> cal.add(Calendar.MONTH, 1)
            PeriodUnit.YEAR -> cal.add(Calendar.YEAR, 1)
        }
        return PeriodBounds(start, cal.timeInMillis)
    }

    /** Sums [points] (timestamp to amount) into chart buckets: hours, weekdays, week-of-month, or months. */
    fun buckets(
        unit: PeriodUnit,
        bounds: PeriodBounds,
        points: List<Pair<Long, Long>>,
        tz: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): List<TrendBucket> {
        val cal = Calendar.getInstance(tz).apply { firstDayOfWeek = Calendar.MONDAY }
        cal.timeInMillis = bounds.start
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val count = when (unit) {
            PeriodUnit.DAY -> 24
            PeriodUnit.WEEK -> 7
            PeriodUnit.MONTH -> (daysInMonth + 6) / 7
            PeriodUnit.YEAR -> 12
        }
        val sums = LongArray(count)
        for ((timestamp, amount) in points) {
            if (timestamp !in bounds) continue
            cal.timeInMillis = timestamp
            val index = when (unit) {
                PeriodUnit.DAY -> cal.get(Calendar.HOUR_OF_DAY)
                PeriodUnit.WEEK -> (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                PeriodUnit.MONTH -> (cal.get(Calendar.DAY_OF_MONTH) - 1) / 7
                PeriodUnit.YEAR -> cal.get(Calendar.MONTH)
            }
            sums[index.coerceIn(0, count - 1)] += amount
        }

        val dayFormat = SimpleDateFormat("EEE", locale).apply { timeZone = tz }
        val monthFormat = SimpleDateFormat("MMM", locale).apply { timeZone = tz }
        return List(count) { i ->
            val label = when (unit) {
                PeriodUnit.DAY -> i.toString().padStart(2, '0')
                PeriodUnit.WEEK -> dayFormat.format(Date(bounds.start + i * 24L * 3_600_000L + 12L * 3_600_000L))
                PeriodUnit.MONTH -> "${i * 7 + 1}-${minOf((i + 1) * 7, daysInMonth)}"
                PeriodUnit.YEAR -> {
                    val c = Calendar.getInstance(tz).apply { timeInMillis = bounds.start; add(Calendar.MONTH, i) }
                    monthFormat.format(c.time)
                }
            }
            TrendBucket(label, sums[i])
        }
    }

    /** Percent change versus the previous period; null when there is nothing to compare against. */
    fun percentChange(current: Long, previous: Long): Int? =
        if (previous <= 0L) null else Math.round((current - previous) * 100.0 / previous).toInt()
}
