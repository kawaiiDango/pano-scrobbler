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
import com.arn.scrobble.api.UserCached
import org.jetbrains.compose.resources.StringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.charts
import pano_scrobbler.composeapp.generated.resources.following
import pano_scrobbler.composeapp.generated.resources.pref_user_label
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.tracks

sealed class PanoTab(
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    data class Scrobbles(val showChips: Boolean = true) :
        PanoTab(titleRes = Res.string.scrobbles, icon = Icons.Outlined.History)

    data object Following : PanoTab(titleRes = Res.string.following, icon = Icons.Outlined.Group)
    data object Charts : PanoTab(titleRes = Res.string.charts, icon = Icons.Outlined.InsertChart)
    data class Profile(val user: UserCached) :
        PanoTab(
            titleRes = Res.string.pref_user_label,
            icon = Icons.Outlined.Person,
        )

    data object TopArtists : PanoTab(titleRes = Res.string.artists, icon = Icons.Outlined.Mic)
    data object TopAlbums : PanoTab(titleRes = Res.string.albums, icon = Icons.Outlined.Album)
    data object TopTracks : PanoTab(titleRes = Res.string.tracks, icon = Icons.Outlined.MusicNote)
}