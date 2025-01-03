package com.kennycason.kumo.padding

import com.kennycason.kumo.Word
import com.kennycason.kumo.image.CollisionRaster
import kotlin.math.min

/**
 * Created by kenny on 7/1/14.
 * Add pixel padding around the numbers
 */
class WordPixelPadder : Padder {
    private val rectanglePadder = RectanglePadder()

    override fun pad(word: Word, padding: Int) {
        if (padding <= 0) {
            return
        }
        rectanglePadder.pad(word, padding)

        val collisionRaster = word.collisionRaster
        // create a copy of the original raster
        val originalRaster = CollisionRaster(collisionRaster)

        val width: Int = originalRaster.dimension.width
        val height: Int = originalRaster.dimension.height

        // this is the array with the sum of all set pixels in the padding area.
        // if the padding area is changed, we only need partial updates
        val pixelsSetInPaddingPerColumn = IntArray(width)

        for (x in 0..<width) {
            // create an array with the number of not transparent pixels in 
            // each coll of the padding area of point 0, 0
            pixelsSetInPaddingPerColumn[x] = countNotTransparentPixels(
                originalRaster, 0, min(padding.toDouble(), (height - 1).toDouble()).toInt(), x
            )
        }

        for (y in 0..<height) {
            // is the line inside the image?
            if (y - padding >= 0) {
                // the line (y - padding) is now outside the padding area, we need to update our index
                val line = y - padding
                var set = -1

                while ((originalRaster.nextNotTransparentPixel(set + 1, width, line)
                        .also { set = it }) != -1
                ) {
                    pixelsSetInPaddingPerColumn[set]--
                }
            }


            // is the line inside the image?
            if (y > 0 && y + padding < height) {
                // the line (y + padding) is now inside the padding area, we need to update our index
                val line = y + padding
                var set = -1

                while ((originalRaster.nextNotTransparentPixel(set + 1, width, line)
                        .also { set = it }) != -1
                ) {
                    pixelsSetInPaddingPerColumn[set]++
                }
            }

            var pixelsSetInPaddingArea = 0

            run {
                var x = 0
                val n = min(padding.toDouble(), (width - 1).toDouble()).toInt()
                while (x < n) {
                    // create the sum of all columns in the padding area of the pixel at 0,y
                    pixelsSetInPaddingArea += pixelsSetInPaddingPerColumn[x]
                    x++
                }
            }

            for (x in 0..<width) {
                if (x - padding >= 0) {
                    // the column (x - padding) is now outside the padding area, we need to update our counter
                    pixelsSetInPaddingArea -= pixelsSetInPaddingPerColumn[x - padding]
                }
                if (x > 0 && x + padding < width) {
                    // the column (x + padding) is now inside the padding area, we need to update our counter
                    pixelsSetInPaddingArea += pixelsSetInPaddingPerColumn[x + padding]
                }


                // do we have any none transparent pixels in this area?
                if (pixelsSetInPaddingArea > 0) {
                    collisionRaster.setPixelIsNotTransparent(x, y)
                }
            }
        }
    }

    private fun countNotTransparentPixels(
        originalRaster: CollisionRaster,
        minY: Int,
        maxY: Int,
        x: Int,
    ): Int {
        var n = 0

        for (y in minY..maxY) {
            if (!originalRaster.isTransparent(x, y)) {
                n++
            }
        }

        return n
    }
}
