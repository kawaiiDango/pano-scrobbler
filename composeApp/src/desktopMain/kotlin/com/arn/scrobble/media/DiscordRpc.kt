package com.arn.scrobble.media

import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.PanoNativeComponents.desktopMediaListener
import com.arn.scrobble.Tokens

object DiscordRpc {
    fun updateDiscordActivity(trackInfo: PlayingTrackInfo, playbackInfo: PlaybackInfo) {
        PanoNativeComponents.updateDiscordActivity(
            clientId = Tokens.DISCORD_CLIENT_ID,
            state = trackInfo.artist,
            details = trackInfo.title,
            largeText = trackInfo.album,
            startTime = trackInfo.playStartTime / 1000,
            endTime = if (trackInfo.durationMillis > 0) (trackInfo.playStartTime + trackInfo.durationMillis) / 1000 else 0L,
            artUrl = "",
            isPlaying = trackInfo.isPlaying,
            statusIsState = true
        )
    }
}