package com.arn.scrobble.api.lastfm;

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ScrobbleData(
    var artist: String,
    var track: String,
    var album: String?,
    var timestamp: Int,
    var trackNumber: Int? = null,
    var mbid: String? = null,
    var albumArtist: String?,
    var duration: Int?,
): Parcelable