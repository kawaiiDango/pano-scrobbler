package com.kennycason.kumo


import com.arn.scrobble.graphics.KumoPoint
import com.arn.scrobble.graphics.KumoRect
import com.arn.scrobble.graphics.PlatformBitmap
import com.arn.scrobble.graphics.PlatformCanvas
import com.kennycason.kumo.collide.Collidable
import com.kennycason.kumo.collide.checkers.RectanglePixelCollisionChecker
import com.kennycason.kumo.image.CollisionRaster

/**
 * Created by kenny on 6/29/14.
 */
class Word(
    val word: String,
    color: Int,
    fontMetricsCanvas: PlatformCanvas,
    private val collisionChecker: RectanglePixelCollisionChecker,
    override val position: KumoPoint = KumoPoint(0, 0),
) : Collidable {
    var bufferedImage: PlatformBitmap
        private set

    override var collisionRaster: CollisionRaster
        private set

    init {
        bufferedImage = render(word, color, fontMetricsCanvas)
        collisionRaster = CollisionRaster(bufferedImage)
    }

    private fun render(
        text: String,
        fontColor: Int,
        fontMetricsCanvas: PlatformCanvas,
    ): PlatformBitmap {
        // get the advance of my text in this font and render context
        val width = fontMetricsCanvas.measureText(text)
        // get the height of a line of text in this font and render context
        val height = fontMetricsCanvas.fontHeight

        val rendered = PlatformBitmap(width.toInt(), height.toInt())

        val gOfRendered = PlatformCanvas(rendered)

        gOfRendered.textSize = fontMetricsCanvas.textSize

        gOfRendered.setColor(fontColor)

        gOfRendered.drawText(
            text, 0f, height - fontMetricsCanvas.descent - fontMetricsCanvas.leading
        )
        return rendered
    }

    fun setBufferedImage(bufferedImage: PlatformBitmap) {
        this.bufferedImage = bufferedImage
        this.collisionRaster = CollisionRaster(bufferedImage)
    }

    override val dimension: KumoRect
        get() = collisionRaster.dimension

    override fun collide(collidable: Collidable): Boolean {
        return collisionChecker.collide(this, collidable)
    }

}
