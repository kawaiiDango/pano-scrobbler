package com.arn.scrobble.utils

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.updates.UpdateAction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.digest_monthly
import pano_scrobbler.composeapp.generated.resources.digest_weekly
import pano_scrobbler.composeapp.generated.resources.graph_yearly
import pano_scrobbler.composeapp.generated.resources.new_player
import pano_scrobbler.composeapp.generated.resources.new_player_prompt_desktop
import pano_scrobbler.composeapp.generated.resources.top_albums
import pano_scrobbler.composeapp.generated.resources.top_artists
import pano_scrobbler.composeapp.generated.resources.top_tracks
import pano_scrobbler.composeapp.generated.resources.update_downloaded

actual object PanoNotifications {
    private val _playingTrackTrayInfo =
        MutableStateFlow<Map<String, PlayingTrackNotifyEvent.PlayingTrackState>>(emptyMap())
    val playingTrackTrayInfo = _playingTrackTrayInfo.asStateFlow()

    private var notify: ((String, String) -> Unit) = { title, text ->
    }

    fun setNotifyFn(
        fn: (String, String) -> Unit
    ) {
        notify = fn
    }

    actual fun notifyScrobble(event: PlayingTrackNotifyEvent.TrackScrobbling) {
        if (event.scrobbleData.appId != null)
            _playingTrackTrayInfo.value += event.scrobbleData.appId to event
    }

    actual fun notifyError(event: PlayingTrackNotifyEvent.Error) {
        if (event.scrobbleData.appId != null)
            _playingTrackTrayInfo.value += event.scrobbleData.appId to event
    }

    actual fun notifyAppDetected(appId: String, appLabel: String) {
        GlobalScope.launch {
            notify(
                getString(Res.string.new_player, appLabel.ifEmpty { appId }),
                getString(Res.string.new_player_prompt_desktop)
            )
        }
    }

    actual fun notifyUnscrobbled(scrobbleData: ScrobbleData, hash: Int) {

    }

    actual suspend fun notifyDigest(timePeriod: TimePeriod, resultsList: List<Pair<Int, String>>) {
        if (resultsList.isEmpty()) {
            return
        }

        val notificationTextList = mutableListOf<String>()
        resultsList.forEach { (type, text) ->
            val title = when (type) {
                Stuff.TYPE_ARTISTS -> getString(Res.string.top_artists)
                Stuff.TYPE_ALBUMS -> getString(Res.string.top_albums)
                Stuff.TYPE_TRACKS -> getString(Res.string.top_tracks)
                else -> throw IllegalArgumentException("Invalid musicEntry type")
            }
            notificationTextList += "[$title]\n$text"
        }

        val notificationTitle = getString(
            when (timePeriod.lastfmPeriod) {
                LastfmPeriod.WEEK -> Res.string.digest_weekly
                LastfmPeriod.MONTH -> Res.string.digest_monthly
                LastfmPeriod.YEAR -> Res.string.graph_yearly
                else -> throw IllegalArgumentException("Invalid period")
            }
        )

        val notificationText = notificationTextList.joinToString("\n")

        notify(
            notificationTitle,
            notificationText
        )

        Logger.i { "notifyDigest: $timePeriod, $resultsList" }
    }

    actual suspend fun notifyUpdater(
        updateAction: UpdateAction
    ) {
        notify(
            getString(Res.string.update_downloaded),
            updateAction.version
        )
    }


    actual fun removeNotificationByTag(tag: String) {
        _playingTrackTrayInfo.value -= tag
    }
}