package com.arn.scrobble.api.lastfm

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ScrobbleData(
    var artist: String,
    var track: String,
    var album: String?,
    var timestamp: Long,
    var trackNumber: Int? = null,
    var mbid: String? = null,
    var albumArtist: String?,
    var duration: Long?,
    var packageName: String?
) {
    override fun toString() =
        "ScrobbleData(artist='$artist', track='$track', album=$album, timestamp=$timestamp, trackNumber=$trackNumber, mbid=$mbid, albumArtist=$albumArtist, duration=$duration, packageName=redacted)"
}