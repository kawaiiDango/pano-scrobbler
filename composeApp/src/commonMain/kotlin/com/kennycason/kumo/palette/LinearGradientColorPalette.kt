package com.kennycason.kumo.palette

/**
 * A LinearGradient ColorPalette
 *
 * @author &#64;wolfposd
 */
class LinearGradientColorPalette(
    color1: Int,
    color2: Int,
    color3: Int? = null,
    gradientSteps: Int,
) {
    /**
     *
     *
     * Creates a ColorPalette using a linear gradient between the two colors
     * specified
     *
     *
     *
     * The following example code will generate a gradient between red and blue,
     * with 18 colors in between red and blue, thus totaling 20 colors:
     *
     * `
     * new LinearGradientColorPalette(Color.RED, 18, Color.Blue)
    ` *
     *
     * @param color1
     * start color
     * @param color2
     * end color
     * @param gradientSteps
     * specifies the amount of colors for this gradient between
     * color1 and color2
     */
    private var next = 0

    private val colors = if (color3 != null) {
        createTwoLinearGradients(color1, color2, color3, gradientSteps / 2, gradientSteps / 2)
    } else {
        createLinearGradient(color1, color2, gradientSteps)
    }

    fun next() = colors[next++ % colors.size]!!

    fun randomNext() = colors[(0 until colors.size).random()]!!

    companion object {
        private fun createTwoLinearGradients(
            color1: Int,
            color2: Int,
            color3: Int,
            gradientStepsC1AndC2: Int,
            gradientStepsC2AndC3: Int,
        ): MutableList<Int?> {
            val colors: MutableList<Int?> = ArrayList<Int?>()

            val gradient1 = createLinearGradient(color1, color2, gradientStepsC1AndC2)
            val gradient2 = createLinearGradient(color2, color3, gradientStepsC2AndC3)

            colors.addAll(gradient1)
            // the first item will overlap with the color2, so ignore it
            colors.addAll(gradient2.subList(1, gradient2.size))

            return colors
        }

        /**
         * Creates a linear Gradient between two colors
         *
         * @param color1
         * start color
         * @param color2
         * end color
         * @param gradientSteps
         * specifies the amount of colors in this gradient between color1
         * and color2, this includes both color1 and color2
         * @return List of colors in this gradient
         */
        private fun createLinearGradient(
            color1: Int,
            color2: Int,
            gradientSteps: Int,
        ): MutableList<Int> {
            val colors: MutableList<Int> = ArrayList<Int>(gradientSteps + 1)

            // add beginning color to the gradient
            colors.add(color1)

            for (i in 1..<gradientSteps) {
                val ratio = i.toFloat() / gradientSteps.toFloat()

                val red = (getRed(color2) * ratio + getRed(color1) * (1 - ratio)).toInt()
                val green = (getGreen(color2) * ratio + getGreen(color1) * (1 - ratio)).toInt()
                val blue = (getBlue(color2) * ratio + getBlue(color1) * (1 - ratio)).toInt()

                colors.add(rgb(red, green, blue))
            }
            // add end color to the gradient
            colors.add(color2)

            return colors
        }

        private fun getRed(color: Int): Int {
            return (color shr 16) and 0xFF
        }

        private fun getGreen(color: Int): Int {
            return (color shr 8) and 0xFF
        }

        private fun getBlue(color: Int): Int {
            return color and 0xFF
        }

        private fun rgb(red: Int, green: Int, blue: Int): Int {
            return -0x1000000 or (red shl 16) or (green shl 8) or blue
        }
    }
}
