package com.petshop.pos.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Decodes retail barcodes (EAN/UPC/Code 128/Code 39/QR) from CameraX frames with ZXing.
 * [onBarcode] is called on the analysis thread; the caller is responsible for hopping to the main thread.
 */
class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.QR_CODE
                ),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val frame = rotateLuminance(bytes, image.width, image.height, plane.rowStride, image.imageInfo.rotationDegrees)
            val source = PlanarYUVLuminanceSource(frame.data, frame.width, frame.height, 0, 0, frame.width, frame.height, false)
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            result.text?.takeIf { it.isNotBlank() }?.let(onBarcode)
        } catch (_: Exception) {
            // NotFoundException on most frames is normal; a bad frame must never crash the scanner.
        } finally {
            reader.reset()
            image.close()
        }
    }
}
