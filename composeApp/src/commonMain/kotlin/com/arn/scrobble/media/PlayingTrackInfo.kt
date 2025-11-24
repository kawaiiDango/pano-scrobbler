package com.arn.scrobble.media

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff
import java.util.Objects
import kotlin.math.abs

class PlayingTrackInfo(
    val appId: String,
    val sessionId: String,
) {
    enum class ScrobbledState {
        NONE,
        PREPARED,
        PREPROCESSED,
        ADDITIONAL_METADATA_FETCHED,
        SUBMITTED,
        CANCELLED,
    }

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

    var artUrl: String = ""
        private set

    var trackId: String? = null
        private set

    var timelineStartTime: Long = 0
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

    var scrobbledState: ScrobbledState = ScrobbledState.NONE
        private set

    var timePlayed: Long = 0L
        private set

    val extras = mutableMapOf<String, String>()

    val hasBlockedTag: Boolean =
        (Stuff.BLOCKED_MEDIA_SESSION_TAGS["*"]?.contains(sessionId) == true ||
                Stuff.BLOCKED_MEDIA_SESSION_TAGS[appId]
                    ?.contains(sessionId) == true)

    fun resetMeta() =
        putOriginals("", "", "", "", 0, null, "", emptyMap())

    fun putOriginals(
        artist: String,
        title: String,
        album: String,
        albumArtist: String,
        durationMillis: Long,
        trackId: String?,
        artUrl: String,
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
        this.trackId = trackId

        this.artUrl = artUrl

        extras.clear()
        extras.putAll(extraData)

        scrobbledState = ScrobbledState.NONE
    }

    fun setArtUrl(artUrl: String?) {
        this.artUrl = artUrl.orEmpty()
    }

    // this is only done for desktop
    fun setTimelineStartTime(seekPosition: Long): Boolean {
        if (seekPosition == -1L)
            return false

        if (durationMillis <= 0) {
            if (timelineStartTime != 0L) {
                timelineStartTime = 0L
                return true
            }
            return false
        }

        val startTime = System.currentTimeMillis() - seekPosition

        // only change if delta is significant
        if (abs(timelineStartTime - startTime) > 1000) {
            timelineStartTime = startTime
            return true
        }

        return false
    }

    fun prepareForScrobbling() {
        if (scrobbledState < ScrobbledState.PREPROCESSED) {
            artist = MetadataUtils.sanitizeArtist(origArtist)
            album = MetadataUtils.sanitizeAlbum(origAlbum)
            albumArtist = MetadataUtils.sanitizeAlbumArtist(origAlbumArtist)
            userPlayCount = 0
            userLoved = false
        }

        isPlaying = true
        playStartTime = System.currentTimeMillis()
        scrobbledState = minOf(ScrobbledState.ADDITIONAL_METADATA_FETCHED, scrobbledState)
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

    fun paused() {
        isPlaying = false
    }

    fun resumed() {
        isPlaying = true
    }

    fun toScrobbleData(useOriginals: Boolean) = ScrobbleData(
        track = if (useOriginals) origTitle else title,
        artist = if (useOriginals) origArtist else artist,
        album = (if (useOriginals) origAlbum else album).ifEmpty { null },
        albumArtist = (if (useOriginals) origAlbumArtist else albumArtist).ifEmpty { null },
        timestamp = playStartTime,
        duration = durationMillis.takeIf { it > 0 },
        appId = appId,
    )

    fun toTrackPlayingEvent() = PlayingTrackNotifyEvent.TrackPlaying(
        scrobbleData = toScrobbleData(false),
        origScrobbleData = toScrobbleData(true),
        hash = hash,
        nowPlaying = scrobbledState < ScrobbledState.SUBMITTED,
        userLoved = userLoved,
        userPlayCount = userPlayCount,
        artUrl = artUrl,
        timelineStartTime = timelineStartTime,
    )

    fun putPreprocessedData(sd: ScrobbleData, additionalMetadataFetched: Boolean) {
        title = sd.track
        album = sd.album.orEmpty()
        artist = sd.artist
        albumArtist = sd.albumArtist.orEmpty()

        if (additionalMetadataFetched)
            scrobbledState = ScrobbledState.ADDITIONAL_METADATA_FETCHED
        else
            scrobbledState = ScrobbledState.PREPROCESSED
    }

    fun scrobbled() {
        scrobbledState = ScrobbledState.SUBMITTED
    }

    fun cancelled() {
        scrobbledState = ScrobbledState.CANCELLED
    }
}