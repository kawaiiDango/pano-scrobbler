package com.arn.scrobble.media

import co.touchlab.kermit.Logger
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class DesktopMediaListener(
    scope: CoroutineScope,
    scrobbleQueue: ScrobbleQueue,
) : MediaListener(scope, scrobbleQueue) {

    private val sessionTrackersMap = mutableMapOf<String, SessionTracker>()
    private var sessionInfos: List<SessionInfo> = emptyList()

    private val seenApps =
        PlatformStuff.mainPrefs.data.mapLatest { it.seenApps }
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                Stuff.mainPrefsInitialValue.seenApps
            )


    fun start() {
        PanoNativeComponents.startListeningMediaInThread()

        scope.launch {
            delay(1500)

            combine(scrobblerEnabled, allowedPackages) { scrobblerEnabled, allowedPackages ->
                if (!scrobblerEnabled) {
                    emptySet()
                } else
                    allowedPackages
            }.collectLatest { allowed ->
                val keysToKeep = sessionTrackersMap.keys
                    .filter { shouldScrobble(it.substringBefore('|')) }
                    .toSet()

                val allowedApps = allowed.filter { shouldScrobble(it) }

                PanoNativeComponents.setAllowedAppIds(allowedApps.toTypedArray())
                removeSessions(keysToKeep)
                platformActiveSessionsChanged(sessionInfos)
            }
        }
    }

    fun platformActiveSessionsChanged(sessions: List<SessionInfo>) {
        Logger.d { "controllers: " + sessions.joinToString { it.app_id } }

        if (!scrobblerEnabled.value)
            return

        val unseenAppItems = sessions
            .filter { it.app_id !in seenApps.value }
            .map { it.app_id to it.app_name }

        if (unseenAppItems.isNotEmpty()) {
            scope.launch {
                PlatformStuff.mainPrefs.updateData { it.copy(seenApps = it.seenApps + unseenAppItems) }
            }

            unseenAppItems.forEach { (appId, friendlyLabel) ->
                PanoNotifications.notifyAppDetected(appId, friendlyLabel)
            }
        }


        val sessionsFiltered = sessions.filter {
            shouldScrobble(it.app_id) && it.app_id !in sessionTrackersMap
        }

//        val tokens = mutableSetOf<MediaSession.Token>()
        for (session in sessionsFiltered) {
//            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
//                if (controller.sessionToken !in controllersMap) {

            val playingTrackInfo =
                findTrackInfoByKey(session.app_id)
                // there is no concept of session tag on desktop platforms
                    ?: PlayingTrackInfo(session.app_id, "").also {
                        putTrackInfo(session.app_id, it)
                    }

            sessionTrackersMap[session.app_id] = SessionTracker(playingTrackInfo)
        }
//            }
//        }
        // Now remove old sessions that are not longer active.
        removeSessions(
            sessions.map { it.app_id }.toSet(),
        )

        this.sessionInfos = sessions
    }

    @Synchronized
    override fun removeSessions(
        tokensToKeep: Set<*>,
        appIdsToKeep: Set<String>?,
    ) {
        val it = sessionTrackersMap.iterator()
        while (it.hasNext()) {
            val (sessionKey, sessionTracker) = it.next()
            if (sessionKey !in tokensToKeep || appIdsToKeep?.contains(sessionKey.substringBefore('|')) == false) {
                sessionTracker.pause()
                it.remove()
            }
        }
    }

    override fun isMediaPlaying() =
        sessionTrackersMap.values.any { it.isMediaPlaying() }

    fun findSessionInfoByAppId(appId: String) =
        sessionTrackersMap.keys.find { it.startsWith("$appId|") }

    private fun findSessionTrackerByHash(hash: Int) =
        sessionTrackersMap.values.firstOrNull { it.trackInfo.lastScrobbleHash == hash }

    fun findControllersByHash(hash: Int) =
        sessionTrackersMap.values.filter { it.trackInfo.lastScrobbleHash == hash }

    override fun mute(hash: Int) {
        // if pano didnt mute this, dont unmute later
//        if (mutedHash == null && audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
//            return

        val callback = findSessionTrackerByHash(hash)
        if (callback != null) {
            PanoNativeComponents.mute(callback.trackInfo.appId)
            Logger.i { "mute: done" }

            mutedHash = hash
            callback.isMuted = true
        }
    }

    override fun unmute(clearMutedHash: Boolean) {
        if (mutedHash != null) {
            Logger.i { "unmute: done" }

            val callback = findSessionTrackerByHash(mutedHash!!)
            callback?.trackInfo?.appId?.let { PanoNativeComponents.unmute(it) }

            callback?.isMuted = false

            if (clearMutedHash)
                mutedHash = null

        }
    }

    override fun skip(hash: Int) {
        val appId = findTrackInfoByHash(hash)?.appId ?: return
        PanoNativeComponents.skip(appId)
    }

    override fun love(hash: Int) {}

    override fun unlove(hash: Int) {}

    private fun SessionTracker.isMediaPlaying() =
        trackInfo.isPlaying && trackInfo.title.isNotBlank() && trackInfo.artist.isNotBlank()


    override fun hasOtherPlayingControllers(appId: String, sessionId: String): Boolean {
        return sessionTrackersMap.entries.any { (sessionKey, sessionTracker) ->
            sessionKey.startsWith("$appId|") &&
                    sessionKey != "$appId|$sessionId" &&
                    sessionTracker.isMediaPlaying()
        }
    }

    fun platformMetadataChanged(metadata: MetadataInfo) {
        Logger.i { "metadata: $metadata" }

        val sessionTracker =
            sessionTrackersMap[metadata.app_id] ?: return

//        Info: (scrobbler) metadata: MetadataInfo(app_id=Spotify.exe, title=Advertisement, artist=Spotify, album=, album_artist=Spotify, track_number=0, duration=25417)
        if (metadata.artist == "Spotify" && metadata.album_artist == "Spotify" && metadata.title == "Advertisement" && metadata.album.isEmpty()) {
            sessionTracker.resetMeta()
            return
        }

        val metadata = transformMediaMetadata(
            sessionTracker.trackInfo,
            metadata
        )

        sessionTracker.metadataChanged(metadata)
    }

    fun platformPlaybackStateChanged(playbackInfo: PlaybackInfo) {
        Logger.i { "playbackInfo: $playbackInfo" }

        val sessionTracker =
            sessionTrackersMap[playbackInfo.app_id] ?: return

        val options = TransformMetadataOptions()
        val (commonPlaybackInfo, ignoreScrobble) =
            transformPlaybackState(sessionTracker.trackInfo, playbackInfo, options)
        sessionTracker.playbackStateChanged(commonPlaybackInfo, ignoreScrobble)
    }
}
