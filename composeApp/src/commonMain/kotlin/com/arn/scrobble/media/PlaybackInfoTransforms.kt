package com.arn.scrobble.media

expect class PlatformPlaybackInfo

expect fun transformPlaybackState(
    trackInfo: PlayingTrackInfo,
    playbackInfo: PlatformPlaybackInfo,
): Pair<PlaybackInfo, Boolean>