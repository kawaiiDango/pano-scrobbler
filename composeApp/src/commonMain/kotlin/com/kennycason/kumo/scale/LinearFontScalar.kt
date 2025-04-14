package com.kennycason.kumo.scale

/**
 * Created by kenny on 6/30/14.
 */
class LinearFontScalar(private val minFont: Int, private val maxFont: Int) : FontScalar {

    init {
        require(maxFont >= minFont) { "maxFont cannot be smaller than minFont" }
    }

    override fun scale(n: Int, maxValue: Int): Float {
        val leftSpan = maxValue.toFloat()
        val rightSpan = (maxFont - minFont).toFloat()

        // Convert the left range into a 0-1 range
        val valueScaled = n / leftSpan

        // Convert the 0-1 range into a value in the right range.
        return minFont + (valueScaled * rightSpan)
    }
}
