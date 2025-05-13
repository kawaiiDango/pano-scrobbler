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

fun hasNavMetadata(dest: NavDestination?): Boolean {
    if (dest == null) return false
    return dest.hasRoute<PanoRoute.SelfHomePager>() || dest.hasRoute<PanoRoute.OthersHomePager>()
}

fun hasTabMetadata(dest: NavDestination?): Boolean {
    if (dest == null) return false
    return dest.hasRoute<PanoRoute.SelfHomePager>() || dest.hasRoute<PanoRoute.OthersHomePager>() ||
            dest.hasRoute<PanoRoute.ChartsPager>() || dest.hasRoute<PanoRoute.MusicEntryInfoPager>()
}