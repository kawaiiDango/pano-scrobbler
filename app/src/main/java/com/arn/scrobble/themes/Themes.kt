package com.arn.scrobble.themes

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.themes.colors.PinkOrange
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.map

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val licenseState by Stuff.billingRepository.licenseState.collectAsStateWithLifecycle()
    val themeName: String? by PlatformStuff.mainPrefs.data.map { it.themeName }
        .collectAsStateWithLifecycle(null)
    val dynamic: Boolean? by PlatformStuff.mainPrefs.data.map { it.themeDynamic }
        .collectAsStateWithLifecycle(null)
    val dayNightMode: DayNightMode? by PlatformStuff.mainPrefs.data.map { it.themeDayNight }
        .collectAsStateWithLifecycle(null)
    val contrastMode: ContrastMode? by PlatformStuff.mainPrefs.data.map { it.themeContrast }
        .collectAsStateWithLifecycle(null)
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val isDark by remember(dayNightMode) {
        mutableStateOf(
            dayNightMode == DayNightMode.DARK || (dayNightMode == DayNightMode.SYSTEM && isSystemInDarkTheme)
        )
    }

    // do nothing if theme prefs are not loaded

    if (licenseState == null || dynamic == null || dayNightMode == null || contrastMode == null || themeName == null)
        return

    val colorScheme: ColorScheme = when {
        licenseState != LicenseState.VALID -> {
            PinkOrange.dark
        }

        dynamic == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (isDark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        else -> {
            val theme = ThemeUtils.themeNameToObject(themeName ?: PinkOrange.name)

            when {
                isDark && contrastMode == ContrastMode.LOW -> theme.dark
                isDark && contrastMode == ContrastMode.MEDIUM -> theme.darkMediumContrast
                isDark && contrastMode == ContrastMode.HIGH -> theme.darkHighContrast

                !isDark && contrastMode == ContrastMode.LOW -> theme.light
                !isDark && contrastMode == ContrastMode.MEDIUM -> theme.lightMediumContrast
                !isDark && contrastMode == ContrastMode.HIGH -> theme.lightHighContrast

                else -> PinkOrange.dark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

@Composable
fun AppPreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}