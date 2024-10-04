package com.arn.scrobble.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arn.scrobble.R


data class PanoFabData(
    val stringRes: Int,
    val icon: ImageVector,
    val route: PanoRoute,
)

fun getFabData(dest: NavDestination): PanoFabData? {
    return when {
        dest.hasRoute<PanoRoute.Placeholder>() -> PanoFabData(
            R.string.add,
            Icons.Outlined.Add,
            PanoRoute.Prefs
        )

        dest.hasRoute<PanoRoute.RegexEdits>() -> PanoFabData(
            R.string.add,
            Icons.Outlined.Add,
            PanoRoute.RegexEditsAdd(null)
        )

        dest.hasRoute<PanoRoute.SimpleEdits>() -> PanoFabData(
            R.string.add,
            Icons.Outlined.Add,
            PanoRoute.SimpleEditsAdd(null)
        )

        dest.hasRoute<PanoRoute.BlockedMetadatas>() -> PanoFabData(
            R.string.add,
            Icons.Outlined.Add,
            PanoRoute.BlockedMetadataAdd(ignoredArtist = null, hash = null)
        )

        dest.hasRoute<PanoRoute.AppList>() ||
                dest.hasRoute<PanoRoute.ThemeChooser>()
            ->
            PanoFabData(
                R.string.done,
                Icons.Outlined.Check,
                PanoRoute.SpecialGoBack
            )

        else -> null
    }
}

fun getTabData(dest: NavDestination): List<PanoTabs>? {
    return when {
        dest.hasRoute<PanoRoute.Placeholder>() -> listOf(
            PanoTabs.Scrobbles,
            PanoTabs.Following,
            PanoTabs.Charts,
            PanoTabs.Profile,
        )

        else -> null
    }
}
