package com.arn.scrobble.kumo.compat

import com.kennycason.kumo.compat.KumoBitmap
import com.kennycason.kumo.compat.KumoGraphicsFactory
import java.io.InputStream

object MyKGraphicsFactory : KumoGraphicsFactory() {
    override fun createBitmap(width: Int, height: Int) = MyKBitmap(width, height)

    override fun decodeStream(inputStream: InputStream) = MyKBitmap(inputStream)

    override fun createCanvas(bitmap: KumoBitmap) = MyKCanvas(bitmap)
}
