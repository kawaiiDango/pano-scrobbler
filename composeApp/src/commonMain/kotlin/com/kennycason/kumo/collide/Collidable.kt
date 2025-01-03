package com.kennycason.kumo.collide

import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.kennycason.kumo.image.CollisionRaster

/**
 * Created by kenny on 6/29/14.
 */
interface Collidable {
    fun collide(collidable: Collidable): Boolean
    val position: KumoPoint
    val dimension: KumoRect
    val collisionRaster: CollisionRaster
}
