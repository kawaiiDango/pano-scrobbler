package com.arn.scrobble.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType

val LocalInnerPadding = compositionLocalOf { PaddingValues.Zero }
val LocalNavDestBackground = compositionLocalOf { Color.Transparent }

@Composable
fun Modifier.navBg() = fillMaxSize()
    .background(LocalNavDestBackground.current)

@Composable
fun Modifier.navScrollableColumn(
    mayHaveBottomFab: Boolean = false,
) = navBg()
    .imePadding()
    .verticalScroll(rememberScrollState())
    .padding(panoContentPadding(mayHaveBottomFab = mayHaveBottomFab))

@Composable
fun Modifier.navColumn() = navBg().imePadding()

@Composable
fun Modifier.navModal(
    scrollState: ScrollState = rememberScrollState(),
    sides: Boolean = true,
    expanded: Boolean = false
) = if (expanded) navBg()
    // ime inset gets "stuck" in expanded mode otherwise
    .padding(
        bottom = with(LocalDensity.current) {
            WindowInsets.ime.getBottom(this)
                .coerceAtLeast(0)
                .toDp()
        }
    )
    .verticalScroll(scrollState)
    .padding(panoContentPadding(sides = sides))
else fillMaxWidth()
    // bottom sheets have their own ime padding
    .verticalScroll(scrollState)
    .padding(
        start = if (sides) 24.dp else 0.dp,
        end = if (sides) 24.dp else 0.dp,
        bottom = max(verticalOverscanPadding(), 16.dp)
    )

@Composable
fun panoContentPadding(
    sides: Boolean = true,
    bottom: Boolean = true,
    mayHaveBottomFab: Boolean = false,
): PaddingValues {
//    val safeDrawingPaddingValues = WindowInsets.safeDrawing.asPaddingValues()
    val innerPadding = LocalInnerPadding.current

    return PaddingValues(
        bottom = if (bottom)
            max(
                max(
                    innerPadding.calculateBottomPadding(),
                    // this is needed for some reason when the bottom navigation bar is visible
//                    safeDrawingPaddingValues.calculateBottomPadding() +
                    if (mayHaveBottomFab && LocalNavigationType.current == PanoNavigationType.BOTTOM_NAVIGATION) 72.dp else 0.dp
                ),
                verticalOverscanPadding() + 16.dp
            )
        else 0.dp,
        start = if (sides)
            max(
                innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                // TVs have a nav rail at the start
                16.dp
            )
        else 0.dp,
        end = if (sides)
            max(
                innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                horizontalOverscanPadding() + 16.dp
            )
        else 0.dp,
    )
}