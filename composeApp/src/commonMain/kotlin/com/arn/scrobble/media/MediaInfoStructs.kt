package com.arn.scrobble.media


data class SessionInfo(
    val rawAppId: String,
    val appName: String,
)

data class MetadataInfo(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val trackNumber: Int,
    val duration: Long,
    val artUrl: String?,
    val normalizedUrlHost: String?,
)

data class PlaybackInfo(
    val state: CommonPlaybackState,
    val position: Long,
    val canSkip: Boolean,
)