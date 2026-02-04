package com.arn.scrobble.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

class PanoSnackbarVisuals(
    override val message: String,
    val isError: Boolean = false,
    longDuration: Boolean = false,
) :
    SnackbarVisuals {
    override val duration: SnackbarDuration = if (longDuration)
        SnackbarDuration.Long
    else
        SnackbarDuration.Short
    override val actionLabel = null

    override val withDismissAction = false
}