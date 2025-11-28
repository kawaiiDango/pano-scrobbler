package com.arn.scrobble.media

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.discordrpc.DiscordRpc
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

actual fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent) {
    if (globalTrackEventFlow.subscriptionCount.value > 0) {
        globalTrackEventFlow.tryEmit(event)
    }

    if (event is PlayingTrackNotifyEvent.TrackCancelled)
        DiscordRpc.clearDiscordActivity(event.hash)
}


actual fun getNowPlayingFromMainProcess(): Pair<ScrobbleData, Int>? {
    val playingEvent = PanoNotifications.playingTrackTrayInfo.value.values
        .filterIsInstance<PlayingTrackNotifyEvent.TrackPlaying>()
        .firstOrNull { it.nowPlaying }
    return playingEvent?.let {
        it.origScrobbleData to it.hash
    }
}

actual suspend fun shouldFetchNpArtUrl(): Boolean {
    return DiscordRpc.wasSuccessFul.value == true &&
            PlatformStuff.mainPrefs.data.map { it.discordRpc.enabled && it.discordRpc.albumArt && it.discordRpc.albumArtFromNowPlaying }
                .first()
}
