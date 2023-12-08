package com.arn.scrobble

import android.os.Parcelable
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.parcelize.Parcelize

// Why is this so confusing?
// https://stackoverflow.com/questions/39916021/does-retrieving-a-parcelable-object-through-bundle-always-create-new-copy

@Parcelize
data class PlayingTrackInfo(
    val packageName: String,

    var title: String = "",
    var origTitle: String = "",

    var album: String = "",
    var origAlbum: String = "",

    var artist: String = "",
    var origArtist: String = "",

    var albumArtist: String = "",
    var origAlbumArtist: String = "",

    var playStartTime: Long = 0,
    var durationMillis: Long = 0,
    var scrobbleElapsedRealtime: Long = 0,
    var hash: Int = 0,
    var isPlaying: Boolean = false,
    var userPlayCount: Int = 0,
    var userLoved: Boolean = false,
    var ignoreOrigArtist: Boolean = false,
    var canDoFallbackScrobble: Boolean = false,

    var lastScrobbleHash: Int = 0,
    var lastSubmittedScrobbleHash: Int = 0,
    var timePlayed: Long = 0L,

    ) : Parcelable {

    fun putOriginals(artist: String, title: String) = putOriginals(artist, title, "", "")

    fun putOriginals(artist: String, title: String, album: String, albumArtist: String) {
        origArtist = artist
        this.artist = artist
        origTitle = title
        this.title = title
        origAlbum = album
        this.album = album
        origAlbumArtist = albumArtist
        this.albumArtist = albumArtist
    }

    fun toScrobbleData() = ScrobbleData().also {
        it.track = title
        it.artist = artist
        it.album = album
        it.albumArtist = albumArtist
        it.timestamp = (playStartTime / 1000).toInt()
        it.pkgName = packageName

        val durationSecs = (durationMillis / 1000).toInt() // in secs
        if (durationSecs >= 30) it.duration = durationSecs
    }

    fun updateMetaFrom(p: PlayingTrackInfo): PlayingTrackInfo {
        title = p.title
        album = p.album
        artist = p.artist
        albumArtist = p.albumArtist
        userLoved = p.userLoved
        userPlayCount = p.userPlayCount
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
    val description: String?,
    val packageName: String,
) : Parcelable