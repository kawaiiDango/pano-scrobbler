package com.arn.scrobble.themes

import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext


@RequiresApi(31)
@Composable
actual fun getDynamicColorScheme(dark: Boolean): ColorScheme {
    val context = LocalContext.current

    return if (dark) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
}

@Composable
actual fun ProvideScrollbarStyle(content: @Composable () -> Unit) {
    content()
}