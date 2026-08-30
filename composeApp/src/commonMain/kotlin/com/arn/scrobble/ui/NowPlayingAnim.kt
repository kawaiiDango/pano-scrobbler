package com.arn.scrobble.ui

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random


// Two slowly breathing radial gradients
fun Modifier.nowPlayingAnim(
    nowPlaying: Boolean,
    colorA: Color,
    colorB: Color,
): Modifier = if (!nowPlaying) this else then(NowPlayingAnimElement(colorA, colorB))

private data class NowPlayingAnimElement(
    val colorA: Color,
    val colorB: Color,
) : ModifierNodeElement<NowPlayingAnimNode>() {

    override fun create() = NowPlayingAnimNode(colorA, colorB)

    override fun update(node: NowPlayingAnimNode) {
        node.colorA = colorA
        node.colorB = colorB
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "nowPlayingAnim"
    }
}

private class NowPlayingAnimNode(
    var colorA: Color,
    var colorB: Color,
) : Modifier.Node(), DrawModifierNode {

    // Set once when the node is created, persists for as long as it stays
    // attached at this call site — the Modifier.Node equivalent of `remember`.
    private val reverseColors = Random.nextBoolean()
    private var phase1 = Random.nextFloat()
    private var phase2 = Random.nextFloat()

    override fun onAttach() {
        coroutineScope.launch {
            var lastFrameNanos = 0L
            var accumulatedNanos = 0L
            while (isActive) {
                val frameNanos = withFrameNanos { it }
                if (lastFrameNanos != 0L) {
                    accumulatedNanos += frameNanos - lastFrameNanos
                    if (accumulatedNanos >= TARGET_FRAME_NANOS) {
                        val deltaMillis = accumulatedNanos / 1_000_000f
                        phase1 += deltaMillis / PERIOD_1_MS
                        phase2 += deltaMillis / PERIOD_2_MS
                        accumulatedNanos = 0L
                        invalidateDraw()
                    }
                }
                lastFrameNanos = frameNanos
            }
        }
    }
    // No onDetach override needed: coroutineScope is cancelled automatically on detach.

    private fun pingPong(raw: Float): Float {
        val t = raw % 1f
        return if (t < 0.5f) t * 2f else (1f - t) * 2f
    }

    override fun ContentDrawScope.draw() {
        val radius = size.maxDimension * 0.9f
        val (fgColor1, fgColor2) = if (reverseColors) colorB to colorA else colorA to colorB

        val breath1 = pingPong(phase1)
        drawRect(
            brush = Brush.radialGradient(
                0f to fgColor1,
                0.6f to fgColor1.copy(alpha = fgColor1.alpha * 0.4f),
                1f to fgColor2.copy(alpha = fgColor2.alpha * 0.2f),
                center = Offset(
                    x = size.width * lerp(-0.1f, 0.4f, breath1),
                    y = size.height * lerp(-0.1f, 0.4f, breath1),
                ),
                radius = radius,
            )
        )

        val breath2 = pingPong(phase2)
        drawRect(
            brush = Brush.radialGradient(
                0f to fgColor2,
                0.6f to fgColor2.copy(alpha = fgColor2.alpha * 0.4f),
                1f to fgColor2.copy(alpha = fgColor2.alpha * 0.2f),
                center = Offset(
                    x = size.width * lerp(0.6f, 1.1f, breath2),
                    y = size.height * lerp(0.6f, 1.1f, breath2),
                ),
                radius = radius,
            )
        )

        drawContent()
    }

    private companion object {
        const val PERIOD_1_MS = 20_000f
        const val PERIOD_2_MS = 27_000f
        const val TARGET_FRAME_NANOS = 1_000_000_000L / 15 // cap redraws at 15fps
    }
}