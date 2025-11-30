package com.kennycason.kumo.wordstart

import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.kennycason.kumo.Word
import java.util.Random

/**
 * Always returns a random point in the image as starting point
 *
 * @author &#64;wolfposd
 */
class RandomWordStart : WordStartStrategy {
    override fun getStartingPoint(dimension: KumoRect, word: Word): KumoPoint {
        val startX =
            RANDOM.nextInt((dimension.width - word.dimension.width).coerceAtLeast(dimension.width))
        val startY = RANDOM.nextInt(
            (dimension.height - word.dimension.height).coerceAtLeast(dimension.height)
        )

        return KumoPoint(startX, startY)
    }

    companion object {
        private val RANDOM = Random()
    }
}
