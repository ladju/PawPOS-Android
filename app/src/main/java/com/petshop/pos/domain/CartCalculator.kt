package com.petshop.pos.domain

/** One priced line of a sale. Prices come from the database at checkout time, never from stale UI state. */
data class PricedLine(val unitPrice: Long, val quantity: Int)

data class CartTotals(
    val subtotal: Long,
    val discount: Long,
    val taxPercent: Int,
    val taxAmount: Long,
    val grandTotal: Long
) {
    companion object {
        val EMPTY = CartTotals(0L, 0L, 0, 0L, 0L)
    }
}

/** Single source of truth for sale math. The cart UI and the checkout transaction both call this. */
object CartCalculator {

    /**
     * subtotal  = sum(unitPrice * qty)
     * discount  = clamp(requestedDiscount, 0..subtotal)
     * taxAmount = round-half-up((subtotal - discount) * taxPercent / 100)
     * total     = subtotal - discount + taxAmount
     */
    fun totals(lines: List<PricedLine>, requestedDiscount: Long, taxPercent: Int): CartTotals {
        if (lines.isEmpty()) return CartTotals.EMPTY
        var subtotal = 0L
        for (line in lines) {
            require(line.unitPrice >= 0) { "Negative price" }
            require(line.quantity > 0) { "Quantity must be positive" }
            subtotal = Math.addExact(subtotal, Math.multiplyExact(line.unitPrice, line.quantity.toLong()))
        }
        val discount = requestedDiscount.coerceIn(0L, subtotal)
        val percent = taxPercent.coerceIn(0, 100)
        val taxable = subtotal - discount
        val tax = (taxable * percent + 50L) / 100L
        return CartTotals(subtotal, discount, percent, tax, taxable + tax)
    }

    /** Change owed to the customer. Always 0 for non-cash tenders and never negative. */
    fun changeDue(isCash: Boolean, grandTotal: Long, tendered: Long): Long =
        if (isCash) (tendered - grandTotal).coerceAtLeast(0L) else 0L

    /** Non-cash tenders (QRIS) have nothing to compare; cash must cover the total. */
    fun isTenderSufficient(isCash: Boolean, grandTotal: Long, tendered: Long): Boolean =
        !isCash || tendered >= grandTotal
}
