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


@Serializable
data class ItunesAlbum(
    val wrapperType: ItunesWrapperType,
    // Always present for entity=album
    val collectionId: Long,
    val collectionName: String,
    val artistName: String,
    // "Album", "Compilation", ...
    val collectionType: String? = null,
    // Can be missing, e.g. on "Various Artists" compilations
    val artistId: Long? = null,
    // Only present on compilations / albums whose collection artist differs from the track artist
    val collectionArtistId: Long? = null,
    val collectionArtistName: String? = null,
    val artworkUrl60: String? = null,
    val artworkUrl100: String? = null,
    // ISO-8601 UTC, e.g. "2011-01-01T08:00:00Z"
//    val releaseDate: String? = null,
//    val primaryGenreName: String? = null,
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

@Serializable
data class ItunesAlbumResponse(
    val resultCount: Int,
    val results: List<ItunesAlbum>,
)