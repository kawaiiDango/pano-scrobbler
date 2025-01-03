package com.kennycason.kumo.wordstart

import com.kennycason.kumo.Word
import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect

/**
 * The WordSpreadScheme provides a possible starting position for a word on the
 * image
 *
 * @author &#64;wolfposd
 */
interface WordStartStrategy {
    /**
     * Calculate a starting point for the given word. The returned Point is not
     * necessarily the actual point on the image, rather a first try.
     *
     * @param dimension
     * width/height of the image
     * @param word
     * the word to be placed
     * @return X/Y starting position
     */
    fun getStartingPoint(dimension: KumoRect, word: Word): KumoPoint
}
