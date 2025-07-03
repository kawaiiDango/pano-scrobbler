package com.arn.scrobble.media


data class SessionInfo(
    val appId: String,
    val appName: String,
)

data class MetadataInfo(
    val appId: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val trackNumber: Int,
    val duration: Long,
)

data class PlaybackInfo(
    val appId: String,
    val state: CommonPlaybackState,
    val position: Long,
    val canSkip: Boolean,
)