package com.arn.scrobble.graphics

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint

actual class PlatformCanvas actual constructor(bitmap: PlatformBitmap) {
    private val canvas = Canvas(bitmap.bitmap)
    private val typeface = FontMgr.default.legacyMakeTypeface(
        "",
        FontStyle.NORMAL
    )
    private var font = Font(typeface)
    private val fontMetrics get() = font.metrics
    private var _textSize = font.size
    actual var textSize: Float
        get() = _textSize
        set(value) {
            if (value != _textSize) {
                font = font.makeWithSize(value)
                _textSize = value
            }
        }
    actual val descent get() = fontMetrics.descent

    actual val fontHeight get() = fontMetrics.height

    actual val leading get() = fontMetrics.leading

    actual fun drawBitmap(bitmap: PlatformBitmap, left: Int, top: Int) {
        canvas.drawImage(Image.makeFromBitmap(bitmap.bitmap), left.toFloat(), top.toFloat())
    }

    actual fun drawColor(color: Int) {
        canvas.clear(color)
    }

    actual fun drawText(text: String, x: Float, y: Float) {
        canvas.drawString(text, x, y, font, paint)
    }

    actual fun translate(x: Float, y: Float) {
        canvas.translate(x, y)
    }

    actual fun measureText(text: String) = font.measureTextWidth(text)

    actual fun setColor(color: Int) {
        paint.color = color
    }

    companion object {
        private val paint = Paint().apply { isAntiAlias = true }
    }
}
