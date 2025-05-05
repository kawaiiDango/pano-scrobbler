package com.arn.scrobble.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.charts
import pano_scrobbler.composeapp.generated.resources.following
import pano_scrobbler.composeapp.generated.resources.pref_user_label
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.tracks

enum class PanoTabType {
    TAB, MENU, PROFILE
}

sealed class PanoTabs(
    val titleRes: StringResource,
    val icon: ImageVector,
    val type: PanoTabType = PanoTabType.TAB,
) {
    data class Scrobbles(val showChips: Boolean = true) :
        PanoTabs(titleRes = Res.string.scrobbles, icon = Icons.Outlined.History)

    data object Following : PanoTabs(titleRes = Res.string.following, icon = Icons.Outlined.Group)
    data object Charts : PanoTabs(titleRes = Res.string.charts, icon = Icons.Outlined.InsertChart)
    data object Profile :
        PanoTabs(
            titleRes = Res.string.pref_user_label,
            icon = Icons.Outlined.Person,
            type = PanoTabType.PROFILE
        )

    data object TopArtists : PanoTabs(titleRes = Res.string.artists, icon = Icons.Outlined.Mic)
    data object TopAlbums : PanoTabs(titleRes = Res.string.albums, icon = Icons.Outlined.Album)
    data object TopTracks : PanoTabs(titleRes = Res.string.tracks, icon = Icons.Outlined.MusicNote)
}