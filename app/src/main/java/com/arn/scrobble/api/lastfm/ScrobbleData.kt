package com.arn.scrobble.api.lastfm

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
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
) : Parcelable