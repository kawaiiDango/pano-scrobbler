package com.arn.scrobble.api.itunes

import kotlinx.serialization.Serializable

@Serializable
data class ItunesTrack(
    val wrapperType: ItunesWrapperType,
    val kind: String,
    val artistId: Long,
    val collectionId: Long,
    val trackId: Long,
    val collectionArtistId: Long? = null,
    val collectionArtistName: String? = null,
    val artistName: String,
    val collectionName: String,
    val trackName: String,
    val collectionCensoredName: String,
    val trackCensoredName: String,
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