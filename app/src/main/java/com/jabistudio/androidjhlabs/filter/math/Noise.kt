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

import java.util.Random
import kotlin.math.sqrt

/**
 * Perlin Noise functions
 */
class Noise : Function2D {
    override fun evaluate(x: Float, y: Float): Float {
        return noise2(x, y)
    }

    companion object {
        private val randomGenerator = Random()

        private const val B = 0x100
        private const val BM = 0xff
        private const val N = 0x1000

        var p: IntArray = IntArray(B + B + 2)
        var g3: Array<FloatArray> = Array(B + B + 2) { FloatArray(3) }
        var g2: Array<FloatArray> = Array(B + B + 2) { FloatArray(2) }
        var g1: FloatArray = FloatArray(B + B + 2)
        var start: Boolean = true

        private fun sCurve(t: Float): Float {
            return t * t * (3.0f - 2.0f * t)
        }

        /**
         * Compute 2-dimensional Perlin noise.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @return noise value at (x,y)
         */
        @JvmStatic
        fun noise2(x: Float, y: Float): Float {
            val bx0: Int
            val by0: Int
            val b00: Int
            val b10: Int
            val b01: Int
            val b11: Int
            val rx0: Float
            val ry0: Float
            val a: Float
            val b: Float
            var u: Float
            var v: Float
            val j: Int

            if (start) {
                start = false
                init()
            }

            var t = x + N
            bx0 = (t.toInt()) and BM
            val bx1 = bx0 + 1 and BM
            rx0 = t - t.toInt()
            val rx1 = rx0 - 1.0f

            t = y + N
            by0 = (t.toInt()) and BM
            val by1 = by0 + 1 and BM
            ry0 = t - t.toInt()
            val ry1 = ry0 - 1.0f

            val i = p[bx0]
            j = p[bx1]

            b00 = p[i + by0]
            b10 = p[j + by0]
            b01 = p[i + by1]
            b11 = p[j + by1]

            val sx = sCurve(rx0)
            val sy = sCurve(ry0)

            var q = g2[b00]
            u = rx0 * q[0] + ry0 * q[1]
            q = g2[b10]
            v = rx1 * q[0] + ry0 * q[1]
            a = lerp(sx, u, v)

            q = g2[b01]
            u = rx0 * q[0] + ry1 * q[1]
            q = g2[b11]
            v = rx1 * q[0] + ry1 * q[1]
            b = lerp(sx, u, v)

            return 1.5f * lerp(sy, a, b)
        }

        fun lerp(t: Float, a: Float, b: Float): Float {
            return a + t * (b - a)
        }

        private fun normalize2(v: FloatArray) {
            val s = sqrt((v[0] * v[0] + v[1] * v[1]).toDouble())
                .toFloat()
            v[0] = v[0] / s
            v[1] = v[1] / s
        }

        fun normalize3(v: FloatArray) {
            val s = sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble())
                .toFloat()
            v[0] = v[0] / s
            v[1] = v[1] / s
            v[2] = v[2] / s
        }

        private fun random(): Int {
            return randomGenerator.nextInt() and 0x7fffffff
        }

        private fun init() {
            var j: Int
            var k: Int

            var i = 0
            while (i < B) {
                p[i] = i

                g1[i] = ((random() % (B + B)) - B).toFloat() / B

                j = 0
                while (j < 2) {
                    g2[i][j] = ((random() % (B + B)) - B).toFloat() / B
                    j++
                }
                normalize2(g2[i])

                j = 0
                while (j < 3) {
                    g3[i][j] = ((random() % (B + B)) - B).toFloat() / B
                    j++
                }
                normalize3(g3[i])
                i++
            }

            i = B - 1
            while (i >= 0) {
                k = p[i]
                p[i] = p[(random() % B).also { j = it }]
                p[j] = k
                i--
            }

            i = 0
            while (i < B + 2) {
                p[B + i] = p[i]
                g1[B + i] = g1[i]
                j = 0
                while (j < 2) {
                    g2[B + i][j] = g2[i][j]
                    j++
                }
                j = 0
                while (j < 3) {
                    g3[B + i][j] = g3[i][j]
                    j++
                }
                i++
            }
        }
    }
}
