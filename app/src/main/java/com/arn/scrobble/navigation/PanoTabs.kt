package com.arn.scrobble.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.arn.scrobble.R

enum class PanoTabType {
    TAB, BUTTON
}

sealed class PanoTabs(
    val titleRes: Int,
    val icon: ImageVector,
    val type: PanoTabType = PanoTabType.TAB
) {
    data object Scrobbles : PanoTabs(titleRes = R.string.scrobbles, icon = Icons.Outlined.History)
    data object Following : PanoTabs(titleRes = R.string.following, icon = Icons.Outlined.Group)
    data object Charts : PanoTabs(titleRes = R.string.charts, icon = Icons.Outlined.InsertChart)
    data object Profile :
        PanoTabs(titleRes = R.string.more, icon = Icons.Outlined.Person, type = PanoTabType.BUTTON)

    data object TopArtists : PanoTabs(titleRes = R.string.artists, icon = Icons.Outlined.Mic)
    data object TopAlbums : PanoTabs(titleRes = R.string.albums, icon = Icons.Outlined.Album)
    data object TopTracks : PanoTabs(titleRes = R.string.tracks, icon = Icons.Outlined.MusicNote)
    data object MoreOptions : PanoTabs(
        titleRes = R.string.more,
        icon = Icons.Outlined.MoreHoriz,
        type = PanoTabType.BUTTON
    )

    data object SimilarArtists :
        PanoTabs(titleRes = R.string.similar_artists, icon = Icons.Outlined.Mic)
}