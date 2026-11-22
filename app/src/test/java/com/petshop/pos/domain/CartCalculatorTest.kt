package com.petshop.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartCalculatorTest {

    @Test
    fun emptyCartIsZero() {
        assertEquals(CartTotals.EMPTY, CartCalculator.totals(emptyList(), 5_000L, 11))
    }

    @Test
    fun subtotalDiscountTaxAndTotal() {
        val lines = listOf(PricedLine(12_500L, 3), PricedLine(100_000L, 1))
        val t = CartCalculator.totals(lines, 10_000L, 11)
        assertEquals(137_500L, t.subtotal)
        assertEquals(10_000L, t.discount)
        assertEquals(14_025L, t.taxAmount) // 127_500 * 11% = 14_025
        assertEquals(141_525L, t.grandTotal)
    }

    @Test
    fun taxRoundsHalfUp() {
        // 1_050 * 5% = 52.5 -> 53
        val t = CartCalculator.totals(listOf(PricedLine(1_050L, 1)), 0L, 5)
        assertEquals(53L, t.taxAmount)
        assertEquals(1_103L, t.grandTotal)
    }

    @Test
    fun negativeDiscountIsIgnored() {
        val t = CartCalculator.totals(listOf(PricedLine(10_000L, 2)), -5_000L, 0)
        assertEquals(0L, t.discount)
        assertEquals(20_000L, t.grandTotal)
    }

    @Test
    fun discountIsCappedAtSubtotal() {
        val t = CartCalculator.totals(listOf(PricedLine(10_000L, 1)), 999_999L, 11)
        assertEquals(10_000L, t.discount)
        assertEquals(0L, t.taxAmount)
        assertEquals(0L, t.grandTotal)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroQuantityIsRejected() {
        CartCalculator.totals(listOf(PricedLine(1_000L, 0)), 0L, 0)
    }

    @Test(expected = ArithmeticException::class)
    fun overflowIsDetectedNotWrapped() {
        CartCalculator.totals(listOf(PricedLine(Long.MAX_VALUE / 2, 3)), 0L, 0)
    }

    @Test
    fun changeNeverNegativeAndOnlyForCash() {
        assertEquals(0L, CartCalculator.changeDue(true, 50_000L, 20_000L))
        assertEquals(5_000L, CartCalculator.changeDue(true, 45_000L, 50_000L))
        assertEquals(0L, CartCalculator.changeDue(false, 45_000L, 50_000L))
    }

    @Test
    fun cashMustCoverTotalButQrisNeedsNoTender() {
        assertFalse(CartCalculator.isTenderSufficient(true, 50_000L, 49_999L))
        assertTrue(CartCalculator.isTenderSufficient(true, 50_000L, 50_000L))
        assertTrue(CartCalculator.isTenderSufficient(false, 50_000L, 0L))
    }

    @Test
    fun parseDigitsIsSafe() {
        assertEquals(0L, Money.parseDigits(""))
        assertEquals(0L, Money.parseDigits("abc"))
        assertEquals(1_250_000L, Money.parseDigits("Rp 1.250.000"))
        assertEquals(Money.MAX_INPUT_AMOUNT, Money.parseDigits("9".repeat(40)))
        assertEquals(500L, Money.parseDigits("000500"))
    }

    @Test
    fun formatUsesIndonesianGrouping() {
        assertEquals("Rp 1.250.000", Money.format(1_250_000L))
        assertEquals("Rp 0", Money.format(0L))
    }

    @Test
    fun cashSuggestionsStartWithExactAndStayAboveTotal() {
        val s = suggestCashAmounts(771_750L)
        assertEquals(771_750L, s.first())
        assertEquals(listOf(771_750L, 780_000L, 800_000L, 1_000_000L), s)
        assertTrue(s.all { it >= 771_750L })
        assertTrue(suggestCashAmounts(0L).isEmpty())
        assertTrue(suggestCashAmounts(-5L).isEmpty())
    }
}
