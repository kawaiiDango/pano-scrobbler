package com.arn.scrobble.media

import kotlinx.serialization.Serializable

@Serializable
data class SessionInfo(
    val session_id: String,
    val app_id: String,
    val app_name: String,
)

@Serializable
data class MetadataInfo(
    val app_id: String,
    val session_id: String,
    val title: String,
    val artist: String,
    val album: String,
    val album_artist: String,
    val track_number: Int,
    val duration: Long,
)

@Serializable
data class PlaybackInfo(
    val app_id: String,
    val session_id: String,
    val state: CommonPlaybackState,
    val position: Long,
    val can_skip: Boolean,
)