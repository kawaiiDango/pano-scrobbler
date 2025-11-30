package com.kennycason.kumo.collide

import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.kennycason.kumo.collide.checkers.RectanglePixelCollisionChecker
import com.kennycason.kumo.image.CollisionRaster

/**
 * Created by kenny on 7/2/14.
 */
class RectanglePixelCollidable(
    override val collisionRaster: CollisionRaster,
    override val position: KumoPoint,
) : Collidable {

    override fun collide(collidable: Collidable): Boolean {
        return RECTANGLE_PIXEL_COLLISION_CHECKER.collide(this, collidable)
    }

    override val dimension: KumoRect
        get() = collisionRaster.dimension

    companion object {
        private val RECTANGLE_PIXEL_COLLISION_CHECKER = RectanglePixelCollisionChecker()
    }
}
