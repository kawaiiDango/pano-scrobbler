package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

@Composable
fun RowOrColumnLayout(
    isColumnMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        require(measurables.size == 2) { "RowOrColumnLayout requires exactly 2 children" }

        val child1 = measurables[0]
        val child2 = measurables[1]

        if (isColumnMode) {
            layoutAsColumn(child1, child2, constraints)
        } else {
            layoutAsRow(child1, child2, constraints)
        }
    }
}


private fun MeasureScope.layoutAsRow(
    child1: Measurable,
    child2: Measurable,
    constraints: Constraints
): MeasureResult {
    // Measure child1 with wrap content width
    val child1Placeable = child1.measure(
        constraints.copy(
            minWidth = 0,
            maxWidth = constraints.maxWidth
        )
    )

    // Calculate remaining width for child2
    val remainingWidth = (constraints.maxWidth - child1Placeable.width).coerceAtLeast(0)

    // Measure child2 with remaining width
    val child2Placeable = child2.measure(
        constraints.copy(
            minWidth = remainingWidth,
            maxWidth = remainingWidth
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
    constraints: Constraints
): MeasureResult {
    // Measure child2 with wrap content height
    val child2Placeable = child2.measure(
        constraints.copy(
            minHeight = 0,
            maxHeight = constraints.maxHeight
        )
    )

    // Calculate remaining height for child1
    val remainingHeight = (constraints.maxHeight - child2Placeable.height).coerceAtLeast(0)

    // Measure child1 with remaining height
    val child1Placeable = child1.measure(
        constraints.copy(
            minHeight = remainingHeight,
            maxHeight = remainingHeight
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