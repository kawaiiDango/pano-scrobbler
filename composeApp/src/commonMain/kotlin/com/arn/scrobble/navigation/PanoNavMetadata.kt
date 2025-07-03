package com.arn.scrobble.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

fun hasTabMetadata(dest: NavDestination?): Boolean {
    if (dest == null) return false
    return dest.hasRoute<PanoRoute.SelfHomePager>() || dest.hasRoute<PanoRoute.OthersHomePager>() ||
            dest.hasRoute<PanoRoute.ChartsPager>() || dest.hasRoute<PanoRoute.MusicEntryInfoPager>()
}