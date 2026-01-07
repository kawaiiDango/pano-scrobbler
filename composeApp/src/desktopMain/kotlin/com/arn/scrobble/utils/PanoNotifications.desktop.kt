package com.arn.scrobble.utils

import co.touchlab.kermit.Logger
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.updates.UpdateAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.new_player
import pano_scrobbler.composeapp.generated.resources.new_player_prompt_desktop
import pano_scrobbler.composeapp.generated.resources.update_downloaded

actual object PanoNotifications {
    private val _playingTrackTrayInfo =
        MutableStateFlow<Map<String, PlayingTrackNotifyEvent.PlayingTrackState>>(emptyMap())
    val playingTrackTrayInfo = _playingTrackTrayInfo.asStateFlow()
    actual suspend fun notifyScrobble(event: PlayingTrackNotifyEvent.TrackPlaying) {
        _playingTrackTrayInfo.value += event.notiKey to event
    }

    actual suspend fun notifyError(event: PlayingTrackNotifyEvent.Error) {
        _playingTrackTrayInfo.value += event.notiKey to event
    }

    actual suspend fun notifyAppDetected(appId: String, appLabel: String) {
        PanoNativeComponents.notify(
            getString(Res.string.new_player, appLabel.ifEmpty { appId }),
            getString(Res.string.new_player_prompt_desktop)
        )
    }

    actual suspend fun notifyUnscrobbled(notiKey: String, scrobbleData: ScrobbleData, hash: Int) {
        removeNotificationByKey(notiKey)
    }

    actual suspend fun notifyDigest(lastfmPeriod: LastfmPeriod, title: String, text: String) {
        if (text.isEmpty()) {
            return
        }
        PanoNativeComponents.notify(
            title,
            text
        )

        Logger.i { "notifyDigest: $lastfmPeriod, $text" }
    }

    actual suspend fun notifyUpdater(
        updateAction: UpdateAction
    ) {
        PanoNativeComponents.notify(
            getString(Res.string.update_downloaded),
            updateAction.version
        )
    }


    actual fun removeNotificationByKey(key: String) {
        _playingTrackTrayInfo.value -= key
    }
}