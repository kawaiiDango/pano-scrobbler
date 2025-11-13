package com.arn.scrobble.api.lastfm

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ScrobbleData(
    val artist: String,
    val track: String,
    val album: String?,
    val timestamp: Long,
    val trackNumber: Int? = null,
    val albumArtist: String?,
    val duration: Long?,
    val appId: String?
) {
    fun safeDuration() = duration?.takeIf { it in (30_000..3600_000) }

    fun toTrack() = Track(
        name = track,
        artist = Artist(artist),
        date = timestamp,
        album = album?.ifEmpty { null }
            ?.let { Album(album, Artist(albumArtist.orEmpty().ifEmpty { artist })) },
        duration = duration,
    )

    fun trimmed() = copy(
        artist = artist.trim(),
        track = track.trim(),
        album = album?.trim()?.ifEmpty { null },
        albumArtist = albumArtist?.trim()?.ifEmpty { null },
    )
}