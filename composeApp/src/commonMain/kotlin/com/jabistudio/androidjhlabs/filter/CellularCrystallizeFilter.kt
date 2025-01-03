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
package com.jabistudio.androidjhlabs.filter

import com.jabistudio.androidjhlabs.filter.math.Function2D
import com.jabistudio.androidjhlabs.filter.math.ImageMath.clamp
import com.jabistudio.androidjhlabs.filter.math.ImageMath.mixColors
import com.jabistudio.androidjhlabs.filter.math.ImageMath.smoothStep
import com.jabistudio.androidjhlabs.filter.math.Noise.Companion.noise2
import java.util.Random
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A filter which produces an image with a cellular texture.
 */
class CellularCrystallizeFilter : Function2D {
    /**
     * Returns the scale of the texture.
     *
     * @return the scale of the texture.
     * @see .setScale
     */
    /**
     * Specifies the scale of the texture.
     *
     * @param scale the scale of the texture.
     * @min-value 1
     * @max-value 300+
     * @see .getScale
     */
    var scale: Float = 32f
    /**
     * Returns the stretch factor of the texture.
     *
     * @return the stretch factor of the texture.
     * @see .setStretch
     */
    /**
     * Specifies the stretch factor of the texture.
     *
     * @param stretch the stretch factor of the texture.
     * @min-value 1
     * @max-value 50+
     * @see .getStretch
     */
    var stretch: Float = 1.0f
    private var angle: Float = 0.0f
    /**
     * Get the amount of texture.
     *
     * @return the amount
     * @see .setAmount
     */
    /**
     * Set the amount of effect.
     *
     * @param amount the amount
     * @min-value 0
     * @max-value 1
     * @see .getAmount
     */
    var amount: Float = 1.0f
    /**
     * Returns the turbulence of the effect.
     *
     * @return the turbulence of the effect.
     * @see .setTurbulence
     */
    /**
     * Specifies the turbulence of the texture.
     *
     * @param turbulence the turbulence of the texture.
     * @min-value 0
     * @max-value 1
     * @see .getTurbulence
     */
    var turbulence: Float = 1.0f
    var gain: Float = 0.5f
    var bias: Float = 0.5f
    var distancePower: Float = 2f
    var useColor: Boolean = false
    private var coefficients: FloatArray = floatArrayOf(1f, 0f, 0f, 0f)
    var angleCoefficient: Float = 0f
    private var random: Random = Random()
    private var m00: Float = 1.0f
    private var m01: Float = 0.0f
    private var m10: Float = 0.0f
    private var m11: Float = 1.0f
    private var results: Array<Point?>? = null
    var randomness: Float = 0f
    var gridType: Int = HEXAGONAL
    private val min = 0f
    private val max = 0f
    var gradientCoefficient: Float = 0f

    var edgeThickness: Float = 0.4f

    var fadeEdges: Boolean = false

    var edgeColor: Int = -0x1000000

    init {
        scale = 16f
        randomness = 0.0f

        results = arrayOfNulls(3)
        for (j in results!!.indices) results!![j] = Point()
        if (probabilities == null) {
            probabilities = ByteArray(8192)
            var factorial = 1f
            var total = 0f
            val mean = 2.5f
            for (i in 0..9) {
                if (i > 1) factorial *= i.toFloat()
                val probability = mean.pow(i) * exp(-mean) / factorial
                val start = (total * 8192).toInt()
                total += probability
                val end = (total * 8192).toInt()
                for (j in start until end) probabilities!![j] = i.toByte()
            }
        }
    }


    fun filter(src: IntArray?, w: Int, h: Int): IntArray {
        var inPixels = src
        inPixels = filterPixels(w, h, inPixels!!)

        return inPixels
    }

    /**
     * Specifies the angle of the texture.
     *
     * @param angle the angle of the texture.
     * @angle
     * @see .getAngle
     */
    fun setAngle(angle: Float) {
        this.angle = angle
        val cos = cos(angle.toDouble()).toFloat()
        val sin = sin(angle.toDouble()).toFloat()
        m00 = cos
        m01 = sin
        m10 = -sin
        m11 = cos
    }

    /**
     * Returns the angle of the texture.
     *
     * @return the angle of the texture.
     * @see .setAngle
     */
    fun getAngle(): Float {
        return angle
    }

    fun setCoefficient(i: Int, v: Float) {
        coefficients[i] = v
    }

    fun getCoefficient(i: Int): Float {
        return coefficients[i]
    }

    var f1: Float
        get() = coefficients[0]
        set(v) {
            coefficients[0] = v
        }

    var f2: Float
        get() = coefficients[1]
        set(v) {
            coefficients[1] = v
        }

    var f3: Float
        get() = coefficients[2]
        set(v) {
            coefficients[2] = v
        }

    var f4: Float
        get() = coefficients[3]
        set(v) {
            coefficients[3] = v
        }

    inner class Point {
        var index: Int = 0
        var x: Float = 0f
        var y: Float = 0f
        var dx: Float = 0f
        var dy: Float = 0f
        var cubeX: Float = 0f
        var cubeY: Float = 0f
        var distance: Float = 0f
    }

    private fun checkCube(
        x: Float,
        y: Float,
        cubeX: Int,
        cubeY: Int,
        results: Array<Point?>?,
    ): Float {
        random.setSeed((571 * cubeX + 23 * cubeY).toLong())
        val numPoints = when (gridType) {
            RANDOM -> probabilities!![random.nextInt() and 0x1fff].toInt()
            SQUARE -> 1
            HEXAGONAL -> 1
            OCTAGONAL -> 2
            TRIANGULAR -> 2
            else -> probabilities!![random.nextInt() and 0x1fff].toInt()
        }
        for (i in 0 until numPoints) {
            var px = 0f
            var py = 0f
            var weight = 1.0f
            when (gridType) {
                RANDOM -> {
                    px = random.nextFloat()
                    py = random.nextFloat()
                }

                SQUARE -> {
                    run {
                        py = 0.5f
                        px = py
                    }
                    if (randomness != 0f) {
                        px += (randomness * (random.nextFloat() - 0.5)).toFloat()
                        py += (randomness * (random.nextFloat() - 0.5)).toFloat()
                    }
                }

                HEXAGONAL -> {
                    if ((cubeX and 1) == 0) {
                        px = 0.75f
                        py = 0f
                    } else {
                        px = 0.75f
                        py = 0.5f
                    }
                    if (randomness != 0f) {
                        px += randomness * noise2(271 * (cubeX + px), 271 * (cubeY + py))
                        py += randomness * noise2(271 * (cubeX + px) + 89, 271 * (cubeY + py) + 137)
                    }
                }

                OCTAGONAL -> {
                    when (i) {
                        0 -> {
                            px = 0.207f
                            py = 0.207f
                        }

                        1 -> {
                            px = 0.707f
                            py = 0.707f
                            weight = 1.6f
                        }
                    }
                    if (randomness != 0f) {
                        px += randomness * noise2(271 * (cubeX + px), 271 * (cubeY + py))
                        py += randomness * noise2(271 * (cubeX + px) + 89, 271 * (cubeY + py) + 137)
                    }
                }

                TRIANGULAR -> {
                    if ((cubeY and 1) == 0) {
                        if (i == 0) {
                            px = 0.25f
                            py = 0.35f
                        } else {
                            px = 0.75f
                            py = 0.65f
                        }
                    } else {
                        if (i == 0) {
                            px = 0.75f
                            py = 0.35f
                        } else {
                            px = 0.25f
                            py = 0.65f
                        }
                    }
                    if (randomness != 0f) {
                        px += randomness * noise2(271 * (cubeX + px), 271 * (cubeY + py))
                        py += randomness * noise2(271 * (cubeX + px) + 89, 271 * (cubeY + py) + 137)
                    }
                }
            }
            var dx = abs((x - px).toDouble()).toFloat()
            var dy = abs((y - py).toDouble()).toFloat()
            dx *= weight
            dy *= weight
            val d = if (distancePower == 1.0f) dx + dy
            else if (distancePower == 2.0f) sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            else (dx.pow(distancePower) + dy.pow(distancePower)).pow(
                (1 / distancePower)
            )

            // Insertion sort the long way round to speed it up a bit
            if (d < results!![0]!!.distance) {
                val p = results[2]
                results[2] = results[1]
                results[1] = results[0]
                results[0] = p
                p!!.distance = d
                p.dx = dx
                p.dy = dy
                p.x = cubeX + px
                p.y = cubeY + py
            } else if (d < results[1]!!.distance) {
                val p = results[2]
                results[2] = results[1]
                results[1] = p
                p!!.distance = d
                p.dx = dx
                p.dy = dy
                p.x = cubeX + px
                p.y = cubeY + py
            } else if (d < results[2]!!.distance) {
                val p = results[2]
                p!!.distance = d
                p.dx = dx
                p.dy = dy
                p.x = cubeX + px
                p.y = cubeY + py
            }
        }
        return results!![2]!!.distance
    }

    override fun evaluate(x: Float, y: Float): Float {
        for (j in results!!.indices) results!![j]!!.distance = Float.POSITIVE_INFINITY

        val ix = x.toInt()
        val iy = y.toInt()
        val fx = x - ix
        val fy = y - iy

        var d = checkCube(fx, fy, ix, iy, results)
        if (d > fy) d = checkCube(fx, fy + 1, ix, iy - 1, results)
        if (d > 1 - fy) d = checkCube(fx, fy - 1, ix, iy + 1, results)
        if (d > fx) {
            checkCube(fx + 1, fy, ix - 1, iy, results)
            if (d > fy) d = checkCube(fx + 1, fy + 1, ix - 1, iy - 1, results)
            if (d > 1 - fy) d = checkCube(fx + 1, fy - 1, ix - 1, iy + 1, results)
        }
        if (d > 1 - fx) {
            d = checkCube(fx - 1, fy, ix + 1, iy, results)
            if (d > fy) d = checkCube(fx - 1, fy + 1, ix + 1, iy - 1, results)
            if (d > 1 - fy) d = checkCube(fx - 1, fy - 1, ix + 1, iy + 1, results)
        }

        var t = 0f
        for (i in 0..2) t += coefficients[i] * results!![i]!!.distance
        if (angleCoefficient != 0f) {
            var angle = atan2((y - results!![0]!!.y).toDouble(), (x - results!![0]!!.x).toDouble())
                .toFloat()
            if (angle < 0) angle += 2 * Math.PI.toFloat()
            angle /= 4 * Math.PI.toFloat()
            t += angleCoefficient * angle
        }
        if (gradientCoefficient != 0f) {
            val a = 1 / (results!![0]!!.dy + results!![0]!!.dx)
            t += gradientCoefficient * a
        }
        return t
    }

    fun getPixel(x: Int, y: Int, inPixels: IntArray, width: Int, height: Int): Int {
        var nx = m00 * x + m01 * y
        var ny = m10 * x + m11 * y
        nx /= scale
        ny /= scale * stretch
        nx += 1000f
        ny += 1000f // Reduce artifacts around 0,0
        var f: Float
        evaluate(nx, ny)

        val f1 = results!![0]!!.distance
        val f2 = results!![1]!!.distance
        var srcx = clamp(((results!![0]!!.x - 1000) * scale).toInt(), 0, width - 1)
        var srcy = clamp(((results!![0]!!.y - 1000) * scale).toInt(), 0, height - 1)
        var v = inPixels[srcy * width + srcx]
        f = (f2 - f1) / edgeThickness
        f = smoothStep(0f, edgeThickness, f)
        if (fadeEdges) {
            srcx = clamp(((results!![1]!!.x - 1000) * scale).toInt(), 0, width - 1)
            srcy = clamp(((results!![1]!!.y - 1000) * scale).toInt(), 0, height - 1)
            var v2 = inPixels[srcy * width + srcx]
            v2 = mixColors(0.5f, v2, v)
            v = mixColors(f, v2, v)
        } else v = mixColors(f, edgeColor, v)
        return v
    }

    override fun toString(): String {
        return "Texture/Cellular..."
    }

    fun filterPixels(
        width: Int, height: Int, inPixels: IntArray,
    ): IntArray {
        var index = 0
        val outPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                outPixels[index++] = getPixel(x, y, inPixels, width, height)
            }
        }
        return outPixels
    }

    companion object {
        private var probabilities: ByteArray? = null
        const val RANDOM: Int = 0
        const val SQUARE: Int = 1
        const val HEXAGONAL: Int = 2
        const val OCTAGONAL: Int = 3
        const val TRIANGULAR: Int = 4
    }
}
