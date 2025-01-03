package com.kennycason.kumo.bg

import com.kennycason.kumo.collide.RectanglePixelCollidable

/**
 * Created by kenny on 6/30/14.
 */
interface Background {
    fun mask(backgroundCollidable: RectanglePixelCollidable)
}
