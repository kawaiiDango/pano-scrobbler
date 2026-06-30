package com.arn.scrobble.media


import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

abstract class MediaListener(
    protected val scope: CoroutineScope,
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

    private var scrobbleLockKey: MediaTrackerKey? = null

    private fun keyOf(tracker: SessionTracker): MediaTrackerKey? =
        sessionTrackers.entries.find { it.value === tracker }?.key

    private fun PlayingTrackInfo.isEligibleForScrobble() = isPlaying &&
            title.isNotEmpty() &&
            artist.isNotEmpty() &&
            scrobbledState < PlayingTrackInfo.ScrobbledState.CANCELLED

    // Called right before a session wants to actually submit a scrobble.
    @Synchronized
    private fun claimScrobbleLock(
        requesterKey: MediaTrackerKey,
        onWon: () -> Unit,
        onLost: (winnerTrackInfo: PlayingTrackInfo) -> Unit,
    ) {
        val holderKey = scrobbleLockKey

        if (holderKey == null || holderKey == requesterKey) {
            scrobbleLockKey = requesterKey
            onWon()
            return
        }

        val holder = sessionTrackers[holderKey]

        if (holder == null || !holder.trackInfo.isEligibleForScrobble()) {
            // stale holder, take over outright
            scrobbleLockKey = requesterKey
            onWon()
            return
        }

        // genuine contention - give it SCROBBLE_LOCK_GRACE_S to resolve itself
        scope.launch {
            delay(SCROBBLE_LOCK_GRACE_S.seconds)

            synchronized(this@MediaListener) {
                if (scrobbleLockKey != holderKey)
                    return@synchronized // already resolved some other way in the meantime

                val holderTrackInfo = sessionTrackers[holderKey]?.trackInfo
                val requesterTrackInfo = sessionTrackers[requesterKey]?.trackInfo

                val holderStillPlaying = holderTrackInfo?.isEligibleForScrobble() ?: false

                if (holderStillPlaying) {
                    val holderAppId = holderTrackInfo.appId
                    val requesterAppId = requesterTrackInfo?.appId

                    Logger.i { "duplicate detected, requester=$requesterAppId holder=$holderAppId" }
                    Logger.d { "requesterTrackInfo:$requesterTrackInfo\nholderTrackInfo:$holderTrackInfo" }

                    onLost(holderTrackInfo)
                } else {
                    scrobbleLockKey = requesterKey
                    onWon()
                }
            }
        }
    }

    @Synchronized
    private fun releaseScrobbleLockIfHeldBy(key: MediaTrackerKey) {
        if (scrobbleLockKey == key) {
            scrobbleLockKey = null
            promoteNextEligibleTracker()
        }
    }

    private fun promoteNextEligibleTracker() {
        sessionTrackers.values
            .filter {
                it.trackInfo.isEligibleForScrobble() && !scrobbleQueue.has(it.trackInfo.hash)
            }
            .minByOrNull { it.trackInfo.playStartTime }
            ?.scrobble()
    }

    abstract fun isMediaPlaying(): Boolean

    fun findTrackerByHash(hash: Int) =
        sessionTrackers.values.find { it.trackInfo.hash == hash }

    fun findPlayingTracker() =
        sessionTrackers.values.find { it.trackInfo.isPlaying }

    fun createTrackInfo(appId: String, notiKey: String): PlayingTrackInfo {
        val trackInfo = PlayingTrackInfo(
            appId,
            notiKey,
            cachedInfos[appId]
        )

        cachedInfos[appId] = trackInfo

        return trackInfo
    }

    abstract fun mute(hash: Int)

    abstract fun unmute(clearMutedHash: Boolean)

    @Synchronized
    fun removeSessions(
        keysToKeep: Set<MediaTrackerKey>,
    ) {
        val it = sessionTrackers.iterator()
        while (it.hasNext()) {
            val (sessionKey, sessionTracker) = it.next()
            if (sessionKey !in keysToKeep) {
                sessionTracker.stop()
                releaseScrobbleLockIfHeldBy(sessionKey)
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
        var lastDuration = trackInfo.durationMillis
            private set

        fun scrobble() {
            Logger.d { "playing: timePlayed=${trackInfo.timePlayed} title=${trackInfo.title} hash=${trackInfo.hash.toHexString()}" }

            scrobbleQueue.remove(trackInfo.lastScrobbleHash)

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)

            // calc delay
            val delayMillis = 4.minutes.inWholeMilliseconds
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

            keyOf(this)?.let { key ->
                claimScrobbleLock(
                    requesterKey = key,
                    onWon = {
                        scrobbleQueue.scrobble(
                            trackInfo = trackInfo,
                            appIsAllowListed = trackInfo.appId in allowedPackages.value,
                            delay = finalDelay,
                        )
                    },
                    onLost = { winnerTrackInfo ->
                        scrobbleQueue.remove(trackInfo.hash)
                        if (winnerTrackInfo.notiKey != trackInfo.notiKey)
                            PanoNotifications.removeNotificationByKey(trackInfo.notiKey)
                    },
                )
            }
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

            if (!sameAsOld || (onlyDurationUpdated && metadata.duration > 0)) {
                trackInfo.putOriginals(
                    artist = metadata.artist,
                    title = metadata.title,
                    album = metadata.album,
                    albumArtist = metadata.albumArtist,
                    trackNumber = metadata.trackNumber,
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
                    scrobble()
                }
            }

            lastDuration = metadata.duration
        }

        fun ignoreScrobble() {
            // scrobbling may have already started from onMetadataChanged
            scrobbleQueue.remove(trackInfo.lastScrobbleHash)
            PanoNotifications.removeNotificationByKey(trackInfo.notiKey)
            trackInfo.cancelled()
            // do not scrobble again
            lastPlaybackState = CommonPlaybackState.None

            keyOf(this)?.let {
                releaseScrobbleLockIfHeldBy(it)
            }
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
            }
            trackInfo.paused()

            scrobbleQueue.remove(trackInfo.lastScrobbleHash)
            PanoNotifications.removeNotificationByKey(trackInfo.notiKey)
            if (isMuted)
                unmute(clearMutedHash = false)

            keyOf(this)?.let { releaseScrobbleLockIfHeldBy(it) }
        }

        abstract fun love()

        abstract fun unlove()
        abstract fun skip()
        abstract fun stop()
    }

    companion object {
        private const val START_POS_LIMIT = 1500L

        // how long a newly-arriving session is given to find out whether the
        // current lock holder is just lingering through a state-update lag
        // (transition overlap) vs. genuinely still playing (real concurrency)
        private const val SCROBBLE_LOCK_GRACE_S = 8
    }
}
