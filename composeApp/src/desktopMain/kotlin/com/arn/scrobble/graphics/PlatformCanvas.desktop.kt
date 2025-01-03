package com.arn.scrobble.graphics

import java.awt.Color
import java.awt.RenderingHints

actual class PlatformCanvas actual constructor(bitmap: PlatformBitmap) {
    private val canvas = bitmap.bitmap.createGraphics()
    private val fontMetrics get() = canvas.fontMetrics
    private val width = bitmap.width
    private val height = bitmap.height
    private var _textSize = canvas.font.size.toFloat()
    actual var textSize: Float
        get() = _textSize
        set(value) {
            if (value != _textSize) {
                canvas.font = canvas.font.deriveFont(value)
                _textSize = value
            }
        }
    actual val descent get() = fontMetrics.descent

    actual val fontHeight get() = fontMetrics.height

    actual val leading get() = fontMetrics.leading

    init {
        canvas.setRenderingHints(getRenderingHints())
    }

    actual fun drawBitmap(bitmap: PlatformBitmap, left: Int, top: Int) {
        canvas.drawImage(bitmap.bitmap, left, top, null)
    }

    actual fun drawColor(color: Int) {
        setColor(color)
        canvas.fillRect(0, 0, width, height)
    }

    actual fun drawText(text: String, x: Int, y: Int) {
        canvas.drawString(text, x, y)
    }

    actual fun translate(x: Int, y: Int) {
        canvas.translate(x, y)
    }

    actual fun measureText(text: String) = fontMetrics.stringWidth(text)

    actual fun setColor(color: Int) {
        canvas.color = Color(color, true)
    }

    private fun getRenderingHints(): RenderingHints {
        val hints: MutableMap<RenderingHints.Key, Any?> = HashMap()
        hints[RenderingHints.KEY_ALPHA_INTERPOLATION] =
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
        hints[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
        hints[RenderingHints.KEY_COLOR_RENDERING] = RenderingHints.VALUE_COLOR_RENDER_QUALITY
        hints[RenderingHints.KEY_FRACTIONALMETRICS] = RenderingHints.VALUE_FRACTIONALMETRICS_ON
        hints[RenderingHints.KEY_INTERPOLATION] = RenderingHints.VALUE_INTERPOLATION_BICUBIC
        hints[RenderingHints.KEY_TEXT_ANTIALIASING] = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB

        return RenderingHints(hints)
    }
}
