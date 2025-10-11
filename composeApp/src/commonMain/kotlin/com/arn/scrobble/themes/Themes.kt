package com.arn.scrobble.themes

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.themes.colors.ThemeVariants
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val licenseState by PlatformStuff.billingRepository.licenseState.collectAsStateWithLifecycle()
    val themeName by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeName }
    val dynamic by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDynamic }
    val dayNightMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDayNight }
    val contrastMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeContrast }
    val isSystemInDarkTheme by isSystemInDarkThemeNative()

    LaunchedEffect(licenseState) {
        if (licenseState != LicenseState.VALID) {
            PlatformStuff.mainPrefs.updateData { it.copy(themeDayNight = DayNightMode.DARK) }
        }
    }

    val isDark by remember(dayNightMode, licenseState, isSystemInDarkTheme) {
        mutableStateOf(
            if (licenseState != LicenseState.VALID)
                true
            else
                dayNightMode == DayNightMode.DARK || (dayNightMode == DayNightMode.SYSTEM && isSystemInDarkTheme)
        )
    }

    val themeAttributes = remember(isDark, contrastMode, themeName) {
        val otherColorSchemes = ThemeUtils.themesMap.values
            .filter { it.name != themeName }
            .map {
                getColorScheme(
                    theme = it,
                    isDark = isDark,
                    contrastMode = contrastMode,
                )
            }

        ThemeAttributes(
            isDark = isDark,
            contrastMode = contrastMode,
            allOnSecondaryContainerColors = otherColorSchemes.map { it.onSecondaryContainer },
            allSecondaryContainerColors = otherColorSchemes.map { it.secondaryContainer },
        )
    }

    val colorScheme: ColorScheme = when {
        licenseState != LicenseState.VALID -> {
            ThemeUtils.defaultTheme.dark
        }

        dynamic && PlatformStuff.supportsDynamicColors -> {
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

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
    ) {
        CompositionLocalProvider(LocalThemeAttributes provides themeAttributes) {
            AddAdditionalProviders {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppPreviewTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme {
        content()
    }
}

@Composable
expect fun isSystemInDarkThemeNative(): State<Boolean>

@Composable
expect fun getDynamicColorScheme(dark: Boolean): ColorScheme

@Composable
expect fun AddAdditionalProviders(content: @Composable () -> Unit)