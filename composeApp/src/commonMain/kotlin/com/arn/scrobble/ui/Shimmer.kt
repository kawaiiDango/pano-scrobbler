package com.arn.scrobble.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val LocalShimmerController = staticCompositionLocalOf { ShimmerController }

/**
 * Drives a single shared shimmer progress value (0f..1f) for the whole process.
 *
 * The animation loop only runs while at least one composable is subscribed,
 * so it is completely idle when no shimmer is on screen.
 */
object ShimmerController {

    const val durationMs = 1_400

    // Monotonic start time in nanoseconds, set when the first shimmer
    // subscribes and cleared when the last one leaves. Modifiers use
    // this to compute the current phase so they can seed their own
    // Animatable at the right offset.
    private val startTimeNanos = MutableStateFlow<Long?>(null)

    /** Returns the current animation progress (0f..1f), or 0f if not running. */
    fun currentProgress(): Float {
        val start = startTimeNanos.value ?: return 0f
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        return (elapsedMs % durationMs).toFloat() / durationMs
    }

    // Never emits — exists only so subscriptionCount works correctly.
    val subscriptionTracker = MutableSharedFlow<Unit>(replay = 0)
    val subscriptionCount = subscriptionTracker.subscriptionCount

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            subscriptionCount
                .collect {
                    startTimeNanos.value = if (it > 0)
                        System.nanoTime()
                    else
                        null
                }
        }
    }
}


fun Modifier.shimmerWindowBounds(
    visible: Boolean = true,
): Modifier = if (!visible) this else composed {
    val windowSize = LocalWindowInfo.current.containerSize.toSize()
    val controller = LocalShimmerController.current

    // Read only in draw phase — never during composition.
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(controller) {
        launch {
            controller.subscriptionTracker.collect {}
        }

        val initialProgress = controller.currentProgress()

        animatable.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = controller.durationMs,
                    easing = FastOutLinearInEasing,
                ),
                repeatMode = RepeatMode.Restart,
                // Offset the repeat so the first iteration is
                // proportionally shorter, matching the remaining phase.
                initialStartOffset = StartOffset(
                    offsetMillis = (initialProgress * controller.durationMs).toInt(),
                    offsetType = StartOffsetType.FastForward,
                ),
            ),
        )
    }

    graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val stripeWidth = windowSize.width * 0.5f
        val sweepDistance = windowSize.width + stripeWidth

        onDrawWithContent {
            // Read only in draw phase — zero recompositions.
            val p = animatable.value
            val startX = -stripeWidth + sweepDistance * p
            val startY = -stripeWidth + sweepDistance * p

            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.2f),
                ),
                start = Offset(startX, startY),
                end = Offset(startX + stripeWidth, startY + stripeWidth),
            )

            drawContent()
            drawRect(brush = shimmerBrush, blendMode = BlendMode.DstIn)
        }
    }
}