package com.arn.scrobble.media


import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.min

abstract class MediaListener(
    val scope: CoroutineScope,
    private val scrobbleQueue: ScrobbleQueue,
) {
    private data class ScrobbleTimingPrefs(
        val delayPercent: Int,
        val delaySecs: Int,
        val minDurationSecs: Int,
    )

    private val mainPrefs = PlatformStuff.mainPrefs

    protected val blockedPackages =
        mainPrefs.data.map { it.blockedPackages }
            .stateIn(scope, SharingStarted.Eagerly, Stuff.mainPrefsInitialValue.blockedPackages)

    protected val allowedPackages =
        mainPrefs.data.map { it.allowedPackages }
            .stateIn(scope, SharingStarted.Eagerly, Stuff.mainPrefsInitialValue.allowedPackages)

    protected val scrobblerEnabled =
        mainPrefs.data.map { it.scrobblerEnabled && it.scrobbleAccounts.isNotEmpty() }
            .stateIn(
                scope, SharingStarted.Eagerly, Stuff.mainPrefsInitialValue.scrobblerEnabled &&
                        Stuff.mainPrefsInitialValue.scrobbleAccounts.isNotEmpty()
            )

    private val scrobbleTimingPrefs =
        PlatformStuff.mainPrefs.data.map {
            ScrobbleTimingPrefs(
                delayPercent = it.delayPercentP,
                delaySecs = it.delaySecsP,
                minDurationSecs = it.minDurationSecsP
            )
        }
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                Stuff.mainPrefsInitialValue.let {
                    ScrobbleTimingPrefs(
                        delayPercent = it.delayPercentP,
                        delaySecs = it.delaySecsP,
                        minDurationSecs = it.minDurationSecsP
                    )
                })

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

    abstract fun hasOtherPlayingControllers(appId: String, sessionId: String): Boolean

    protected fun shouldScrobble(appId: String): Boolean {
        val should = scrobblerEnabled.value && (appId in allowedPackages.value)
        return should
    }

    open inner class SessionTracker(
        val trackInfo: PlayingTrackInfo,
    ) {

        var lastPlaybackState: CommonPlaybackState? = null
        private var lastPosition: Long = -1
        var isMuted = false

        private fun scrobble() {
            if (hasOtherPlayingControllers(trackInfo.appId, trackInfo.sessionId) &&
                trackInfo.hasBlockedTag
            ) {
                Logger.d { "multiple scrobblable controllers for ${trackInfo.appId}, ignoring ${trackInfo.sessionId}" }
                pause()
                return
            }

            Logger.d { "playing: timePlayed=${trackInfo.timePlayed} title=${trackInfo.title} hash=${trackInfo.hash}" }

            scrobbleQueue.remove(trackInfo.lastScrobbleHash)

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)

            // calc delay
            val delayMillis = scrobbleTimingPrefs.value.delaySecs * 1000L
            val delayFraction = scrobbleTimingPrefs.value.delayPercent / 100.0
            val delayMillisFraction = if (trackInfo.durationMillis > 0)
                (trackInfo.durationMillis * delayFraction).toLong()
            else
                Long.MAX_VALUE

            var finalDelay = min(delayMillisFraction, delayMillis)
                .coerceAtLeast(scrobbleTimingPrefs.value.minDurationSecs * 1000L) // don't scrobble < n seconds

            finalDelay = (finalDelay - trackInfo.timePlayed)
                .coerceAtLeast(2000)// deal with negative or 0 delay

            scrobbleQueue.scrobble(
                trackInfo = trackInfo,
                appIsAllowListed = trackInfo.appId in allowedPackages.value,
                delay = finalDelay,
            )
        }


        fun metadataChanged(metadata: MetadataInfo, extras: Map<String, String>) {
            val sameAsOld =
                metadata.artist == trackInfo.origArtist &&
                        metadata.title == trackInfo.origTitle &&
                        metadata.album == trackInfo.origAlbum &&
                        metadata.albumArtist == trackInfo.origAlbumArtist
            val onlyDurationUpdated = sameAsOld && metadata.duration != trackInfo.durationMillis

            if (!sameAsOld || onlyDurationUpdated) {
                trackInfo.putOriginals(
                    metadata.artist,
                    metadata.title,
                    metadata.album,
                    metadata.albumArtist,
                    metadata.duration,
                    extras,
                )

                if (mutedHash != null && trackInfo.hash != mutedHash && lastPlaybackState == CommonPlaybackState.Playing)
                    unmute(clearMutedHash = isMuted)

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if ((!scrobbleQueue.has(trackInfo.hash) || onlyDurationUpdated) &&
                    lastPlaybackState == CommonPlaybackState.Playing &&
                    metadata.artist.isNotEmpty() && metadata.title.isNotEmpty()
                ) {
                    trackInfo.resetTimePlayed()
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

            val isPossiblyAtStart =
                playbackInfo.position != -1L && playbackInfo.position < START_POS_LIMIT

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
                            trackInfo.resetTimePlayed()

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
                    trackInfo.addTimePlayed()
                else
                    trackInfo.resetTimePlayed()
            }

            scrobbleQueue.remove(
                trackInfo.lastScrobbleHash,
            )
            if (!hasOtherPlayingControllers(trackInfo.appId, trackInfo.sessionId))
                PanoNotifications.removeNotificationByTag(trackInfo.appId)
            if (isMuted)
                unmute(clearMutedHash = false)
        }

        fun resetMeta() {
            trackInfo.putOriginals("", "")
        }
    }

    companion object {
        private const val START_POS_LIMIT = 1500L
    }
}
