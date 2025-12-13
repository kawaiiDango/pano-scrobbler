package com.arn.scrobble.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.compositionLocalOf

data class ModalScrollProps(
    val scrollState: ScrollState,
    val scrollEnabled: Boolean,
)

val LocalModalScrollProps = compositionLocalOf<ModalScrollProps> {
    error("No ModalScrollProps provided")
}
