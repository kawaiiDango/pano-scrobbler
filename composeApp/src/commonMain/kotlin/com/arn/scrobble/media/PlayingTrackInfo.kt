package com.arn.scrobble.media

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.MetadataUtils
import java.util.Objects
import kotlin.math.abs

class PlayingTrackInfo(
    val appId: String, // normalized app id
    val uniqueId: String,
    cachedTrackInfo: PlayingTrackInfo?,
) {
    enum class ScrobbledState {
        NONE,
        PREPARED,
        PREPROCESSED,
        ADDITIONAL_METADATA_FETCHED,
        NOW_PLAYING_SUBMITTED,
        SCROBBLE_SUBMITTED,
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

    // null = not fetched, empty = fetched but no art
    var artUrl: String? = null
        private set

    var normalizedUrlHost: String? = null
        private set

    private var msid: String? = null

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
    val extras = mutableMapOf<String, String>()

    // cached values
    var timelineStartTime: Long = cachedTrackInfo?.timelineStartTime ?: 0L
        private set
    var playStartTime: Long = cachedTrackInfo?.playStartTime ?: 0L
        private set
    var scrobbledState: ScrobbledState = cachedTrackInfo?.scrobbledState ?: ScrobbledState.NONE
        private set
    var timePlayed: Long = cachedTrackInfo?.timePlayed ?: 0L
        private set
    var lastScrobbleHash: Int = cachedTrackInfo?.lastScrobbleHash ?: 0
        private set

//    fun resetMeta() =
//        putOriginals("", "", "", "", 0, null, null, emptyMap())

    fun putOriginals(
        artist: String,
        title: String,
        album: String,
        albumArtist: String,
        durationMillis: Long,
        normalizedUrlHost: String?,
        artUrl: String?,
        extraData: Map<String, String> = emptyMap(),
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
        hash = Objects.hash(albumArtist, artist, album, title, appId, uniqueId)
        this.normalizedUrlHost = normalizedUrlHost

        this.artUrl = artUrl

        extras.clear()
        extras.putAll(extraData)

        scrobbledState = ScrobbledState.NONE
        msid = null
    }

    fun setArtUrl(artUrl: String?) {
        this.artUrl = artUrl
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
        lastScrobbleHash = hash

        if (scrobbledState < ScrobbledState.PREPROCESSED) {
            artist = MetadataUtils.sanitizeArtist(origArtist)
            album = MetadataUtils.sanitizeAlbum(origAlbum)
            albumArtist = MetadataUtils.sanitizeAlbumArtist(origAlbumArtist)
            userPlayCount = 0
            userLoved = false
            scrobbledState = ScrobbledState.PREPARED
        }

        isPlaying = true
        playStartTime = System.currentTimeMillis()
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
        notiKey = uniqueId,
        scrobbleData = toScrobbleData(false),
        origScrobbleData = toScrobbleData(true),
        msid = msid,
        hash = hash,
        nowPlaying = scrobbledState < ScrobbledState.SCROBBLE_SUBMITTED,
        userLoved = userLoved,
        userPlayCount = userPlayCount,
        artUrl = artUrl,
        timelineStartTime = timelineStartTime,
        preprocessed = scrobbledState >= ScrobbledState.PREPROCESSED,
    )

    fun putPreprocessedData(sd: ScrobbleData, additionalMetadataFetched: Boolean) {
        title = sd.track
        album = sd.album.orEmpty()
        artist = sd.artist
        albumArtist = sd.albumArtist.orEmpty()

        scrobbledState = if (additionalMetadataFetched)
            ScrobbledState.ADDITIONAL_METADATA_FETCHED
        else
            ScrobbledState.PREPROCESSED
    }

    fun nowPlayingSubmitted(msid: String?) {
        scrobbledState = ScrobbledState.NOW_PLAYING_SUBMITTED
        this.msid = msid
    }

    fun scrobbled() {
        scrobbledState = ScrobbledState.SCROBBLE_SUBMITTED
    }

    fun cancelled() {
        scrobbledState = ScrobbledState.CANCELLED
    }
}