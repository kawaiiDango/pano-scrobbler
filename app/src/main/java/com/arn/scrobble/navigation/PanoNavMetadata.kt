package com.arn.scrobble.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arn.scrobble.R
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.utils.Stuff

data class PanoNavMetadata(
    val titleRes: Int,
    val icon: ImageVector,
    val route: PanoRoute,
)

fun hasNavMetadata(dest: NavDestination): Boolean {
    return dest.hasRoute<PanoRoute.HomePager>() || dest.hasRoute<PanoRoute.ChartsPager>()
}