package com.arn.scrobble.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType

val LocalInnerPadding = compositionLocalOf { PaddingValues.Zero }

@Composable
fun Modifier.navBg() = fillMaxSize()
//    .then(
//        MaterialTheme.colorScheme.surface.let {
//            if (it.alpha == 1f) Modifier.background(it) else Modifier
//        }
//    )

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