package com.kennycason.kumo

import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.arn.scrobble.graphics.PlatformBitmap
import com.arn.scrobble.graphics.PlatformCanvas
import com.kennycason.kumo.bg.Background
import com.kennycason.kumo.collide.RectanglePixelCollidable
import com.kennycason.kumo.collide.checkers.RectanglePixelCollisionChecker
import com.kennycason.kumo.image.CollisionRaster
import com.kennycason.kumo.padding.WordPixelPadder
import com.kennycason.kumo.palette.LinearGradientColorPalette
import com.kennycason.kumo.scale.FontScalar
import com.kennycason.kumo.wordstart.RandomWordStart
import com.kennycason.kumo.wordstart.WordStartStrategy
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Created by kenny on 6/29/14.
 */
class WordCloud(
    private val dimension: KumoRect,
    private val background: Background,
    private val padding: Int,
    private val fontScalar: FontScalar,
    private val colorPalette: LinearGradientColorPalette,
    private val wordStartStrategy: WordStartStrategy = RandomWordStart(),
) {
    private val collisionChecker = RectanglePixelCollisionChecker()
    private val collisionRaster = CollisionRaster(dimension)
    private val backgroundCollidable = RectanglePixelCollidable(collisionRaster, KumoPoint(0, 0))
    val bitmap = PlatformBitmap(dimension.width, dimension.height)
    private val padder = WordPixelPadder()
    val skipped = mutableSetOf<Word>()
    private val fontMetricsCanvas: PlatformCanvas

    init {
        val onePixel = PlatformBitmap(1, 1)
        fontMetricsCanvas = PlatformCanvas(onePixel)
    }

    fun build(wordFrequencies: List<WordFrequency>): WordCloud {
        skipped.clear()

        // the background masks all none usable pixels and we can only check this raster
        background.mask(backgroundCollidable)

        var currentWord = 1
        for (word in buildWords(wordFrequencies.sorted())) {
            val point: KumoPoint = wordStartStrategy.getStartingPoint(dimension, word)
            val placed = place(word, point)

            if (!placed) {
                skipped.add(word)
            }

            currentWord++
        }

        return this
    }

    /**
     * try to place in center, build out in a spiral trying to place words for N steps
     * @param word the word being placed
     * @param start the place to start trying to place the word
     */
    private fun place(word: Word, start: KumoPoint): Boolean {
        val graphics = PlatformCanvas(bitmap)

        val maxRadius = computeRadius(dimension, start)
        val position: KumoPoint = word.position

        var r = 0
        while (r < maxRadius) {
            for (x in (-start.x).coerceAtLeast(-r)..r.coerceAtMost(dimension.width - start.x - 1)) {
                position.x = start.x + x

                val offset = sqrt((r * r - x * x).toDouble()).toInt()


                // try positive root
                position.y = start.y + offset
                if (position.y >= 0 && position.y < dimension.height && canPlace(word)) {
                    collisionRaster.mask(word.collisionRaster, position)
                    graphics.drawBitmap(word.bufferedImage, position.x, position.y)
                    return true
                }


                // try negative root (if offset != 0)
                position.y = start.y - offset
                if (offset != 0 && position.y >= 0 && position.y < dimension.height &&
                    canPlace(word)
                ) {
                    collisionRaster.mask(word.collisionRaster, position)
                    graphics.drawBitmap(word.bufferedImage, position.x, position.y)
                    return true
                }
            }
            r += 2
        }

        return false
    }

    private fun canPlace(word: Word): Boolean {
        val position: KumoPoint = word.position
        val dimensionOfWord: KumoRect = word.dimension


        // are we inside the background?
        if (position.y < 0 || position.y + dimensionOfWord.height > dimension.height) {
            return false
        } else if (position.x < 0 || position.x + dimensionOfWord.width > dimension.width) {
            return false
        }

        return !backgroundCollidable.collide(word) // is there a collision with the background shape?
    }

    private fun buildWords(wordFrequencies: List<WordFrequency>): List<Word> {
        val maxFrequency = maxFrequency(wordFrequencies)
        return wordFrequencies.filter { it.word.isNotEmpty() }
            .map { buildWord(it, maxFrequency) }
    }

    private fun buildWord(
        wordFrequency: WordFrequency,
        maxFrequency: Int,
    ): Word {
        val frequency = wordFrequency.frequency
        val fontHeight = fontScalar.scale(frequency, maxFrequency)
        fontMetricsCanvas.textSize = fontHeight

        val word = Word(
            wordFrequency.word,
            colorPalette.next(),
            fontMetricsCanvas,
            collisionChecker,
        )

        if (padding > 0) {
            padder.pad(word, padding)
        }
        return word
    }

    companion object {
        /**
         * compute the maximum radius for the placing spiral
         *
         * @param dimension the size of the backgound
         * @param start the center of the spiral
         * @return the maximum usefull radius
         */
        fun computeRadius(dimension: KumoRect, start: KumoPoint): Int {
            val maxDistanceX: Int = start.x.coerceAtLeast(dimension.width - start.x) + 1
            val maxDistanceY: Int = start.y.coerceAtLeast(dimension.height - start.y) + 1


            // we use the pythagorean theorem to determinate the maximum radius
            return ceil(sqrt((maxDistanceX * maxDistanceX + maxDistanceY * maxDistanceY).toDouble())).toInt()
        }

        private fun maxFrequency(wordFrequencies: List<WordFrequency>): Int {
            return wordFrequencies.firstOrNull()?.frequency ?: 1
        }
    }
}
