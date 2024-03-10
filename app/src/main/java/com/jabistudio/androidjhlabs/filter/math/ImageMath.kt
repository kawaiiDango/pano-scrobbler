/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jabistudio.androidjhlabs.filter.math

/**
 * A class containing static math methods useful for image processing.
 */
object ImageMath {
    /**
     * A smoothed step function. A cubic function is used to smooth the step between two thresholds.
     *
     * @param a the lower threshold position
     * @param b the upper threshold position
     * @param x the input parameter
     * @return the output value
     */
    @JvmStatic
    fun smoothStep(a: Float, b: Float, x: Float): Float {
        var x = x
        if (x < a) return 0f
        if (x >= b) return 1f
        x = (x - a) / (b - a)
        return x * x * (3 - 2 * x)
    }

    /**
     * Clamp a value to an interval.
     *
     * @param a the lower clamp threshold
     * @param b the upper clamp threshold
     * @param x the input parameter
     * @return the clamped value
     */
    @JvmStatic
    fun clamp(x: Int, a: Int, b: Int): Int {
        return if ((x < a)) a else if ((x > b)) b else x
    }

    /**
     * Linear interpolation.
     *
     * @param t the interpolation parameter
     * @param a the lower interpolation range
     * @param b the upper interpolation range
     * @return the interpolated value
     */
    private fun lerp(t: Float, a: Int, b: Int): Int {
        return (a + t * (b - a)).toInt()
    }

    /**
     * Linear interpolation of ARGB values.
     *
     * @param t    the interpolation parameter
     * @param rgb1 the lower interpolation range
     * @param rgb2 the upper interpolation range
     * @return the interpolated value
     */
    @JvmStatic
    fun mixColors(t: Float, rgb1: Int, rgb2: Int): Int {
        var a1 = (rgb1 shr 24) and 0xff
        var r1 = (rgb1 shr 16) and 0xff
        var g1 = (rgb1 shr 8) and 0xff
        var b1 = rgb1 and 0xff
        val a2 = (rgb2 shr 24) and 0xff
        val r2 = (rgb2 shr 16) and 0xff
        val g2 = (rgb2 shr 8) and 0xff
        val b2 = rgb2 and 0xff
        a1 = lerp(t, a1, a2)
        r1 = lerp(t, r1, r2)
        g1 = lerp(t, g1, g2)
        b1 = lerp(t, b1, b2)
        return (a1 shl 24) or (r1 shl 16) or (g1 shl 8) or b1
    }
}
