package com.kennycason.kumo.collide.checkers


import com.kennycason.kumo.collide.Collidable

/**
 * Created by kenny on 7/1/14.
 */
class RectanglePixelCollisionChecker {
    /*
          ax,ay ___________ax + a.width
            |                 |
            |                 |
            |  bx, by_________|__ bx + b.width
            |  |(INTERSECTION)|       |
            |__|______________|       |
            ay + height               |
               |______________________|
             by + height
          */
    fun collide(collidable: Collidable, collidable2: Collidable): Boolean {
        // check if bounding boxes intersect
        if (!rectCollide(collidable, collidable2)) {
            return false
        }

        val position = collidable.position
        val position2 = collidable2.position
        val collisionRaster = collidable.collisionRaster
        val collisionRaster2 = collidable2.collisionRaster

        // get the overlapping box
        val startX: Int = position.x.coerceAtLeast(position2.x)
        val endX: Int =
            (position.x + collidable.dimension.width).coerceAtMost(position2.x + collidable2.dimension.width)

        val startY: Int = position.y.coerceAtLeast(position2.y)
        val endY: Int =
            (position.y + collidable.dimension.height).coerceAtMost(position2.y + collidable2.dimension.height)

        val endX1: Int = endX - position.x
        val endX2: Int = endX - position2.x

        val stop1: Int = position.x + -1
        val stop2: Int = position2.x + -1


        // this is the fast path of finding collisions: we expect the none transparent
        // pixel to be around the center, using padding will increase this effect.
        val stepSize = ((endY - startY) / 3) + 1

        for (i in stepSize - 1 downTo 0) {
            var y = startY + i
            while (y < endY) {
                val yOfPosition: Int = y - position.y
                var absolute1: Int = position.x + collisionRaster.nextNotTransparentPixel(
                    startX - position.x, endX1, yOfPosition
                )

                if (absolute1 == stop1) {
                    y += stepSize
                    continue
                }

                val yOfPosition2: Int = y - position2.y
                var absolute2: Int = position2.x + collisionRaster2.nextNotTransparentPixel(
                    startX - position2.x, endX2, yOfPosition2
                )

                if (absolute2 == stop2) {
                    y += stepSize
                    continue
                }

                while (true) {
                    if (absolute1 > absolute2) {
                        absolute2 = position2.x + collisionRaster2.nextNotTransparentPixel(
                            absolute1 - position2.x, endX2, yOfPosition2
                        )

                        if (absolute2 == stop2) {
                            break
                        }
                    } else if (absolute1 < absolute2) {
                        absolute1 = position.x + collisionRaster.nextNotTransparentPixel(
                            absolute2 - position.x, endX1, yOfPosition
                        )

                        if (absolute1 == stop1) {
                            break
                        }
                    } else {
                        return true
                    }
                }
                y += stepSize
            }
        }
        return false
    }

    fun rectCollide(collidable: Collidable, collidable2: Collidable): Boolean {
        val position = collidable.position
        val position2 = collidable2.position

        if ((position.x + collidable.dimension.width < position2.x)
            || (position2.x + collidable2.dimension.width < position.x)
        ) {
            return false
        }
        if ((position.y + collidable.dimension.height < position2.y)
            || (position2.y + collidable2.dimension.height < position.y)
        ) {
            return false
        }
        return true
    }
}
