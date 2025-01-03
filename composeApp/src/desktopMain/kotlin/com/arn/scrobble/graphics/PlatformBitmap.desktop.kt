package com.arn.scrobble.graphics

import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.asImage
import coil3.request.SuccessResult
import org.jetbrains.skiko.toBitmap
import org.jetbrains.skiko.toBufferedImage
import java.awt.image.BufferedImage

actual class PlatformBitmap {

    var bitmap: BufferedImage
        private set

    private var isImmutable = false

    actual val width: Int
        get() = bitmap.width

    actual val height: Int
        get() = bitmap.height

    actual constructor(width: Int, height: Int) {
        bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    }

    actual constructor(coilBitmap: Any) {
        val coilBitmap = coilBitmap as coil3.Bitmap
        bitmap = coilBitmap.toBufferedImage()
        isImmutable = coilBitmap.isImmutable
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
        bitmap.getRGB(x, y, width, height, pixels, offset, stride)
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
        if (isImmutable) {
            bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            isImmutable = false
        }
        bitmap.setRGB(x, y, width, height, pixels, offset, stride)
    }

    actual fun recycle() {
    }
}


actual fun PlatformBitmap.toImageBitmap() = bitmap.toComposeImageBitmap()

actual fun PlatformBitmap.copyToCoilResult(successResult: SuccessResult) =
    successResult.copy(image = bitmap.toBitmap().asImage())