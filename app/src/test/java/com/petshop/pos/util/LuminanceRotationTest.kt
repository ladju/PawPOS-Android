package com.petshop.pos.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LuminanceRotationTest {
    // 2 wide x 3 tall:  1 2 / 3 4 / 5 6   (rowStride 4 -> two padding bytes per row)
    private val padded = byteArrayOf(1, 2, 9, 9, 3, 4, 9, 9, 5, 6, 9, 9)

    private fun List<Int>.bytes() = map { it.toByte() }
    private fun LuminanceFrame.list() = data.map { it.toInt() }

    @Test
    fun noRotationStripsRowPadding() {
        val f = rotateLuminance(padded, 2, 3, 4, 0)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), f.list())
        assertEquals(2, f.width); assertEquals(3, f.height)
    }

    @Test
    fun rotate90Clockwise() {
        val f = rotateLuminance(padded, 2, 3, 4, 90)
        assertEquals(listOf(5, 3, 1, 6, 4, 2), f.list())
        assertEquals(3, f.width); assertEquals(2, f.height)
    }

    @Test
    fun rotate180() {
        assertEquals(listOf(6, 5, 4, 3, 2, 1), rotateLuminance(padded, 2, 3, 4, 180).list())
    }

    @Test
    fun rotate270() {
        val f = rotateLuminance(padded, 2, 3, 4, 270)
        assertEquals(listOf(2, 4, 6, 1, 3, 5), f.list())
        assertEquals(3, f.width); assertEquals(2, f.height)
    }

    @Test
    fun fourQuarterTurnsReturnTheOriginal() {
        var f = rotateLuminance(padded, 2, 3, 4, 90)
        repeat(3) { f = rotateLuminance(f.data, f.width, f.height, f.width, 90) }
        assertEquals(listOf(1, 2, 3, 4, 5, 6), f.list())
    }
}
