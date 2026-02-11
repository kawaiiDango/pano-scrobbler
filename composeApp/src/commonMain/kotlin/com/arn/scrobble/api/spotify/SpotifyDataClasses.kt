package com.arn.scrobble.api.spotify

import kotlinx.serialization.Serializable


@Serializable
data class SpotifyTokenResponse(
    val access_token: String,
    val expires_in: Int,
)

@Serializable
data class SpotifySearchResponse(
    val artists: SearchItems<ArtistItem>?,
    val albums: SearchItems<AlbumItem>?,
    val tracks: SearchItems<TrackItem>?,
)

sealed interface SpotifyMusicItem {
    val id: String
    val name: String
    val uri: String
    val href: String
}

@Serializable
data class SearchItems<T : SpotifyMusicItem>(
    val href: String,
    val items: List<T>,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int
)

@Serializable
data class ArtistItem(
    override val href: String,
    override val id: String,
    val images: List<Image>?,
    override val name: String,
    override val uri: String
) : SpotifyMusicItem {
    val mediumImageUrl: String?
        get() = images?.getOrNull(images.size - 2)?.url
    val largeImageUrl: String?
        get() = images?.firstOrNull()?.url

}

@Serializable
data class Image(
    val height: Int,
    val url: String,
    val width: Int
)

@Serializable
data class TrackItem(
    val album: AlbumItem,
    val artists: List<ArtistItem>,
    override val href: String,
    override val id: String,
    override val name: String,
    override val uri: String
) : SpotifyMusicItem

@Serializable
data class AlbumItem(
    val album_type: String,
    val artists: List<ArtistItem>,
    override val href: String,
    override val id: String,
    val images: List<Image>?,
    override val name: String,
    override val uri: String
) : SpotifyMusicItem {
    val mediumImageUrl: String?
        get() = images?.getOrNull(images.size - 2)?.url
    val largeImageUrl: String?
        get() = images?.firstOrNull()?.url

}

enum class SpotifySearchType {
    track, artist, album
}