package com.arn.scrobble.pref

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun ImportScreenPermissionsRequest(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onGranted()
    }
}