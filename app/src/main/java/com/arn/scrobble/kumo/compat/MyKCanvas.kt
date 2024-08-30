package com.arn.scrobble.kumo.compat

import android.graphics.Canvas
import android.graphics.Paint
import com.kennycason.kumo.compat.KumoBitmap
import com.kennycason.kumo.compat.KumoCanvas

class MyKCanvas(bitmap: KumoBitmap) : KumoCanvas {
    private val canvas = Canvas((bitmap as MyKBitmap).bitmap)

    override fun drawBitmap(bitmap: KumoBitmap, left: Int, top: Int) {
        canvas.drawBitmap((bitmap as MyKBitmap).bitmap, left.toFloat(), top.toFloat(), null)
    }

    override fun drawColor(color: Int) {
        canvas.drawColor(color)
    }

    override fun drawText(text: String, x: Int, y: Int) {
        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)
    }

    override fun rotate(degrees: Float, x: Float, y: Float) {
        canvas.rotate(degrees, x, y)
    }

    override fun translate(x: Int, y: Int) {
        canvas.translate(x.toFloat(), y.toFloat())
    }

    override fun measureText(text: String) = paint.measureText(text).toInt()

    override fun setColor(color: Int) {
        paint.color = color
    }

    override fun setTextSize(textSize: Float) {
        if (textSize == paint.textSize) return
        paint.textSize = textSize
    }

    override fun getTextSize() = paint.textSize

    override fun getFontHeight() = paint.fontSpacing.toInt()

    override fun getDescent() = paint.fontMetrics.descent.toInt()

    override fun getLeading() = paint.fontMetrics.leading.toInt()

    companion object {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }
}
