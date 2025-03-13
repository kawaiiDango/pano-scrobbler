package com.arn.scrobble.graphics

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import coil3.asImage
import coil3.request.SuccessResult

actual class PlatformBitmap {
    var bitmap: Bitmap
        private set

    actual val width: Int
        get() = bitmap.width

    actual val height: Int
        get() = bitmap.height

    actual constructor(width: Int, height: Int) {
        bitmap = createBitmap(width, height)
    }

    actual constructor(coilBitmap: Any) {
        bitmap = coilBitmap as Bitmap
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
        bitmap.getPixels(pixels, offset, stride, x, y, width, height)
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
        if (!bitmap.isMutable)
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        bitmap.setPixels(pixels, offset, stride, x, y, width, height)
    }

    actual fun recycle() {
        bitmap.recycle()
    }

}

actual fun PlatformBitmap.toImageBitmap() = bitmap.asImageBitmap()

actual fun PlatformBitmap.copyToCoilResult(successResult: SuccessResult) =
    successResult.copy(image = bitmap.asImage())