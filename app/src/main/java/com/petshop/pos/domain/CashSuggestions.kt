package com.petshop.pos.domain

/**
 * Quick-tender suggestions based on Indonesian banknotes. The first entry is always the exact amount
 * ("Uang Pas"); the rest are the next sensible round amounts above the total, max 5 entries.
 */
fun suggestCashAmounts(total: Long): List<Long> {
    if (total <= 0L) return emptyList()
    val amounts = linkedSetOf(total)

    val step1: Long
    val step2: Long
    val step3: Long
    val milestone: Long
    when {
        total < 20_000L -> { step1 = 1_000L; step2 = 5_000L; step3 = 10_000L; milestone = if (total > 10_000L) 50_000L else 20_000L }
        total < 100_000L -> { step1 = 5_000L; step2 = 10_000L; step3 = 50_000L; milestone = 100_000L }
        total < 500_000L -> { step1 = 10_000L; step2 = 50_000L; step3 = 100_000L; milestone = 500_000L }
        total < 1_000_000L -> { step1 = 10_000L; step2 = 50_000L; step3 = 100_000L; milestone = 1_000_000L }
        else -> {
            step1 = 50_000L; step2 = 100_000L; step3 = 500_000L
            milestone = if (total % 1_000_000L == 0L) total else ((total / 1_000_000L) + 1) * 1_000_000L
        }
    }

    fun ceilTo(n: Long, step: Long) = if (n % step == 0L) n else ((n / step) + 1) * step

    for (r in listOf(ceilTo(total, step1), ceilTo(total, step2), ceilTo(total, step3), milestone)) {
        if (r > total) amounts.add(r)
    }
    var next = ceilTo(total, step3)
    while (amounts.size < 4 && next < total + step3 * 10) {
        next += step3
        if (next > total) amounts.add(next)
    }
    return amounts.sorted().take(5)
}
