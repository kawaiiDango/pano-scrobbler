package com.arn.scrobble.themes

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class ThemeAttributes(
    val isDark: Boolean,
    val contrastMode: ContrastMode,
    val allOnSecondaryContainerColors: List<Color>,
    val allSecondaryContainerColors: List<Color>,
)

val LocalThemeAttributes = compositionLocalOf<ThemeAttributes> {
    error("No ThemeAttributes provided")
}
