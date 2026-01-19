package com.arn.scrobble.media

import co.touchlab.kermit.Logger
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.discordrpc.DiscordRpc
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch


class DesktopMediaListener(
    scope: CoroutineScope,
    scrobbleQueue: ScrobbleQueue,
) : MediaListener(scope, scrobbleQueue) {

    override val notifyTimelineUpdates = true

    private val sessionTrackersMap = mutableMapOf<String, SessionTracker>()
    private var sessionInfos: List<SessionInfo> = emptyList()

    private val seenApps =
        PlatformStuff.mainPrefs.data.stateInWithCache(scope) { it.seenApps }

    fun start() {
        PanoNativeComponents.startListeningMediaInThread()

        scope.launch {
            delay(1500)

            combine(scrobblerEnabled, allowedPackages) { scrobblerEnabled, allowedPackages ->
                if (!scrobblerEnabled) {
                    hashSetOf()
                } else
                    allowedPackages.toHashSet()
            }
                .distinctUntilChanged()
                .drop(1) // drop initial value
                .collectLatest {
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
        Logger.d { "controllers: " + sessions.joinToString { it.rawAppId } }

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
        val newSessions = shouldScrobbleSessions.filter { it.rawAppId !in sessionTrackersMap }

//        val tokens = mutableSetOf<MediaSession.Token>()
        for (session in newSessions) {
//            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
//                if (controller.sessionToken !in controllersMap) {

            val normalizedAppId = DesktopStuff.normalizeAppId(session.rawAppId)
            val playingTrackInfo =
                findTrackInfoByKey(session.rawAppId)
                // there is no concept of session tag on desktop platforms
                    ?: PlayingTrackInfo(normalizedAppId, session.rawAppId).also {
                        putTrackInfo(session.rawAppId, it)
                    }

            sessionTrackersMap[playingTrackInfo.uniqueId] = SessionTracker(playingTrackInfo)
        }
//            }
        // Now remove old sessions that are no longer active.
        removeSessions(
            shouldScrobbleSessions.map { it.rawAppId }.toSet(),
        )

    }

    @Synchronized
    override fun removeSessions(
        tokensToKeep: Set<*>,
    ) {
        val it = sessionTrackersMap.iterator()
        while (it.hasNext()) {
            val (sessionKey, sessionTracker) = it.next()
            if (sessionKey !in tokensToKeep) {
                sessionTracker.pause()
                it.remove()
                DiscordRpc.clearDiscordActivity(sessionTracker.trackInfo.appId)
            }
        }
    }

    override fun isMediaPlaying() =
        sessionTrackersMap.values.any { it.isMediaPlaying() }

    private fun findSessionTrackerByHash(hash: Int) =
        sessionTrackersMap.values.firstOrNull { it.trackInfo.hash == hash }

    override fun mute(hash: Int) {
        // if pano didnt mute this, dont unmute later
//        if (mutedHash == null && audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
//            return

        val callback = findSessionTrackerByHash(hash)
        if (callback != null) {
            PanoNativeComponents.mute(callback.trackInfo.uniqueId)
            Logger.i { "mute: done" }

            mutedHash = hash
            callback.isMuted = true
        }
    }

    override fun unmute(clearMutedHash: Boolean) {
        if (mutedHash != null) {
            Logger.i { "unmute: done" }

            val callback = findSessionTrackerByHash(mutedHash!!)
            callback?.trackInfo?.uniqueId?.let { PanoNativeComponents.unmute(it) }

            callback?.isMuted = false

            if (clearMutedHash)
                mutedHash = null

        }
    }

    override fun skip(hash: Int) {
        findTrackInfoByHash(hash)?.uniqueId?.let {
            PanoNativeComponents.skip(it)
        }
    }

    override fun love(hash: Int) {}

    override fun unlove(hash: Int) {}

    private fun SessionTracker.isMediaPlaying() =
        trackInfo.isPlaying && trackInfo.title.isNotBlank() && trackInfo.artist.isNotBlank()


    override fun hasOtherPlayingControllers(appId: String): Boolean {
        return sessionTrackersMap.entries.count { (sessionKey, sessionTracker) ->
            sessionKey.startsWith("$appId|") && sessionTracker.isMediaPlaying()
        } > 1
    }

    fun platformMetadataChanged(uniqueAppId: String, metadata: MetadataInfo) {

        val sessionTracker =
            sessionTrackersMap[uniqueAppId] ?: return

//        Info: (scrobbler) metadata: MetadataInfo(app_id=Spotify.exe, title=Advertisement, artist=Spotify, album=, album_artist=Spotify, track_number=0, duration=25417)
        if (metadata.artist == "Spotify" && metadata.albumArtist == "Spotify" && metadata.title == "Advertisement" && metadata.album.isEmpty()) {
            sessionTracker.trackInfo.resetMeta()
            return
        }

        val (metadata, extras) = transformMediaMetadata(
            sessionTracker.trackInfo,
            metadata
        )

        sessionTracker.metadataChanged(metadata, extras)
    }

    fun platformPlaybackStateChanged(uniqueAppId: String, playbackInfo: PlaybackInfo) {
        val sessionTracker = sessionTrackersMap[uniqueAppId] ?: return

        val options = TransformMetadataOptions()
        val (commonPlaybackInfo, ignoreScrobble) =
            transformPlaybackState(sessionTracker.trackInfo, playbackInfo, options)
        sessionTracker.playbackStateChanged(commonPlaybackInfo, ignoreScrobble)
    }
}
