package com.arn.scrobble.media

import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntryAttr
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.utils.Stuff
import kotlinx.serialization.Serializable

@Serializable
data class PlayingTrackInfo(
    val appId: String,
    val sessionId: String,

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
    var scrobbleAtMonotonicTime: Long = 0,
    var hash: Int = 0,
    var isPlaying: Boolean = false,
    var userPlayCount: Int = 0,
    var userLoved: Boolean = false,
    var ignoreOrigArtist: Boolean = false,
    var canDoFallbackScrobble: Boolean = false,

    var lastScrobbleHash: Int = 0,
    var lastSubmittedScrobbleHash: Int = 0,
    var timePlayed: Long = 0L,

    val hasBlockedTag: Boolean = (Stuff.BLOCKED_MEDIA_SESSION_TAGS["*"]?.contains(sessionId) == true ||
            Stuff.BLOCKED_MEDIA_SESSION_TAGS[appId]
                ?.contains(sessionId) == true),
) {

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

    fun toScrobbleData() = ScrobbleData(
        track = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        timestamp = playStartTime,
        duration = durationMillis.takeIf { it >= 30000L },
        packageName = appId,
    )

    fun toTrack() = Track(
        name = title,
        artist = Artist(artist),
        album = album.ifEmpty { null }
            ?.let { Album(album, albumArtist.ifEmpty { null }?.let { Artist(it) }) },
        userplaycount = userPlayCount,
        userloved = userLoved,
        duration = durationMillis,
        _attr = MusicEntryAttr(
            nowplaying = isPlaying,
        ),
    )

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
        album = sd.album ?: ""
        artist = sd.artist
        albumArtist = sd.albumArtist ?: ""
        return this
    }

    fun markAsScrobbled() {
        if (lastScrobbleHash == hash) {
            lastSubmittedScrobbleHash = hash
        }
    }
}