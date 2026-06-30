package com.arn.scrobble.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

actual suspend fun determineLocalNetworkPermissionException(url: String?): Throwable? = null

@Composable
actual fun LocalNetworkPermissionsRequest(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onGranted()
    }
}