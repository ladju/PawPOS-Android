package com.petshop.pos.domain

import java.text.NumberFormat
import java.util.Locale

/**
 * All money in PawPOS is whole rupiah stored as [Long]. IDR has no minor unit in daily retail use,
 * so there is no floating point anywhere in the sales path.
 */
object Money {
    /** Upper bound for any single user-typed amount (about Rp 1 trillion), far from Long overflow. */
    const val MAX_INPUT_AMOUNT: Long = 999_999_999_999L

    fun format(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
        format.maximumFractionDigits = 0
        return "Rp ${format.format(amount)}"
    }

    /**
     * Extracts digits from free-form text ("Rp 1.250.000", "12.500") and returns a safe amount.
     * Blank or non-numeric input returns 0 and over-long input is capped at [MAX_INPUT_AMOUNT].
     */
    fun parseDigits(raw: String): Long {
        val digits = raw.filter { it in '0'..'9' }.trimStart('0')
        if (digits.isEmpty()) return 0L
        if (digits.length > 12) return MAX_INPUT_AMOUNT
        return digits.toLong().coerceAtMost(MAX_INPUT_AMOUNT)
    }
}
