package com.kennycason.kumo.scale

import kotlin.math.ln

/**
 * Created by kenny on 6/30/14.
 */
class LogFontScalar(private val minFont: Int, private val maxFont: Int) : FontScalar {
    init {
        require(maxFont >= minFont) { "maxFont cannot be smaller than minFont" }
    }

    override fun scale(n: Int, maxValue: Int): Float {
        val leftSpan = (if (maxValue == 1) 1.0 else ln(maxValue.toDouble()))
        val rightSpan = (maxFont - minFont).toDouble()

        // Convert the left range into a 0-1 range
        val valueScaled = (if (n == 1) 1.0 else ln(n.toDouble())) / leftSpan

        // Convert the 0-1 range into a value in the right range.
        return (minFont + (valueScaled * rightSpan)).toFloat()
    }
}
