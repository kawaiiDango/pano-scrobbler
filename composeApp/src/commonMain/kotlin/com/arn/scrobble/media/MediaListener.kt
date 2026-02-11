package com.arn.scrobble.media


import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import kotlinx.coroutines.CoroutineScope
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

    protected val allowedPackages = mainPrefs.data.stateInWithCache(scope) { it.allowedPackages }

    protected val scrobblerEnabled =
        mainPrefs.data.stateInWithCache(scope) { it.scrobblerEnabled && it.scrobbleAccounts.isNotEmpty() }

    protected abstract val notifyTimelineUpdates: Boolean

    private val scrobbleTimingPrefs =
        PlatformStuff.mainPrefs.data.stateInWithCache(scope) {
            ScrobbleTimingPrefs(
                delayPercent = it.delayPercentP,
                delaySecs = it.delaySecsP,
                minDurationSecs = it.minDurationSecsP
            )
        }

    protected val sessionTrackers = mutableMapOf<MediaTrackerKey, SessionTracker>()
    private val cachedInfos = mutableMapOf<String, PlayingTrackInfo>()
    protected var mutedHash: Int? = null

    abstract fun isMediaPlaying(): Boolean

    fun findTrackerByHash(hash: Int) =
        sessionTrackers.values.find { it.trackInfo.hash == hash }

    fun findPlayingTracker() =
        sessionTrackers.values.find { it.trackInfo.isPlaying }

    fun createTrackInfo(appId: String, uniqueId: String): PlayingTrackInfo {
        val trackInfo = PlayingTrackInfo(
            appId,
            uniqueId,
            cachedInfos[appId]
        )

        cachedInfos[appId] = trackInfo

        return trackInfo
    }

    abstract fun mute(hash: Int)

    abstract fun unmute(clearMutedHash: Boolean)

    @Synchronized
    fun removeSessions(
        keysToKeep: Set<*>,
    ) {
        val it = sessionTrackers.iterator()
        while (it.hasNext()) {
            val (sessionKey, sessionTracker) = it.next()
            if (sessionKey !in keysToKeep) {
                sessionTracker.stop()
                it.remove()
            }
        }
    }

    abstract fun shouldScrobble(rawAppId: String): Boolean

    abstract inner class SessionTracker(
        val trackInfo: PlayingTrackInfo,
    ) {

        private var lastPlaybackState: CommonPlaybackState? = null
        var isMuted = false

        private fun scrobble() {
            Logger.d { "playing: timePlayed=${trackInfo.timePlayed} title=${trackInfo.title} hash=${trackInfo.hash.toHexString()}" }

            scrobbleQueue.remove(trackInfo.lastScrobbleHash)

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)

            // calc delay
            val delayMillis = scrobbleTimingPrefs.value.delaySecs * 1000L
            val delayFraction = scrobbleTimingPrefs.value.delayPercent / 100.0
            val delayMillisFraction = if (trackInfo.durationMillis > 0)
                (trackInfo.durationMillis * delayFraction).toLong()
            else // Assume 2 min track if duration is unknown. This happens mostly with radio apps
                (120_000 * delayFraction).toLong()

            // don't scrobble < n seconds
            // -subtract some to round off. Sometimes 30 second tracks are reported as 29988ms
            var finalDelay = min(delayMillisFraction, delayMillis)
                .coerceAtLeast(scrobbleTimingPrefs.value.minDurationSecs * 1000L - 600L)

            finalDelay = (finalDelay - trackInfo.timePlayed)
                .coerceAtLeast(2000)// deal with negative or 0 delay

            scrobbleQueue.scrobble(
                trackInfo = trackInfo,
                appIsAllowListed = trackInfo.appId in allowedPackages.value,
                delay = finalDelay,
            )
        }


        fun metadataChanged(
            metadata: MetadataInfo,
            ignoreScrobble: Boolean,
        ) {
            val sameAsOld =
                metadata.artist == trackInfo.origArtist &&
                        metadata.title == trackInfo.origTitle &&
                        metadata.album == trackInfo.origAlbum &&
                        metadata.albumArtist == trackInfo.origAlbumArtist
            val onlyDurationUpdated = sameAsOld && metadata.duration != trackInfo.durationMillis

            if (BuildKonfig.DEBUG || (!sameAsOld || onlyDurationUpdated))
                Logger.i { "$metadata $lastPlaybackState ${hashCode().toHexString()}" }

            if (!sameAsOld || onlyDurationUpdated) {
                trackInfo.putOriginals(
                    artist = metadata.artist,
                    title = metadata.title,
                    album = metadata.album,
                    albumArtist = metadata.albumArtist,
                    durationMillis = metadata.duration,
                    normalizedUrlHost = metadata.normalizedUrlHost,
                    artUrl = metadata.artUrl,
                )

                if (mutedHash != null && trackInfo.hash != mutedHash && lastPlaybackState == CommonPlaybackState.Playing)
                    unmute(clearMutedHash = isMuted)

                if (ignoreScrobble || metadata.artist.isEmpty() || metadata.title.isEmpty()) {
                    ignoreScrobble()
                } else if ((!scrobbleQueue.has(trackInfo.hash) || onlyDurationUpdated) &&
                    lastPlaybackState == CommonPlaybackState.Playing
                ) {
                    // for cases:
                    // - meta is sent after play
                    // - "gapless playback", where playback state never changes
                    trackInfo.resetTimePlayed()
                    scrobble()
                }
            }
        }

        private fun ignoreScrobble() {
            // scrobbling may have already started from onMetadataChanged
            scrobbleQueue.remove(trackInfo.lastScrobbleHash)
            PanoNotifications.removeNotificationByKey(trackInfo.uniqueId)
            trackInfo.cancelled()
            // do not scrobble again
            lastPlaybackState = CommonPlaybackState.None
        }

        fun playbackStateChanged(
            playbackInfo: PlaybackInfo,
            ignoreScrobble: Boolean,
        ) {
            if (BuildKonfig.DEBUG || (lastPlaybackState != playbackInfo.state))
                Logger.i { "$playbackInfo lastPlaybackState: $lastPlaybackState ${hashCode().toHexString()}" }

            if (ignoreScrobble) {
                ignoreScrobble()
                return
            }

            val isPossiblyAtStart =
                playbackInfo.position != -1L && playbackInfo.position < START_POS_LIMIT

            val timelineChanged = trackInfo.setTimelineStartTime(playbackInfo.position) &&
                    playbackInfo.state == CommonPlaybackState.Playing

            val playbackStateChanged = lastPlaybackState != playbackInfo.state

            if (!playbackStateChanged /* bandcamp does this */ &&
                !(playbackInfo.state == CommonPlaybackState.Playing && isPossiblyAtStart) &&
                !timelineChanged
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

                    if (!isMuted && trackInfo.hash == mutedHash)
                        mute(trackInfo.hash)

                    if (trackInfo.scrobbledState < PlayingTrackInfo.ScrobbledState.CANCELLED) {
                        trackInfo.resumed()

                        if (trackInfo.hash != trackInfo.lastScrobbleHash ||
                            (playbackInfo.position >= 0L && isPossiblyAtStart && timelineChanged)
                        )
                            trackInfo.resetTimePlayed()

                        if (!scrobbleQueue.has(trackInfo.hash) &&
                            ((playbackInfo.position >= 0L && isPossiblyAtStart) ||
                                    trackInfo.scrobbledState < PlayingTrackInfo.ScrobbledState.SCROBBLE_SUBMITTED &&
                                    // ignore state=playing, pos=lowValue spam
                                    !(!playbackStateChanged &&
                                            trackInfo.lastScrobbleHash == trackInfo.hash &&
                                            System.currentTimeMillis() - trackInfo.playStartTime < START_POS_LIMIT * 2
                                            )
                                    )
                        ) {
                            scrobble()
                        } else if ((timelineChanged && notifyTimelineUpdates || playbackStateChanged) &&
                            trackInfo.scrobbledState <= PlayingTrackInfo.ScrobbledState.SCROBBLE_SUBMITTED
                        ) {
                            // update notification
                            notifyPlayingTrackEvent(trackInfo.toTrackPlayingEvent())
                        }
                    }
                }

                else -> {
                }
            }

            if (playbackInfo.state != CommonPlaybackState.Waiting)
                lastPlaybackState = playbackInfo.state

        }

        fun pause() {
            if (lastPlaybackState == CommonPlaybackState.Playing) {
                if (scrobbleQueue.has(trackInfo.lastScrobbleHash))
                    trackInfo.addTimePlayed()
                else
                    trackInfo.resetTimePlayed()
                trackInfo.paused()
            }

            scrobbleQueue.remove(trackInfo.lastScrobbleHash)
            PanoNotifications.removeNotificationByKey(trackInfo.uniqueId)
            if (isMuted)
                unmute(clearMutedHash = false)
        }

        abstract fun love()

        abstract fun unlove()
        abstract fun skip()
        abstract fun stop()
    }

    companion object {
        private const val START_POS_LIMIT = 1500L
    }
}
