package com.arn.scrobble.api.itunes

import kotlinx.serialization.Serializable

@Serializable
data class ItunesTrack(
    val wrapperType: ItunesWrapperType,
    val kind: String,
    val trackId: Long,
    val trackName: String,
    val artistId: Long,
    val artistName: String,
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val collectionArtistId: Long? = null,
    val collectionArtistName: String? = null,
    val artworkUrl60: String? = null,
    val artworkUrl100: String? = null,
    val trackTimeMillis: Long? = null,
)

enum class ItunesWrapperType {
    track, collection, artist
}

@Serializable
data class ItunesArtist(
    val wrapperType: ItunesWrapperType,
    val artistId: Long,
    val artistName: String,
    val artistLinkUrl: String,
    val artistType: String? = null,
)

@Serializable
data class ItunesArtistResponse(
    val resultCount: Int,
    val results: List<ItunesArtist>,
)

@Serializable
data class ItunesTrackResponse(
    val resultCount: Int,
    val results: List<ItunesTrack>,
)