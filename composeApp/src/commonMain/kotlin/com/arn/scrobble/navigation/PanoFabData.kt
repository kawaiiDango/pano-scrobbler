package com.arn.scrobble.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource


data class PanoFabData(
    val stringRes: StringResource,
    val icon: ImageVector,
    val showOnTv: Boolean,
    // if route is null, fab goes back
    val route: PanoRoute?,
)