package com.arn.scrobble.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arn.scrobble.api.AccountType
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

fun getFabData(dest: NavDestination): PanoFabData? {
    return when {
        dest.hasRoute<PanoRoute.RegexEdits>() -> PanoFabData(
            Res.string.add,
            Icons.Outlined.Add,
            PanoRoute.RegexEditsAdd(null)
        )

        dest.hasRoute<PanoRoute.SimpleEdits>() -> PanoFabData(
            Res.string.add,
            Icons.Outlined.Add,
            PanoRoute.SimpleEditsAdd(null)
        )

        dest.hasRoute<PanoRoute.BlockedMetadatas>() -> PanoFabData(
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

fun getTabData(dest: NavDestination, accountType: AccountType): List<PanoTabs>? {
    return when {
        dest.hasRoute<PanoRoute.SelfHomePager>() ||
                dest.hasRoute<PanoRoute.OthersHomePager>()
            -> when (accountType) {
            AccountType.LASTFM,
            AccountType.LISTENBRAINZ,
            AccountType.CUSTOM_LISTENBRAINZ,
                -> listOf(
                PanoTabs.Scrobbles(),
                PanoTabs.Following,
                PanoTabs.Charts,
                PanoTabs.Profile,
            )

            AccountType.LIBREFM,
            AccountType.GNUFM,
                -> listOf(
                PanoTabs.Scrobbles(),
                PanoTabs.Charts,
                PanoTabs.Profile,
            )

            AccountType.MALOJA,
            AccountType.PLEROMA,
            AccountType.FILE,
                -> listOf(
                PanoTabs.Scrobbles(showChips = false),
                PanoTabs.Profile,
            )
        }

        dest.hasRoute<PanoRoute.MusicEntryInfoPager>() || dest.hasRoute<PanoRoute.ChartsPager>() ->
            listOf(
                PanoTabs.TopArtists,
                PanoTabs.TopAlbums,
                PanoTabs.TopTracks,
            )

        else -> null
    }
}
