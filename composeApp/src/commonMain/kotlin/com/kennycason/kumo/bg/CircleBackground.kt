package com.kennycason.kumo.bg

import com.kennycason.kumo.collide.RectanglePixelCollidable
import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.kennycason.kumo.image.CollisionRaster

/**
 * Created by kenny on 6/30/14.
 */
class CircleBackground(private val radius: Int) : Background {
    private val position: KumoPoint = KumoPoint(0, 0)

    override fun mask(background: RectanglePixelCollidable) {
        val dimensionOfBackground: KumoRect = background.dimension
        val rasterOfBackground: CollisionRaster = background.collisionRaster

        for (y in 0..<dimensionOfBackground.height) {
            for (x in 0..<dimensionOfBackground.width) {
                if (!inCircle(x, y)) {
                    rasterOfBackground.setPixelIsNotTransparent(
                        position.x + x, position.y + y
                    )
                }
            }
        }
    }

    private fun inCircle(x: Int, y: Int): Boolean {
        val centerX: Int = position.x + x - radius
        val centerY: Int = position.y + y - radius
        return (centerX * centerX) + (centerY * centerY) <= radius * radius
    }
}
