package com.arn.scrobble.themes

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
actual fun getDynamicColorScheme(dark: Boolean): ColorScheme {
    throw NotImplementedError("Not implemented on desktop")
}

@Composable
actual fun ProvideScrollbarStyle(content: @Composable () -> Unit) {
    val scrollbarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val defaultScrollbarStyle = remember { defaultScrollbarStyle() }

    CompositionLocalProvider(
        LocalScrollbarStyle provides defaultScrollbarStyle.copy(
            unhoverColor = scrollbarColor.copy(alpha = defaultScrollbarStyle.unhoverColor.alpha),
            hoverColor = scrollbarColor.copy(alpha = defaultScrollbarStyle.hoverColor.alpha),
        )
    ) {
        content()
    }
}