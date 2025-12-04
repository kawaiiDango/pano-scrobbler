package com.arn.scrobble.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun NavFromTrayEffect(
    onOpenDialog: (PanoDialog) -> Unit,
    onNavigate: (PanoRoute) -> Unit
)