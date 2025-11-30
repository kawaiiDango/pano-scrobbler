package com.kennycason.kumo.image

import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.arn.scrobble.graphics.PlatformBitmap
import java.util.BitSet

/**
 * Created by kenny on 7/4/14.
 */
class CollisionRaster {
    private val data: BitSet
    val dimension: KumoRect

    constructor(bufferedImage: PlatformBitmap) : this(
        KumoRect(
            bufferedImage.width,
            bufferedImage.height
        )
    ) {
        val width = dimension.width
        val height = dimension.height
        val pixels = IntArray(width * height)
        bufferedImage.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0..<height) {
            for (x in 0..<width) {
                val pixel = pixels[y * width + x]
                val pixelIsTransparent = (pixel and -0x1000000) == 0x00000000

                if (!pixelIsTransparent) {
                    setPixelIsNotTransparent(x, y)
                }
            }
        }
    }

    constructor(dimension: KumoRect) {
        this.dimension = dimension

        data = BitSet(dimension.width * dimension.height)
    }

    constructor(other: CollisionRaster) {
        this.dimension = other.dimension
        this.data = other.data.clone() as BitSet
    }

    private fun computeIndex(x: Int, y: Int): Int {
        require(!(x < 0 || x >= dimension.width)) { "x is out of bounds" }
        require(!(y < 0 || y >= dimension.height)) { "y is out of bounds" }

        return (y * dimension.width) + x
    }

    fun setPixelIsNotTransparent(x: Int, y: Int) {
        data.set(computeIndex(x, y))
    }

    fun mask(collisionRaster: CollisionRaster, point: KumoPoint) {
        val maxHeight =
            (point.y + collisionRaster.dimension.height).coerceAtMost(dimension.height)
        val maxWidth =
            (point.x + collisionRaster.dimension.width).coerceAtMost(dimension.width)

        var offY: Int = point.y
        var offY2 = 0
        while (offY < maxHeight) {
            // we can't set the "line is not transparent" flag here, 
            // the maxWidth might be smaller than the collisionRaster's width
            var offX: Int = point.x
            var offX2 = 0
            while (offX < maxWidth) {
                if (!collisionRaster.isTransparent(offX2, offY2)) {
                    setPixelIsNotTransparent(offX, offY)
                }
                offX++
                offX2++
            }
            offY++
            offY2++
        }
    }

    /**
     * @param minX (inclusive) start of the pixels to test
     * @param maxX (exclusive) end of the pixels to test
     * @param y the line to check
     */
    fun nextNotTransparentPixel(minX: Int, maxX: Int, y: Int): Int {
        require(maxX <= dimension.width) { "maxX is out of bounds" }

        val idx = computeIndex(minX, y)
        val set = data.nextSetBit(idx)

        return if (set != -1 && set < idx + maxX - minX) {
            (set - idx) + minX
        } else {
            -1
        }
    }

    fun isTransparent(x: Int, y: Int): Boolean {
        return !data.get(computeIndex(x, y))
    }
}
