package com.arn.scrobble.media

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff
import java.util.Objects

class PlayingTrackInfo(
    val appId: String,
    val sessionId: String,
) {
    var title: String = ""
        private set
    var origTitle: String = ""
        private set

    var album: String = ""
        private set

    var origAlbum: String = ""
        private set

    var artist: String = ""
        private set
    var origArtist: String = ""
        private set

    var albumArtist: String = ""
        private set
    var origAlbumArtist: String = ""
        private set

    var trackId: String? = null
        private set

    var playStartTime: Long = 0
        private set

    var durationMillis: Long = 0
        private set

    var hash: Int = 0
        private set

    var isPlaying: Boolean = false
        private set

    var userPlayCount: Int = 0
        private set

    var userLoved: Boolean = false
        private set

    var lastScrobbleHash: Int = 0
        private set

    var lastSubmittedScrobbleHash: Int = 0
        private set

    var timePlayed: Long = 0L
        private set

    var preprocessed: Boolean = false
        private set

    var additionalMetadataFetched: Boolean = false
        private set

    val extras = mutableMapOf<String, String>()

    val hasBlockedTag: Boolean =
        (Stuff.BLOCKED_MEDIA_SESSION_TAGS["*"]?.contains(sessionId) == true ||
                Stuff.BLOCKED_MEDIA_SESSION_TAGS[appId]
                    ?.contains(sessionId) == true)

    fun putOriginals(artist: String, title: String) =
        putOriginals(artist, title, "", "", 0, null, emptyMap())

    fun putOriginals(
        artist: String,
        title: String,
        album: String,
        albumArtist: String,
        durationMillis: Long,
        trackId: String?,
        extraData: Map<String, String>
    ) {
        origArtist = artist
        this.artist = artist
        origTitle = title
        this.title = title
        origAlbum = album
        this.album = album
        origAlbumArtist = albumArtist
        this.albumArtist = albumArtist

        this.durationMillis = durationMillis
        hash = Objects.hash(albumArtist, artist, album, title, appId, sessionId)
        preprocessed = false
        additionalMetadataFetched = false
        this.trackId = trackId

        extras.clear()
        extras.putAll(extraData)
    }

    fun prepareForScrobbling() {
        if (!preprocessed) {
            artist = MetadataUtils.sanitizeArtist(origArtist)
            album = MetadataUtils.sanitizeAlbum(origAlbum)
            albumArtist = MetadataUtils.sanitizeAlbumArtist(origAlbumArtist)
            userPlayCount = 0
            userLoved = false
        }

        isPlaying = true
        playStartTime = System.currentTimeMillis()
        lastScrobbleHash = hash
        lastSubmittedScrobbleHash = 0
    }

    fun updateUserProps(
        userPlayCount: Int = this.userPlayCount,
        userLoved: Boolean = this.userLoved,
    ) {
        this.userPlayCount = userPlayCount
        this.userLoved = userLoved
    }

    fun resetTimePlayed() {
        timePlayed = 0L
    }

    fun addTimePlayed() {
        timePlayed += System.currentTimeMillis() - playStartTime
    }

    fun toScrobbleData(useOriginals: Boolean) = ScrobbleData(
        track = if (useOriginals) origTitle else title,
        artist = if (useOriginals) origArtist else artist,
        album = (if (useOriginals) origAlbum else album).ifEmpty { null },
        albumArtist = (if (useOriginals) origAlbumArtist else albumArtist).ifEmpty { null },
        timestamp = playStartTime,
        duration = durationMillis.takeIf { it >= 30000L },
        appId = appId,
    )

    fun putPreprocessedData(sd: ScrobbleData, additionalMetadataFetched: Boolean) {
        title = sd.track
        album = sd.album.orEmpty()
        artist = sd.artist
        albumArtist = sd.albumArtist.orEmpty()
        preprocessed = true
        this.additionalMetadataFetched = additionalMetadataFetched
    }

    fun markAsScrobbled() {
        if (lastScrobbleHash == hash) {
            lastSubmittedScrobbleHash = hash
            isPlaying = false
        }
    }
}