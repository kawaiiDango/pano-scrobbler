package com.arn.scrobble.themes

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class ThemeAttributes(
    val isDark: Boolean,
    val isTranslucent: Boolean,
    val blurMainWindow: Boolean,
    val blurSubWindow: Boolean,
    val contrastMode: ContrastMode,
    val style: PaletteStyle,
    val avatarColors: List<Color>,
    val avatarContainerColors: List<Color>,
)

val LocalThemeAttributes = compositionLocalOf<ThemeAttributes> {
    error("No ThemeAttributes provided")
}
