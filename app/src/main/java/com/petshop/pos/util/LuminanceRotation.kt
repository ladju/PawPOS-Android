package com.petshop.pos.util

/** A tightly packed 8-bit luminance image (no row padding). */
class LuminanceFrame(val data: ByteArray, val width: Int, val height: Int)

/**
 * Copies a camera Y plane (which may have [rowStride] padding per row) into a tight array and rotates it clockwise by
 * [rotationDegrees] (0, 90, 180 or 270) so a barcode held horizontally is horizontal for the decoder.
 * Pure Kotlin so it can be unit tested without a device.
 */
fun rotateLuminance(
    plane: ByteArray,
    width: Int,
    height: Int,
    rowStride: Int,
    rotationDegrees: Int
): LuminanceFrame {
    val tight = ByteArray(width * height)
    for (y in 0 until height) {
        System.arraycopy(plane, y * rowStride, tight, y * width, width)
    }
    return when (((rotationDegrees % 360) + 360) % 360) {
        90 -> {
            val out = ByteArray(tight.size)
            for (y in 0 until height) for (x in 0 until width) out[x * height + (height - 1 - y)] = tight[y * width + x]
            LuminanceFrame(out, height, width)
        }
        180 -> LuminanceFrame(tight.reversedArray(), width, height)
        270 -> {
            val out = ByteArray(tight.size)
            for (y in 0 until height) for (x in 0 until width) out[(width - 1 - x) * height + y] = tight[y * width + x]
            LuminanceFrame(out, height, width)
        }
        else -> LuminanceFrame(tight, width, height)
    }
}
