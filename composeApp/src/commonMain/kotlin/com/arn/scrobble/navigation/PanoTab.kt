package com.arn.scrobble.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.getPeriodTypeIcon
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.BarChart4Bars
import com.arn.scrobble.icons.Casino
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.Group
import com.arn.scrobble.icons.History
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.Person
import com.arn.scrobble.icons.Refresh
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.StringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.charts
import pano_scrobbler.composeapp.generated.resources.following
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.pref_user_label
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.recents
import pano_scrobbler.composeapp.generated.resources.reload
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.time_jump
import pano_scrobbler.composeapp.generated.resources.tracks

sealed class PanoTab(
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    data class Subtab(
        val id: Int,
        val icon: ImageVector,
        val titleRes: StringResource,
    )

    interface HasSubtabs {
        val subTabs: List<Subtab>
    }

    data object Scrobbles : PanoTab(titleRes = Res.string.scrobbles, icon = Icons.History),
        HasSubtabs {
        enum class ScrobblesType {
            REFRESH,
            RECENTS,
            LOVED,
            TIME_JUMP,
            RANDOM,
        }

        override val subTabs = listOfNotNull(
            if (PlatformStuff.isDesktop || PlatformStuff.isTv)
                Subtab(ScrobblesType.REFRESH.ordinal, Icons.Refresh, Res.string.reload)
            else
                null,
            Subtab(ScrobblesType.RECENTS.ordinal, Icons.History, Res.string.recents),
            Subtab(ScrobblesType.LOVED.ordinal, Icons.Favorite, Res.string.loved),
            Subtab(
                ScrobblesType.TIME_JUMP.ordinal,
                getPeriodTypeIcon(TimePeriodType.CUSTOM),
                Res.string.time_jump
            ),
            Subtab(ScrobblesType.RANDOM.ordinal, Icons.Casino, Res.string.random_text)
        )
    }

    data object ScrobblesNoSubtabs : PanoTab(titleRes = Res.string.scrobbles, icon = Icons.History)

    data object Following : PanoTab(titleRes = Res.string.following, icon = Icons.Group)
    data object Charts : PanoTab(titleRes = Res.string.charts, icon = Icons.BarChart4Bars)
    data object Profile : PanoTab(titleRes = Res.string.pref_user_label, icon = Icons.Person)
    data object TopArtists : PanoTab(titleRes = Res.string.artists, icon = Icons.Mic)
    data object TopAlbums : PanoTab(titleRes = Res.string.albums, icon = Icons.Album)
    data object TopTracks : PanoTab(titleRes = Res.string.tracks, icon = Icons.MusicNote)
}