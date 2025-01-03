package com.arn.scrobble.media

sealed interface PlayingTrackNotificationState {
    val trackInfo: PlayingTrackInfo

    data class Scrobbling(override val trackInfo: PlayingTrackInfo, val nowPlaying: Boolean) :
        PlayingTrackNotificationState

    data class Error(override val trackInfo: PlayingTrackInfo, val scrobbleError: ScrobbleError) :
        PlayingTrackNotificationState
}