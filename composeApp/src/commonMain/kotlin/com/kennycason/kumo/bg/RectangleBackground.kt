package com.kennycason.kumo.bg

import com.kennycason.kumo.collide.RectanglePixelCollidable
import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.kennycason.kumo.image.CollisionRaster

/**
 * A Background Collision Mode in the shape of a rectangle
 *
 * @author kenny, wolfposd
 * @version 2015.11.26
 */
class RectangleBackground(private val position: KumoPoint, private val dimension: KumoRect) :
    Background {

    /**
     * Creates a rectangle background starting at (0|0) with specified width/height
     * @param dimension dimension of background
     */
    constructor(dimension: KumoRect) : this(KumoPoint(0, 0), dimension)

    override fun mask(background: RectanglePixelCollidable) {
        val dimensionOfShape: KumoRect = dimension

        val minY = position.y.coerceAtLeast(0)
        val minX = position.x.coerceAtLeast(0)

        val maxY: Int = dimensionOfShape.height + position.y - 1
        val maxX: Int = dimensionOfShape.width + position.x - 1

        val dimensionOfBackground: KumoRect = background.dimension
        val rasterOfBackground: CollisionRaster = background.collisionRaster

        for (y in 0..<dimensionOfBackground.height) {
            for (x in 0..<dimensionOfBackground.width) {
                if ((y < minY) || (y > maxY) || (x < minX) || (x > maxX)) {
                    rasterOfBackground.setPixelIsNotTransparent(x, y)
                }
            }
        }
    }
}
