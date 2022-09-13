package com.arn.scrobble

import android.os.Bundle
import android.os.Parcelable
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.parcelize.Parcelize

// Why is this so confusing?
// https://stackoverflow.com/questions/39916021/does-retrieving-a-parcelable-object-through-bundle-always-create-new-copy

@Parcelize
data class PlayingTrackInfo(
    val packageName: String,

    var title: String = "",
    var album: String = "",
    var artist: String = "",
    var albumArtist: String = "",
    var playStartTime: Long = 0,
    var durationMillis: Long = 0,
    var scrobbleElapsedRealtime: Long = 0,
    var hash: Int = 0,
    var isPlaying: Boolean = false,
    var userPlayCount: Int = 0,
    var userLoved: Boolean = false,
    var ignoredArtist: String? = null,

    var lastScrobbleHash: Int = 0,
    var lastSubmittedScrobbleHash: Int = 0,
    var timePlayed: Long = 0L,

    ) : Parcelable {

    fun toScrobbleData() = ScrobbleData().also {
        it.track = title
        it.artist = artist
        it.album = album
        it.albumArtist = albumArtist
        it.timestamp = (playStartTime / 1000).toInt()

        val durationSecs = (durationMillis / 1000).toInt() // in secs
        if (durationSecs >= 30)
            it.duration = durationSecs
    }

    fun toMultiFieldBundle() = Bundle().apply {
        putString(NLService.B_ARTIST, artist)
        putString(NLService.B_ALBUM, album)
        putString(NLService.B_TRACK, title)
        putString(NLService.B_ALBUM_ARTIST, albumArtist)
        putLong(NLService.B_TIME, playStartTime)
    }

    fun updateMetaFrom(p: PlayingTrackInfo): PlayingTrackInfo {
        title = p.title
        album = p.album
        artist = p.artist
        albumArtist = p.albumArtist
        userLoved = p.userLoved
        userPlayCount = p.userPlayCount
        ignoredArtist = p.ignoredArtist
        return this
    }

    fun updateMetaFrom(sd: ScrobbleData): PlayingTrackInfo {
        title = sd.track
        album = sd.album
        artist = sd.artist
        albumArtist = sd.albumArtist
        return this
    }

    fun markAsScrobbled() {
        if (lastScrobbleHash == hash) {
            lastSubmittedScrobbleHash = hash
        }
    }
}

@Parcelize
data class ScrobbleError(
    val title: String,
    val description: String? = null,
    val canForceScrobble: Boolean = true,
) : Parcelable