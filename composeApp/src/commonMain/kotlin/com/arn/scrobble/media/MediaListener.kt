package com.arn.scrobble.media


import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
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
    )

    abstract fun hasOtherPlayingControllers(appId: String): Boolean

    abstract fun shouldScrobble(rawAppId: String): Boolean

    open inner class SessionTracker(
        val trackInfo: PlayingTrackInfo,
    ) {

        private var lastPlaybackState: CommonPlaybackState? = null
        private var lastScrobbleHash = 0
        var isMuted = false

        private fun scrobble() {
            if (hasOtherPlayingControllers(trackInfo.appId) &&
                trackInfo.hasBlockedTag
            ) {
                Logger.d { "multiple scrobblable controllers for ${trackInfo.appId}, ignoring ${trackInfo.uniqueId}" }
                pause()
                return
            }

            Logger.d { "playing: timePlayed=${trackInfo.timePlayed} title=${trackInfo.title} hash=${trackInfo.hash.toHexString()}" }

            scrobbleQueue.remove(lastScrobbleHash)
            lastScrobbleHash = trackInfo.hash

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


        fun metadataChanged(metadata: MetadataInfo, extras: Map<String, String>) {
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
                    trackId = metadata.trackId.ifEmpty { null },
                    artUrl = metadata.artUrl.ifEmpty { null },
                    extraData = extras,
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
            scrobbleQueue.remove(lastScrobbleHash)
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

                    if (trackInfo.title != "" && trackInfo.artist != "") {
                        trackInfo.resumed()

                        if (!isMuted && trackInfo.hash == mutedHash)
                            mute(trackInfo.hash)

                        if (trackInfo.hash != lastScrobbleHash ||
                            (playbackInfo.position >= 0L && isPossiblyAtStart && timelineChanged)
                        )
                            trackInfo.resetTimePlayed()

                        if (!scrobbleQueue.has(trackInfo.hash) &&
                            ((playbackInfo.position >= 0L && isPossiblyAtStart) ||
                                    trackInfo.scrobbledState < PlayingTrackInfo.ScrobbledState.SUBMITTED &&
                                    // ignore state=playing, pos=lowValue spam
                                    !(!playbackStateChanged &&
                                            lastScrobbleHash == trackInfo.hash &&
                                            System.currentTimeMillis() - trackInfo.playStartTime < START_POS_LIMIT * 2
                                            )
                                    )
                        ) {
                            scrobble()
                        } else if ((timelineChanged && notifyTimelineUpdates || playbackStateChanged) &&
                            trackInfo.scrobbledState <= PlayingTrackInfo.ScrobbledState.SUBMITTED
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
                if (scrobbleQueue.has(lastScrobbleHash))
                    trackInfo.addTimePlayed()
                else
                    trackInfo.resetTimePlayed()
                trackInfo.paused()
            }

            scrobbleQueue.remove(lastScrobbleHash)
            if (!hasOtherPlayingControllers(trackInfo.appId))
                PanoNotifications.removeNotificationByKey(trackInfo.uniqueId)
            if (isMuted)
                unmute(clearMutedHash = false)
        }
    }

    companion object {
        private const val START_POS_LIMIT = 1500L
    }
}
