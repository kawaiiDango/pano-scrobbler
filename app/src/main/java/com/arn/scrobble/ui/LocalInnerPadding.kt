package com.arn.scrobble.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

val LocalInnerPadding = compositionLocalOf { PaddingValues(0.dp) }

@Composable
fun Modifier.addColumnPadding() = verticalScroll(rememberScrollState())
    .padding(panoContentPadding())

@Composable
fun panoContentPadding(sides: Boolean = true, bottom: Boolean = true): PaddingValues {
    val safeDrawingPaddingValues = WindowInsets.safeDrawing.asPaddingValues()
    val innerPadding = LocalInnerPadding.current

    return PaddingValues(
        bottom = if (bottom)
            max(
                max(
                    innerPadding.calculateBottomPadding(),
                    // this is needed for some reason when the bottom navigation bar is visible
                    safeDrawingPaddingValues.calculateBottomPadding()
                ),
                verticalOverscanPadding()
            )
        else 0.dp,
        start = if (sides)
            max(
                innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                horizontalOverscanPadding()
            )
        else 0.dp,
        end = if (sides)
            max(
                innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                horizontalOverscanPadding()
            )
        else 0.dp,
    )
}