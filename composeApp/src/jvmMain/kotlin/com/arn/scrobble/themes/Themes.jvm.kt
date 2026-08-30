package com.arn.scrobble.themes

import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.utils.DesktopStuff
import kotlinx.coroutines.flow.filterNotNull

@Composable
actual fun isSystemInDarkThemeNative(): State<Boolean> {
    return remember { PanoNativeComponents.onDarkModeChangeFlow.filterNotNull() }
        .collectAsState(false)
}

@Composable
actual fun getDynamicColorScheme(dark: Boolean): ColorScheme {
    throw NotImplementedError("Not implemented on desktop")
}

@Composable
actual fun AddAdditionalProviders(content: @Composable () -> Unit) {
    val scrollbarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val defaultScrollbarStyle = remember { defaultScrollbarStyle() }

    val contextMenuRepresentation = if (LocalThemeAttributes.current.isDark) {
        DarkDefaultContextMenuRepresentation
    } else {
        LightDefaultContextMenuRepresentation
    }

    val linuxSystemPropDensity = remember {
        System.getProperty("sun.java2d.uiScale")
            ?.takeIf { DesktopStuff.IS_LINUX }
            ?.toFloatOrNull()
            ?.let { Density(density = it, fontScale = 1f) }
        // fontScale is hardcoded to 1f with t'odo comments in LayoutConfiguration.desktop.kt
        // sun.java2d.uiScale is correctly set to a fraction from skiko, but java rounds it off
    }

    val density = LocalDensity.current.let {
        if (linuxSystemPropDensity != null && linuxSystemPropDensity.density != it.density) {
            linuxSystemPropDensity
        } else {
            it
        }
    }

    CompositionLocalProvider(
        LocalScrollbarStyle provides defaultScrollbarStyle.copy(
            unhoverColor = scrollbarColor.copy(alpha = defaultScrollbarStyle.unhoverColor.alpha),
            hoverColor = scrollbarColor.copy(alpha = defaultScrollbarStyle.hoverColor.alpha),
        ),
        LocalContextMenuRepresentation provides contextMenuRepresentation,
        LocalDensity provides density
    ) {
        content()
    }
}

actual fun setupWindowBlurListener(activity: Any?, onBlurChanged: (Boolean) -> Unit) {
    onBlurChanged(true)
}