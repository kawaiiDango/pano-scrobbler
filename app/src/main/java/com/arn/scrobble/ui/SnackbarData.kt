package com.arn.scrobble.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

class PanoSnackbarVisuals(
    override val message: String,
    val isError: Boolean,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) :
    SnackbarVisuals {
    override val actionLabel = null

    override val withDismissAction = false
}