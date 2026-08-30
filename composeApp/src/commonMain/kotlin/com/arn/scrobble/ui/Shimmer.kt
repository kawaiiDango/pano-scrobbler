package com.arn.scrobble.ui

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.cos
import kotlin.math.sin

private object ShimmerClock {

    var frameTimeMillis by mutableLongStateOf(0L)
        private set

    private var refCount = 0
    private var job: Job? = null

    fun start(hostScope: CoroutineScope) {
        if (refCount == 0) {
            // Borrow the MonotonicFrameClock from whichever node happens to attach first.
            // It's the same Choreographer-backed clock app-wide, so this is safe to reuse
            // in a longer-lived scope that outlives that one node.
            val frameClock = hostScope.coroutineContext[MonotonicFrameClock]
                ?: EmptyCoroutineContext
            val clockScope =
                CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + frameClock)
            job = clockScope.launch {
                while (isActive) {
                    withFrameNanos { nanos -> frameTimeMillis = nanos / 1_000_000L }
                }
            }
        }
        refCount++
    }

    fun stop() {
        refCount--
        if (refCount <= 0) {
            refCount = 0
            job?.cancel()
            job = null
        }
    }
}

fun Modifier.shimmerWindowBounds(
    enabled: Boolean = true,
): Modifier {
    if (!enabled) return this

    // Isolate this element's pixels into their own compositing layer so that the
    // BlendMode.DstIn draw below masks against *this element's own alpha* only,
    // instead of blending against whatever happens to be behind it on the canvas.
    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .then(ShimmerElement)
}

private data object ShimmerElement : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode()

    override fun update(node: ShimmerNode) {
        // nothing to update
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmerWindowBounds"
    }
}

private class ShimmerNode() : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    // This element's position relative to the compose root (~= window), and the root's
    // size. Updated on every layout pass, including ones caused by ancestor scrolling.
    private var positionInRoot: Offset = Offset.Zero
    private var rootSize: IntSize = IntSize.Zero

    override fun onAttach() {
        ShimmerClock.start(coroutineScope)
    }

    override fun onDetach() {
        ShimmerClock.stop()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        positionInRoot = coordinates.positionInRoot()
        rootSize = coordinates.findRootCoordinates().size
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        val windowWidth = rootSize.width.toFloat()
        val windowHeight = rootSize.height.toFloat()
        if (windowWidth <= 0f || windowHeight <= 0f) return

        // Reading this snapshot state here is what makes draw() automatically re-run every
        // frame — Modifier.Node's draw phase is snapshot-aware, so no manual invalidateDraw()
        // loop is needed. Every ShimmerNode reads the same value, so they all redraw in the
        // same frame with the same phase.
        val t = ShimmerClock.frameTimeMillis
        val phase = (t % periodMillis).toFloat() / periodMillis

        val bandWidth = windowWidth * bandWidthFraction

        // Project all four window corners onto the travel direction to find the full range
        // the band's "position" (u) needs to cover to sweep the entire window, whatever the
        // angle's sign.
        val u00 = 0f
        val u10 = windowWidth * projectedTiltX
        val u01 = windowHeight * projectedTiltY
        val u11 = windowWidth * projectedTiltX + windowHeight * projectedTiltY
        val uMin = minOf(minOf(u00, u10), minOf(u01, u11))
        val uMax = maxOf(maxOf(u00, u10), maxOf(u01, u11))

        val travel = (uMax - uMin) + bandWidth * 2f
        val bandCenterUWindow = uMin - bandWidth + phase * travel

        val elementOriginU = positionInRoot.x * projectedTiltX + positionInRoot.y * projectedTiltY
        val bandCenterULocal = bandCenterUWindow - elementOriginU

        val s1 = bandCenterULocal - bandWidth / 2f
        val s2 = bandCenterULocal + bandWidth / 2f

        val brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                highlightColor,
                highlightColor,
                baseColor,
            ),
            start = Offset(s1 * projectedTiltX, s1 * projectedTiltY),
            end = Offset(s2 * projectedTiltX, s2 * projectedTiltY),
        )

        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

    private companion object {
        val baseColor = Color.White.copy(alpha = 0.3f)
        val highlightColor = Color.White.copy(alpha = 0.7f)
        val bandWidthFraction = 0.25f
        val periodMillis = 1400

        // Direction the band travels in window space. At tiltDegrees = 0 this is pure
        // horizontal ((1,0)) and everything below collapses to a plain left-to-right sweep;
        // any nonzero angle tilts both the travel direction and the band itself together,
        // since a band is just the set of points with equal projection onto this direction.
        val angleRad = Math.toRadians(20.0)
        val projectedTiltX = cos(angleRad).toFloat()
        val projectedTiltY = sin(angleRad).toFloat()
    }
}