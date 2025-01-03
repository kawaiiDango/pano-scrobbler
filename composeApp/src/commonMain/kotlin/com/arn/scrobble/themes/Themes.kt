package com.arn.scrobble.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.themes.colors.ThemeVariants
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val licenseState by PlatformStuff.billingRepository.licenseState.collectAsStateWithLifecycle()
    val themeName by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeName }
    val dynamic by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDynamic }
    val dayNightMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDayNight }
    val contrastMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeContrast }
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val isDark by remember(dayNightMode) {
        mutableStateOf(
            dayNightMode == DayNightMode.DARK || (dayNightMode == DayNightMode.SYSTEM && isSystemInDarkTheme)
        )
    }

    val themeAttributes = remember(isDark, contrastMode) {
        val colorSchemes = ThemeUtils.themesMap.values.map {
            getColorScheme(
                theme = it,
                isDark = isDark,
                contrastMode = contrastMode,
            )
        }

        ThemeAttributes(
            isDark = isDark,
            contrastMode = contrastMode,
            allOnSecondaryColors = colorSchemes.map { it.onSecondary },
            allSecondaryContainerColors = colorSchemes.map { it.secondaryContainer },
        )
    }

    val colorScheme: ColorScheme = when {
        licenseState != LicenseState.VALID -> {
            ThemeUtils.defaultTheme.dark
        }

        dynamic == true && PlatformStuff.supportsDynamicColors -> {
            getDynamicColorScheme(isDark)
        }

        else -> {
            val theme = ThemeUtils.themeNameToObject(themeName)

            getColorScheme(
                theme = theme,
                isDark = isDark,
                contrastMode = contrastMode,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        CompositionLocalProvider(LocalThemeAttributes provides themeAttributes) {
            ProvideScrollbarStyle {
                content()
            }
        }
    }
}

private fun getColorScheme(
    theme: ThemeVariants,
    isDark: Boolean,
    contrastMode: ContrastMode,
): ColorScheme {
    return when {
        isDark && contrastMode == ContrastMode.LOW -> theme.dark
        isDark && contrastMode == ContrastMode.MEDIUM -> theme.darkMediumContrast
        isDark && contrastMode == ContrastMode.HIGH -> theme.darkHighContrast

        !isDark && contrastMode == ContrastMode.LOW -> theme.light
        !isDark && contrastMode == ContrastMode.MEDIUM -> theme.lightMediumContrast
        !isDark && contrastMode == ContrastMode.HIGH -> theme.lightHighContrast

        else -> theme.dark
    }
}

@Composable
fun AppPreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Composable
expect fun getDynamicColorScheme(dark: Boolean): ColorScheme

@Composable
expect fun ProvideScrollbarStyle(content: @Composable () -> Unit)