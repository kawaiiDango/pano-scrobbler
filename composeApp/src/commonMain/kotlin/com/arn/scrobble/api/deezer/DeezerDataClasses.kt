package com.arn.scrobble.api.deezer

import kotlinx.serialization.Serializable

@Serializable
data class DeezerSearchResponse(
    val data: List<DeezerTrack>,
    val total: Int
)

@Serializable
data class DeezerTrack(
    val id: Long,
    val title: String,
    val title_short: String,
    val title_version: String,
    val duration: Int,
    val artist: DeezerArtist,
    val album: DeezerAlbum,
    val contributors: List<DeezerArtist>? = null,
    val type: String
)

@Serializable
data class DeezerArtist(
    val id: Long,
    val name: String,
    val type: String
)

@Serializable
data class DeezerAlbum(
    val id: Long,
    val title: String,
    val cover: String,
    val type: String
)