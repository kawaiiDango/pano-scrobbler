package com.arn.scrobble.graphics

import android.graphics.Canvas
import android.graphics.Paint

actual class PlatformCanvas actual constructor(bitmap: PlatformBitmap) {
    private val canvas = Canvas(bitmap.bitmap)

    actual val descent get() = paint.fontMetrics.descent.toInt()
    actual val leading get() = paint.fontMetrics.leading.toInt()
    actual val fontHeight get() = paint.fontSpacing.toInt()
    actual var textSize: Float
        get() = paint.textSize
        set(value) {
            if (value != paint.textSize)
                paint.textSize = value
        }

    actual fun drawBitmap(bitmap: PlatformBitmap, left: Int, top: Int) {
        canvas.drawBitmap(
            bitmap.bitmap,
            left.toFloat(),
            top.toFloat(),
            null
        )
    }

    actual fun drawColor(color: Int) {
        canvas.drawColor(color)
    }

    actual fun drawText(text: String, x: Int, y: Int) {
        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)
    }

    actual fun translate(x: Int, y: Int) {
        canvas.translate(x.toFloat(), y.toFloat())
    }

    actual fun measureText(text: String) = paint.measureText(text).toInt()

    actual fun setColor(color: Int) {
        paint.color = color
    }

    companion object {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }
}