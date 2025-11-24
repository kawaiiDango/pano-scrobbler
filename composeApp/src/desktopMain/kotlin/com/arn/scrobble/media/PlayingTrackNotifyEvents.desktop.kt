package com.arn.scrobble.media

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.PanoNotifications

actual fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent) {
    if (globalTrackEventFlow.subscriptionCount.value > 0) {
        globalTrackEventFlow.tryEmit(event)
    }
}


actual fun getNowPlayingFromMainProcess(): Pair<ScrobbleData, Int>? {
    val playingEvent = PanoNotifications.playingTrackTrayInfo.value.values
        .filterIsInstance<PlayingTrackNotifyEvent.TrackPlaying>()
        .firstOrNull { it.nowPlaying }
    return playingEvent?.let {
        it.origScrobbleData to it.hash
    }
}