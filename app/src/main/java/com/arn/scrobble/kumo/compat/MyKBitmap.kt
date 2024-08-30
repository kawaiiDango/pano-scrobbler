package com.arn.scrobble.kumo.compat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kennycason.kumo.compat.KumoBitmap
import java.io.InputStream

class MyKBitmap : KumoBitmap {
    val bitmap: Bitmap

    constructor(width: Int, height: Int) {
        this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    constructor(inputStream: InputStream) {
        this.bitmap = BitmapFactory.decodeStream(inputStream)
    }

    override fun getWidth() = bitmap.width

    override fun getHeight() = bitmap.height

    override fun getPixels(
        pixels: IntArray,
        offset: Int,
        stride: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        bitmap.getPixels(pixels, offset, stride, x, y, width, height)
    }

    override fun recycle() {
        bitmap.recycle()
    }

    override fun convertTo(): Any {
        return bitmap
    }
}
