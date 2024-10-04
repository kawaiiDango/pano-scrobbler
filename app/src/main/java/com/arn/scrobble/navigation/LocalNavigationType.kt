package com.arn.scrobble.navigation

import androidx.compose.runtime.compositionLocalOf

enum class PanoNavigationType {
    BOTTOM_NAVIGATION,
    NAVIGATION_RAIL,
    PERMANENT_NAVIGATION_DRAWER,
}

val LocalNavigationType = compositionLocalOf { PanoNavigationType.BOTTOM_NAVIGATION }
