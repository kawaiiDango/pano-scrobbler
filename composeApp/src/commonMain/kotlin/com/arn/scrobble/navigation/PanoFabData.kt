package com.arn.scrobble.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.StringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add
import pano_scrobbler.composeapp.generated.resources.done


data class PanoFabData(
    val stringRes: StringResource,
    val icon: ImageVector,
    val route: PanoRoute?,
    val dialog: PanoDialog? = null,
)

fun getFabData(dest: NavDestination?): PanoFabData? {
    if (dest == null) return null

    return when {
        dest.hasRoute<PanoRoute.RegexEdits>() && !PlatformStuff.isTv -> PanoFabData(
            Res.string.add,
            Icons.Outlined.Add,
            PanoRoute.RegexEditsAdd(null)
        )

        dest.hasRoute<PanoRoute.SimpleEdits>() && !PlatformStuff.isTv -> PanoFabData(
            Res.string.add,
            Icons.Outlined.Add,
            PanoRoute.SimpleEditsAdd(null)
        )

        dest.hasRoute<PanoRoute.BlockedMetadatas>() && !PlatformStuff.isTv -> PanoFabData(
            Res.string.add,
            Icons.Outlined.Add,
            route = null,
            dialog = PanoDialog.BlockedMetadataAdd(ignoredArtist = null, hash = null)
        )

        dest.hasRoute<PanoRoute.AppList>() ||
                dest.hasRoute<PanoRoute.ThemeChooser>()
            ->
            PanoFabData(
                Res.string.done,
                Icons.Outlined.Check,
                PanoRoute.SpecialGoBack
            )

        else -> null
    }
}