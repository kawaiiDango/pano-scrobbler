package com.arn.scrobble.graphics

import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.asImage
import coil3.request.SuccessResult
import org.jetbrains.skia.Bitmap

actual class PlatformBitmap {

    var bitmap: Bitmap
        private set

    actual val width: Int
        get() = bitmap.width

    actual val height: Int
        get() = bitmap.height

    actual constructor(width: Int, height: Int) {
        bitmap = Bitmap().apply { allocN32Pixels(width, height) }
    }

    actual constructor(coilBitmap: Any) {
        bitmap = coilBitmap as coil3.Bitmap
    }

    actual fun getPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val byteArray = bitmap.readPixels()!!
        val argbIntArray = IntArray(width * height) { i ->
            val index = (i * 4)
            val r = byteArray[index].toInt() and 0xFF
            val g = byteArray[index + 1].toInt() and 0xFF
            val b = byteArray[index + 2].toInt() and 0xFF
            val a = byteArray[index + 3].toInt() and 0xFF
            (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        System.arraycopy(argbIntArray, 0, pixels, offset, argbIntArray.size)
    }

    actual fun setPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        // todo: fix later
//        if (bitmap.isImmutable)
//            bitmap.isImmutable = false
        // intArray is in ARGB
        val byteArray = ByteArray(pixels.size * 4) { i ->
            val pixel = pixels[i / 4 + offset]
            when (i % 4) {
                0 -> (pixel shr 16 and 0xFF).toByte() // R
                1 -> (pixel shr 8 and 0xFF).toByte() // G
                2 -> (pixel and 0xFF).toByte() // B
                else -> (pixel shr 24 and 0xFF).toByte() // A
            }
        }

        bitmap.installPixels(byteArray)
    }

    actual fun recycle() {
        bitmap.close()
    }
}


actual fun PlatformBitmap.toImageBitmap() = bitmap.asComposeImageBitmap()

actual fun PlatformBitmap.copyToCoilResult(successResult: SuccessResult) =
    successResult.copy(image = bitmap.asImage())