package com.arn.scrobble.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf

val LocalModalShownTracker = compositionLocalOf { mutableIntStateOf(0) }
