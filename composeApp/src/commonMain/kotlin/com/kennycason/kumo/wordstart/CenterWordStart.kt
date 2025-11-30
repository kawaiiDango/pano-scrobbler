package com.kennycason.kumo.wordstart

import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.kennycason.kumo.Word

/**
 * Always returns the Center of the image
 *
 * @author &#64;wolfposd
 */
class CenterWordStart : WordStartStrategy {
    override fun getStartingPoint(dimension: KumoRect, word: Word): KumoPoint {
        val x: Int = (dimension.width / 2) - (word.dimension.width / 2)
        val y: Int = (dimension.height / 2) - (word.dimension.height / 2)

        return KumoPoint(x, y)
    }
}
