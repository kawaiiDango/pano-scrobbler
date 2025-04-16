package com.arn.scrobble.media

expect class PlatformPlaybackInfo

expect fun transformPlaybackState(
    trackInfo: PlayingTrackInfo,
    playbackInfo: PlatformPlaybackInfo,
    options: TransformMetadataOptions
): Pair<PlaybackInfo, Boolean>

data class TransformMetadataOptions(
    val scrobbleSpotifyRemote: Boolean = false,
)