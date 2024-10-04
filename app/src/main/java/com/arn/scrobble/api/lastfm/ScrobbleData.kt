package com.arn.scrobble.api.lastfm

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Parcelize
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
) : Parcelable {
    override fun toString() =
        "ScrobbleData(artist='$artist', track='$track', album=$album, timestamp=$timestamp, trackNumber=$trackNumber, mbid=$mbid, albumArtist=$albumArtist, duration=$duration, packageName=redacted)"
}