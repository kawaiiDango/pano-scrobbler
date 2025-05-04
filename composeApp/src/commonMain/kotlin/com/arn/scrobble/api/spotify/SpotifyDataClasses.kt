package com.arn.scrobble.api.spotify

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Serializable
data class SpotifyTokenResponse(
    val access_token: String,
    val expires_in: Int,
)

@Serializable
data class TrackFeatures(
    val acousticness: Float,
    val danceability: Float,
    val duration_ms: Int,
    val energy: Float,
    val id: String,
    val instrumentalness: Float,
    val key: Int,
    val liveness: Float,
    val loudness: Float,
    val mode: Int,
    val speechiness: Float,
    val tempo: Float,
    val time_signature: Int,
    val valence: Float,
) {
    fun getKeyString(): String? {
        var scale = when (key) {
            0 -> "C"
            1 -> "C♯"
            2 -> "D"
            3 -> "D♯"
            4 -> "E"
            5 -> "F"
            6 -> "F♯"
            7 -> "G"
            8 -> "G♯"
            9 -> "A"
            10 -> "A♯"
            11 -> "B"
            else -> null
        }

        if (scale != null)
            scale += (if (mode == 1) "" else "m")
        return scale
    }
}

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
    val followers: Followers?,
    val genres: List<String>?,
    override val href: String,
    override val id: String,
    val images: List<Image>?,
    override val name: String,
    val popularity: Int?,
    override val uri: String
) : SpotifyMusicItem {
    val mediumImageUrl: String?
        get() = images?.getOrNull(images.size - 2)?.url
}

@Serializable
data class Followers(
    val href: String?,
    val total: Int
)

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
    val duration_ms: Int,
    override val href: String,
    override val id: String,
    override val name: String,
    val popularity: Int,
    val preview_url: String?,
    override val uri: String
) : SpotifyMusicItem {
    fun getReleaseDateDate(): Date? {
        val sdf = when (album.release_date_precision) {
            "year" -> SimpleDateFormat("yyyy", Locale.getDefault())
            "month" -> SimpleDateFormat("yyyy-MM", Locale.getDefault())
            "day" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            else -> return null
        }
        return sdf.parse(album.release_date)
    }
}

data class TrackWithFeatures(
    val track: TrackItem,
    val features: TrackFeatures?
)

@Serializable
data class AlbumItem(
    val album_type: String,
    val artists: List<ArtistItem>,
    override val href: String,
    override val id: String,
    val images: List<Image>?,
    override val name: String,
    val release_date: String,
    val release_date_precision: String,
    val total_tracks: Int,
    override val uri: String
) : SpotifyMusicItem {
    val mediumImageUrl: String?
        get() = images?.getOrNull(images.size - 2)?.url
}

enum class SpotifySearchType {
    track, artist, album
}