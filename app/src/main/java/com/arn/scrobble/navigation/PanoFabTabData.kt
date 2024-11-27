package com.arn.scrobble.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track


data class PanoFabData(
    val stringRes: Int,
    val icon: ImageVector,
    val route: PanoRoute,
)

fun getFabData(dest: NavDestination): PanoFabData? {
    return when {
//        dest.hasRoute<PanoRoute.HomePager>() -> PanoFabData(
//            R.string.add,
//            Icons.Outlined.Add,
//            PanoRoute.MusicEntryInfo(
//                track = Track(
//                    name = "Lights",
//                    artist = Artist(
//                        name = "Ellie Goulding",
//                    ),
//                    album = null,
//                ),
//                user = Scrobblables.current.value?.userAccount?.user!!,
//                pkgName = null,
//            )
//        )

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
        dest.hasRoute<PanoRoute.HomePager>() -> listOf(
            PanoTabs.Scrobbles,
            PanoTabs.Following,
            PanoTabs.Charts,
            PanoTabs.Profile,
        )

        dest.hasRoute<PanoRoute.MusicEntryInfoPager>() -> listOf(
            PanoTabs.TopTracks,
            PanoTabs.TopAlbums,
            PanoTabs.TopArtists,
        )

        dest.hasRoute<PanoRoute.ChartsPager>() -> listOf(
            PanoTabs.TopArtists,
            PanoTabs.TopAlbums,
            PanoTabs.TopTracks,
            PanoTabs.MoreOptions,
        )

        else -> null
    }
}
