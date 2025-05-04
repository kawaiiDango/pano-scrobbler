package com.arn.scrobble.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import org.jetbrains.compose.resources.StringResource

data class PanoNavMetadata(
    val titleRes: StringResource,
    val icon: ImageVector,
    val route: PanoRoute,
)

fun hasNavMetadata(dest: NavDestination): Boolean {
    return dest.hasRoute<PanoRoute.SelfHomePager>() || dest.hasRoute<PanoRoute.OthersHomePager>()
}