package com.arn.scrobble.media


import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Objects

abstract class MediaListener(
    val scope: CoroutineScope,
    private val scrobbleQueue: ScrobbleQueue,
) {
    private val mainPrefs = PlatformStuff.mainPrefs

    protected val blockedPackages =
        mainPrefs.data.map { it.blockedPackages }
            .stateIn(scope, SharingStarted.Lazily, Stuff.mainPrefsInitialValue.blockedPackages)

    protected val allowedPackages =
        mainPrefs.data.map { it.allowedPackages }
            .stateIn(scope, SharingStarted.Lazily, Stuff.mainPrefsInitialValue.allowedPackages)

    protected val scrobblerEnabled =
        mainPrefs.data.map { it.scrobblerEnabled }
            .stateIn(scope, SharingStarted.Lazily, Stuff.mainPrefsInitialValue.scrobblerEnabled)

    protected val autoDetectApps =
        mainPrefs.data.map { it.autoDetectAppsP }
            .stateIn(scope, SharingStarted.Lazily, Stuff.mainPrefsInitialValue.autoDetectAppsP)

    protected val loggedIn =
        mainPrefs.data.map { it.scrobbleAccounts.isNotEmpty() }
            .stateIn(
                scope,
                SharingStarted.Lazily,
                Stuff.mainPrefsInitialValue.scrobbleAccounts.isNotEmpty()
            )

    private val packageTagTrackMap = mutableMapOf<String, PlayingTrackInfo>()
    protected var mutedHash: Int? = null

    abstract fun isMediaPlaying(): Boolean

    fun findTrackInfoByHash(hash: Int) =
        packageTagTrackMap.values.find { it.hash == hash }

    fun findPlayingTrackInfo() =
        packageTagTrackMap.values.find { it.isPlaying }

    fun findTrackInfoByKey(key: String) =
        packageTagTrackMap[key]

    fun putTrackInfo(key: String, trackInfo: PlayingTrackInfo) {
        packageTagTrackMap[key] = trackInfo
    }

    abstract fun mute(hash: Int)

    abstract fun unmute(clearMutedHash: Boolean)

    abstract fun skip(hash: Int)

    abstract fun love(hash: Int)

    abstract fun unlove(hash: Int)

    abstract fun removeSessions(
        tokensToKeep: Set<*>,
        appIdsToKeep: Set<String>? = null,
    )

    abstract fun hasOtherPlayingControllers(thisTrackInfo: PlayingTrackInfo): Boolean

    protected fun shouldScrobble(appId: String): Boolean {
        val should = scrobblerEnabled.value && loggedIn.value &&
                (appId in allowedPackages.value ||
                        (autoDetectApps.value && appId !in blockedPackages.value))

        return should
    }

    abstract fun shouldIgnoreOrigArtist(trackInfo: PlayingTrackInfo): Boolean

    open inner class SessionTracker(
        val trackInfo: PlayingTrackInfo,
    ) {

        var lastPlaybackState: CommonPlaybackState? = null
        private var lastPosition: Long = -1
        var isMuted = false

        private fun scrobble() {
            if (hasOtherPlayingControllers(trackInfo) && trackInfo.hasBlockedTag) {
                Logger.d { "multiple scrobblable controllers for ${trackInfo.appId}, ignoring ${trackInfo.sessionId}" }
                pause()
                return
            }

            Logger.d { "playing: timePlayed=${trackInfo.timePlayed} title=${trackInfo.title} hash=${trackInfo.hash}" }

            trackInfo.playStartTime = System.currentTimeMillis()
            scrobbleQueue.remove(trackInfo.lastScrobbleHash)

            trackInfo.lastScrobbleHash = trackInfo.hash
            trackInfo.lastSubmittedScrobbleHash = 0

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)

            scrobbleQueue.nowPlaying(
                trackInfo = trackInfo,
                appIsAllowListed = trackInfo.appId in allowedPackages.value
            )
        }

        fun metadataChanged(metadata: MetadataInfo, canDoFallbackScrobble: Boolean) {
            val sameAsOld =
                metadata.artist == trackInfo.origArtist &&
                        metadata.title == trackInfo.origTitle &&
                        metadata.album == trackInfo.origAlbum &&
                        metadata.album_artist == trackInfo.origAlbumArtist
            val onlyDurationUpdated = sameAsOld && metadata.duration != trackInfo.durationMillis

            if (!sameAsOld || onlyDurationUpdated) {
                trackInfo.putOriginals(
                    metadata.artist,
                    metadata.title,
                    metadata.album,
                    metadata.album_artist
                )

                trackInfo.ignoreOrigArtist = shouldIgnoreOrigArtist(trackInfo)

                trackInfo.canDoFallbackScrobble = canDoFallbackScrobble

                trackInfo.durationMillis = metadata.duration
                trackInfo.hash =
                    Objects.hash(metadata.artist, metadata.album, metadata.title, trackInfo.appId)

                if (mutedHash != null && trackInfo.hash != mutedHash && lastPlaybackState == CommonPlaybackState.Playing)
                    unmute(clearMutedHash = isMuted)

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if ((!scrobbleQueue.has(trackInfo.hash) || onlyDurationUpdated) &&
                    lastPlaybackState == CommonPlaybackState.Playing &&
                    metadata.artist.isNotEmpty() && metadata.title.isNotEmpty()
                ) {
                    trackInfo.timePlayed = 0
                    scrobble()
                }
            }
        }

        private fun ignoreScrobble() {
            // scrobbling may have already started from onMetadataChanged
            scrobbleQueue.remove(trackInfo.lastScrobbleHash)
            PanoNotifications.removeNotificationByTag(trackInfo.appId)

            // do not scrobble again
            lastPlaybackState = CommonPlaybackState.None
        }

        fun playbackStateChanged(
            playbackInfo: PlaybackInfo,
            ignoreScrobble: Boolean,
        ) {
            if (ignoreScrobble) {
                ignoreScrobble()
                return
            }

            val isPossiblyAtStart = playbackInfo.position < START_POS_LIMIT

            if (lastPlaybackState == playbackInfo.state /* bandcamp does this */ &&
                !(playbackInfo.state == CommonPlaybackState.Playing && isPossiblyAtStart)
            )
                return

            when (playbackInfo.state) {
                CommonPlaybackState.Paused,
                CommonPlaybackState.Stopped,
                CommonPlaybackState.None,
                    -> {
                    pause()
                    Logger.d { "paused timePlayed=${trackInfo.timePlayed}" }
                }

                CommonPlaybackState.Playing -> {
                    if (mutedHash != null && trackInfo.hash != mutedHash)
                        unmute(clearMutedHash = isMuted)

                    if (trackInfo.title != "" && trackInfo.artist != "") {

                        if (!isMuted && trackInfo.hash == mutedHash)
                            mute(trackInfo.hash)
                        // ignore state=playing, pos=lowValue spam
                        if (lastPlaybackState == playbackInfo.state && trackInfo.lastScrobbleHash == trackInfo.hash &&
                            System.currentTimeMillis() - trackInfo.playStartTime < START_POS_LIMIT * 2
                        )
                            return

                        if (trackInfo.hash != trackInfo.lastScrobbleHash || (playbackInfo.position >= 0L && isPossiblyAtStart))
                            trackInfo.timePlayed = 0

                        if (!scrobbleQueue.has(trackInfo.hash) &&
                            ((playbackInfo.position >= 0L && isPossiblyAtStart) ||
                                    trackInfo.hash != trackInfo.lastSubmittedScrobbleHash)
                        ) {
                            scrobble()
                        }
                    }
                }

                else -> {
                }
            }

            lastPosition = playbackInfo.position // can be -1
            lastPlaybackState = playbackInfo.state
            if (playbackInfo.state != CommonPlaybackState.Waiting)
                lastPlaybackState = playbackInfo.state

        }

        fun pause() {
            if (lastPlaybackState == CommonPlaybackState.Playing) {
                if (scrobbleQueue.has(trackInfo.lastScrobbleHash))
                    trackInfo.timePlayed += System.currentTimeMillis() - trackInfo.playStartTime
                else
                    trackInfo.timePlayed = 0
            }

            scrobbleQueue.remove(
                trackInfo.lastScrobbleHash,
            )
            if (!hasOtherPlayingControllers(trackInfo))
                PanoNotifications.removeNotificationByTag(trackInfo.appId)
            if (isMuted)
                unmute(clearMutedHash = false)
        }

        fun resetMeta() {
            trackInfo.apply {
                artist = ""
                album = ""
                title = ""
                albumArtist = ""
                durationMillis = 0L
            }
        }
    }

    companion object {
        private const val START_POS_LIMIT = 1500L
    }
}
