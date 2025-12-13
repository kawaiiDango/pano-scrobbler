package com.arn.scrobble.navigation

import androidx.compose.runtime.compositionLocalOf

val LocalActivityRestoredFlag = compositionLocalOf<Boolean> {
    error("No ActivityRestoredFlag provided")
}
