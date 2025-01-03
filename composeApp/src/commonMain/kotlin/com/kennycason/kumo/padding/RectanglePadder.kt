package com.kennycason.kumo.padding

import com.arn.scrobble.graphics.PlatformBitmap
import com.arn.scrobble.graphics.PlatformCanvas
import com.kennycason.kumo.Word


/**
 * Created by kenny on 7/2/14.
 */
class RectanglePadder : Padder {
    override fun pad(word: Word, padding: Int) {
        if (padding <= 0) {
            return
        }

        val bufferedImage = word.bufferedImage
        val width = bufferedImage.width + padding * 2
        val height = bufferedImage.height + padding * 2

        val newBufferedImage = PlatformBitmap(width, height)
        val graphics = PlatformCanvas(newBufferedImage)
        graphics.drawBitmap(bufferedImage, padding, padding)

        bufferedImage.recycle()
        word.setBufferedImage(newBufferedImage)
    }
}
