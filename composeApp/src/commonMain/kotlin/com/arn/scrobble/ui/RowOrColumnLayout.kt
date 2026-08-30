package com.arn.scrobble.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun RowOrColumnLayout(
    isColumnMode: Boolean,
    modifier: Modifier = Modifier,
    imageAnimationSpec: AnimationSpec<Float> = tween(),
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Animated side length (px) of the square image box, shared across every mode below.
    // -1 means "unset": nothing to animate from yet, so the next measure pass that wants a
    // computed size snaps straight to its target instead of animating.
    val imageSizeAnim = remember { Animatable(-1f) }

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        require(measurables.size == 2) { "RowOrColumnLayout requires exactly 2 children" }

        val child1 = measurables[0]
        val child2 = measurables[1]

        if (isColumnMode) {
            layoutAsColumn(
                child1,
                child2,
                constraints,
                imageSizeAnim,
                coroutineScope,
                imageAnimationSpec,
            )
        } else {
            layoutAsRow(
                child1,
                child2,
                constraints,
                imageSizeAnim,
                coroutineScope,
                imageAnimationSpec,
            )
        }
    }
}

/**
 * Resolves the actual pixel size to use for THIS measure pass, and kicks off (or retargets)
 * the animation towards [targetSize] if needed.
 *
 * Reads/writes [imageSizeAnim], which is a snapshot-backed value - reading `.value` here (from
 * inside a measure block) is what makes Compose automatically re-measure on every animation
 * frame without needing recomposition.
 */
private fun resolveAnimatedImageSize(
    targetSize: Int,
    imageSizeAnim: Animatable<Float, AnimationVector1D>,
    coroutineScope: CoroutineScope,
    animationSpec: AnimationSpec<Float>,
): Int {
    val storedSize = imageSizeAnim.value

    if (storedSize < 0f) {
        // Nothing to animate from - snap directly to the target.
        coroutineScope.launch { imageSizeAnim.snapTo(targetSize.toFloat()) }
        return targetSize
    }

    // Only (re)start the animation when the target actually changed - this measure block
    // reruns on every animation frame (it reads imageSizeAnim.value), so without this check
    // we'd restart from scratch every frame and it would never converge.
    if (imageSizeAnim.targetValue != targetSize.toFloat()) {
        coroutineScope.launch {
            imageSizeAnim.animateTo(targetSize.toFloat(), animationSpec)
        }
    }

    return storedSize.toInt().coerceAtLeast(0)
}

private fun MeasureScope.layoutAsRow(
    child1: Measurable,
    child2: Measurable,
    constraints: Constraints,
    imageSizeAnim: Animatable<Float, AnimationVector1D>,
    coroutineScope: CoroutineScope,
    animationSpec: AnimationSpec<Float>,
): MeasureResult {
    val child1Placeable = if (constraints.hasBoundedHeight) {
        // Bounded (finite) height budget from outside -> unbounded/computed image: fill it,
        // reserving just enough width for the caption's minimum needs. minIntrinsicWidth
        // already respects e.g. a widthIn(min = ...) on the caption, so that floor applies
        // here for free. Swap to maxIntrinsicWidth if the caption ends up too cramped.
        val captionMinWidth = child2
            .minIntrinsicWidth(constraints.maxHeight)
            .coerceAtMost(constraints.maxWidth)

        val remainingWidthForImage = (constraints.maxWidth - captionMinWidth).coerceAtLeast(0)
        val targetImageSize = min(constraints.maxHeight, remainingWidthForImage)

        val imageSize = resolveAnimatedImageSize(
            targetImageSize,
            imageSizeAnim,
            coroutineScope,
            animationSpec
        )

        child1.measure(Constraints.fixed(imageSize, imageSize))
    } else {
        // Unbounded height from outside -> bounded/compact image: let its own modifiers
        // (size/aspectRatio) decide, this layout doesn't compute anything.
        val placeable = child1.measure(
            constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth)
        )

        // Keep the shared animatable seeded with whatever's actually on screen, so if we
        // switch into a computed mode (unbounded row, or column) next frame, it animates
        // from here instead of a stale value.
        val currentWidth = placeable.width.toFloat()
        if (imageSizeAnim.value != currentWidth) {
            coroutineScope.launch { imageSizeAnim.snapTo(currentWidth) }
        }

        placeable
    }

    val remainingWidth = (constraints.maxWidth - child1Placeable.width).coerceAtLeast(0)
    val child2Placeable = child2.measure(
        constraints.copy(
            minWidth = remainingWidth,
            maxWidth = remainingWidth,
            minHeight = 0,
            maxHeight = constraints.maxHeight
        )
    )

    val totalWidth = child1Placeable.width + child2Placeable.width
    val totalHeight = max(child1Placeable.height, child2Placeable.height)

    return layout(totalWidth, totalHeight) {
        // Vertically center both children in row mode
        val child1Y = (totalHeight - child1Placeable.height) / 2
        val child2Y = (totalHeight - child2Placeable.height) / 2

        child1Placeable.placeRelative(0, child1Y)
        child2Placeable.placeRelative(child1Placeable.width, child2Y)
    }
}

private fun MeasureScope.layoutAsColumn(
    child1: Measurable,
    child2: Measurable,
    constraints: Constraints,
    imageSizeAnim: Animatable<Float, AnimationVector1D>,
    coroutineScope: CoroutineScope,
    animationSpec: AnimationSpec<Float>,
): MeasureResult {
    val availableWidth = constraints.maxWidth

    // Caption's natural width, ignoring its internal weight(1f) expansion —
    // same mechanism IntrinsicSize.Min/Max uses. Capped to what's actually on screen.
    // Only meaningful when we have a real (bounded) height to reserve space against.
    val captionIntrinsicWidth = if (constraints.hasBoundedHeight) {
        child2
            .maxIntrinsicWidth(constraints.maxHeight)
            .coerceAtMost(availableWidth)
    } else {
        availableWidth
    }

    // Caption's height doesn't depend on its final width here
    val captionHeight = child2.minIntrinsicHeight(captionIntrinsicWidth)

    val remainingHeight = (constraints.maxHeight - captionHeight).coerceAtLeast(0)
    val targetImageSize = min(remainingHeight, availableWidth)

    val imageSize = resolveAnimatedImageSize(
        targetImageSize,
        imageSizeAnim,
        coroutineScope,
        animationSpec
    )

    val child1Placeable = child1.measure(Constraints.fixed(imageSize, imageSize))

    // Match the image width if the caption wants less than that (so bounds align
    // exactly); otherwise let it take the width it actually needs, capped to available space.
    val captionWidth = max(imageSize, captionIntrinsicWidth).coerceAtMost(availableWidth)

    val child2Placeable = child2.measure(
        Constraints(
            minWidth = captionWidth,
            maxWidth = captionWidth,
            minHeight = 0,
            maxHeight = constraints.maxHeight
        )
    )

    val totalWidth = max(child1Placeable.width, child2Placeable.width)
    val totalHeight = child1Placeable.height + child2Placeable.height

    return layout(totalWidth, totalHeight) {
        // Horizontally center both children in column mode
        val child1X = (totalWidth - child1Placeable.width) / 2
        val child2X = (totalWidth - child2Placeable.width) / 2

        child1Placeable.placeRelative(child1X, 0)
        child2Placeable.placeRelative(child2X, child1Placeable.height)
    }
}