package com.arn.scrobble.themes

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class ThemeAttributes(
    val isDark: Boolean,
    val contrastMode: ContrastMode,
    val allOnSecondaryColors: List<Color>,
    val allSecondaryContainerColors: List<Color>,
)

val LocalThemeAttributes = compositionLocalOf {
    ThemeAttributes(
        isDark = true,
        contrastMode = ContrastMode.LOW,
        allOnSecondaryColors = listOf(Color.Magenta),
        allSecondaryContainerColors = listOf(Color.Blue),
    )
}
