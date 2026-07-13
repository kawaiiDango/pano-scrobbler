package com.arn.scrobble.media

import co.touchlab.kermit.Logger
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.steelseries.SteelSeriesReceiverServer
import com.arn.scrobble.discordrpc.DiscordRpc
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


class DesktopMediaListener(
    scope: CoroutineScope,
    scrobbleQueue: ScrobbleQueue,
) : MediaListener(scope, scrobbleQueue) {

    override val notifyTimelineUpdates = true

    private var sessionInfos: List<SessionInfo> = emptyList()

    private val seenApps =
        PlatformStuff.mainPrefs.data.stateInWithCache(scope) { it.seenApps }

    private val blockedHostnames =
        PlatformStuff.mainPrefs.data.stateInWithCache(scope) { it.blockedHostnames }

    private val seenHostnames =
        PlatformStuff.mainPrefs.data.stateInWithCache(scope) { it.seenHostnames }

    fun start() {
        scope.launch {
            delay(1.seconds)

            combine(scrobblerEnabled, allowedPackages) { scrobblerEnabled, allowedPackages ->
                if (!scrobblerEnabled) {
                    hashSetOf()
                } else
                    allowedPackages.toHashSet()
            }
                .distinctUntilChanged()
                .drop(1) // drop initial value
                .collect {
                    PanoNativeComponents.refreshSessions()

                    // refresh with the same data to check for shouldScrobble again
                    platformActiveSessionsChanged(sessionInfos)
                }
        }
    }

    override fun shouldScrobble(rawAppId: String): Boolean {
        val should = scrobblerEnabled.value &&
                (DesktopStuff.normalizeAppId(rawAppId) in allowedPackages.value)
        return should
    }


    fun platformActiveSessionsChanged(sessions: List<SessionInfo>) {
        this.sessionInfos = sessions
        Logger.i { "controllers: " + sessions.joinToString { it.rawAppId } }

        val normalizedAppIdsToNames = sessions
            .associate { DesktopStuff.normalizeAppId(it.rawAppId) to it.appName }

        val unseenAppItems = normalizedAppIdsToNames
            .filter { (appId, name) -> appId !in seenApps.value }

        if (unseenAppItems.isNotEmpty() && scrobblerEnabled.value) {
            scope.launch {
                PlatformStuff.mainPrefs.updateData { it.copy(seenApps = it.seenApps + unseenAppItems) }

                unseenAppItems.forEach { (appId, friendlyLabel) ->
                    PanoNotifications.notifyAppDetected(appId, friendlyLabel)
                }
            }
        }


        val shouldScrobbleSessions = sessions.filter { shouldScrobble(it.rawAppId) }
        val newSessions = shouldScrobbleSessions.filter { it.rawAppId !in sessionTrackers }

        for (session in newSessions) {
            val normalizedAppId = DesktopStuff.normalizeAppId(session.rawAppId)
            val playingTrackInfo = createTrackInfo(normalizedAppId, session.rawAppId)

            sessionTrackers[playingTrackInfo.notiKey] =
                DesktopSessionTracker(playingTrackInfo.notiKey, playingTrackInfo)
        }

        // Now remove old sessions that are no longer active.
        val shouldScrobbleSessionsAppIds = shouldScrobbleSessions.map { it.rawAppId }.toSet()

        // stop server when tidal stops
        if (!(Stuff.PACKAGE_TIDAL_WIN in shouldScrobbleSessionsAppIds ||
                    Stuff.PACKAGE_TIDAL_WIN_EXE in shouldScrobbleSessionsAppIds ||
                    Stuff.PACKAGE_TIDAL_WIN_STORE in shouldScrobbleSessionsAppIds)
        )
            SteelSeriesReceiverServer.stopServer()

        removeSessions(shouldScrobbleSessionsAppIds)
    }

    override fun isMediaPlaying() =
        sessionTrackers.values.any { it.isMediaPlaying() }

    override fun mute(hash: Int) {
        // if pano didnt mute this, dont unmute later
//        if (mutedHash == null && audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
//            return

        val tracker = findTrackerByHash(hash)
        if (tracker != null) {
            PanoNativeComponents.mute(tracker.trackInfo.notiKey)
            Logger.i { "mute: done" }

            mutedHash = hash
            tracker.isMuted = true
        }
    }

    override fun unmute(clearMutedHash: Boolean) {
        if (mutedHash != null) {
            Logger.i { "unmute: done" }

            val tracker = findTrackerByHash(mutedHash!!)
            tracker?.trackInfo?.notiKey?.let { PanoNativeComponents.unmute(it) }

            tracker?.isMuted = false

            if (clearMutedHash)
                mutedHash = null

        }
    }

    private fun SessionTracker.isMediaPlaying() =
        trackInfo.isPlaying && trackInfo.title.isNotBlank() && trackInfo.artist.isNotBlank()

    fun platformMetadataChanged(uniqueAppId: String, metadata: MetadataInfo) {

        val sessionTracker = sessionTrackers[uniqueAppId] ?: return

        var (metadata, ignoreScrobble) = transformMediaMetadata(
            sessionTracker.trackInfo,
            metadata
        )

        if (!metadata.normalizedUrlHost.isNullOrEmpty() && metadata.normalizedUrlHost in blockedHostnames.value) {
            ignoreScrobble = true
        }

        sessionTracker.metadataChanged(metadata, ignoreScrobble)
    }

    fun platformPlaybackStateChanged(uniqueAppId: String, playbackInfo: PlaybackInfo) {
        val sessionTracker = sessionTrackers[uniqueAppId] ?: return

        if (sessionTracker.lastDuration <= 0 &&
            sessionTracker.trackInfo.isPlaying &&
            playbackInfo.state == CommonPlaybackState.Playing &&
            playbackInfo.position == 0L
        ) {
            Logger.i { "ignoring duration<=0, position=0" }
            return
        }

        val options = TransformMetadataOptions()
        val (commonPlaybackInfo, ignoreScrobble) =
            transformPlaybackState(sessionTracker.trackInfo, playbackInfo, options)
        sessionTracker.playbackStateChanged(commonPlaybackInfo, ignoreScrobble)
    }

    inner class DesktopSessionTracker(
        key: MediaTrackerKey,
        trackInfo: PlayingTrackInfo,
    ) : SessionTracker(key, trackInfo) {
        override fun love() {
        }

        override fun unlove() {
        }

        override fun skip() {
            PanoNativeComponents.skip(trackInfo.notiKey)
        }

        override fun stop() {
            pause()
            DiscordRpc.clearDiscordActivity(trackInfo.appId)
        }

        override fun onBeforeScrobble() {
            val normalizedUrlHost = trackInfo.normalizedUrlHost

            if (!normalizedUrlHost.isNullOrEmpty() && normalizedUrlHost !in seenHostnames.value) {
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(seenHostnames = it.seenHostnames + normalizedUrlHost) }
                }
            }
        }
    }
}
