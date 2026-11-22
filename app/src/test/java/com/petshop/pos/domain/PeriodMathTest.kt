package com.petshop.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PeriodMathTest {
    private val utc = TimeZone.getTimeZone("UTC")

    /** Wednesday 20 Nov 2024, 15:30 UTC. */
    private val now: Long = Calendar.getInstance(utc).apply {
        clear(); set(2024, Calendar.NOVEMBER, 20, 15, 30, 0)
    }.timeInMillis

    private fun at(y: Int, m: Int, d: Int, h: Int = 0): Long =
        Calendar.getInstance(utc).apply { clear(); set(y, m, d, h, 0, 0) }.timeInMillis

    @Test
    fun dayIsMidnightToMidnight() {
        val b = PeriodMath.bounds(PeriodUnit.DAY, 0, now, utc)
        assertEquals(at(2024, Calendar.NOVEMBER, 20), b.start)
        assertEquals(at(2024, Calendar.NOVEMBER, 21), b.end)
        assertTrue(now in b)
    }

    @Test
    fun weekStartsOnMonday() {
        val b = PeriodMath.bounds(PeriodUnit.WEEK, 0, now, utc)
        assertEquals(at(2024, Calendar.NOVEMBER, 18), b.start)
        assertEquals(at(2024, Calendar.NOVEMBER, 25), b.end)
    }

    @Test
    fun previousMonthSteps() {
        val b = PeriodMath.bounds(PeriodUnit.MONTH, 1, now, utc)
        assertEquals(at(2024, Calendar.OCTOBER, 1), b.start)
        assertEquals(at(2024, Calendar.NOVEMBER, 1), b.end)
    }

    @Test
    fun yearBounds() {
        val b = PeriodMath.bounds(PeriodUnit.YEAR, 1, now, utc)
        assertEquals(at(2023, Calendar.JANUARY, 1), b.start)
        assertEquals(at(2024, Calendar.JANUARY, 1), b.end)
    }

    @Test
    fun endIsExclusive() {
        val b = PeriodMath.bounds(PeriodUnit.DAY, 0, now, utc)
        assertTrue(b.start in b)
        assertTrue(b.end !in b)
    }

    @Test
    fun hourBucketsSumToTotalAndIgnoreOutsidePoints() {
        val b = PeriodMath.bounds(PeriodUnit.DAY, 0, now, utc)
        val points = listOf(
            at(2024, Calendar.NOVEMBER, 20, 9) to 10_000L,
            at(2024, Calendar.NOVEMBER, 20, 9) to 5_000L,
            at(2024, Calendar.NOVEMBER, 20, 15) to 7_000L,
            at(2024, Calendar.NOVEMBER, 19, 9) to 999_999L // yesterday: must be ignored
        )
        val buckets = PeriodMath.buckets(PeriodUnit.DAY, b, points, utc)
        assertEquals(24, buckets.size)
        assertEquals(15_000L, buckets[9].amount)
        assertEquals(7_000L, buckets[15].amount)
        assertEquals(22_000L, buckets.sumOf { it.amount })
    }

    @Test
    fun percentChangeHandlesNoPreviousData() {
        assertEquals(null, PeriodMath.percentChange(100L, 0L))
        assertEquals(50, PeriodMath.percentChange(150L, 100L))
        assertEquals(-25, PeriodMath.percentChange(75L, 100L))
    }
}
