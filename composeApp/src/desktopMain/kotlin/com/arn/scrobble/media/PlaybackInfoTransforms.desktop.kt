package com.arn.scrobble.media


actual typealias PlatformPlaybackInfo = PlaybackInfo

actual fun transformPlaybackState(
    trackInfo: PlayingTrackInfo,
    playbackInfo: PlatformPlaybackInfo,
    options: TransformMetadataOptions
): Pair<PlaybackInfo, Boolean> {
    val commonPlaybackInfo = playbackInfo

    return commonPlaybackInfo to false
}