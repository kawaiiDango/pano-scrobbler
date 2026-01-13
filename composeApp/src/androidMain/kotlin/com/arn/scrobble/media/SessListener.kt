package com.arn.scrobble.media

import android.content.ComponentName
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import co.touchlab.kermit.Logger
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.media.PlayerActions.love
import com.arn.scrobble.media.PlayerActions.skip
import com.arn.scrobble.media.PlayerActions.unlove
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.AndroidStuff.dump
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.mute
import pano_scrobbler.composeapp.generated.resources.skip

class SessListener(
    scope: CoroutineScope,
    scrobbleQueue: ScrobbleQueue,
    private val audioManager: AudioManager,
) : MediaListener(scope, scrobbleQueue), OnActiveSessionsChangedListener {

    override val notifyTimelineUpdates = false
    private val mainPrefs = PlatformStuff.mainPrefs
    private val controllersMap =
        mutableMapOf<MediaSession.Token, Pair<MediaController, ControllerCallback>>()
    private var platformControllers: List<MediaController>? = null

    private val scrobbleSpotifyRemote = mainPrefs.data.map { it.scrobbleSpotifyRemoteP }
        .stateIn(
            scope, SharingStarted.Eagerly,
            Stuff.mainPrefsInitialValue.scrobbleSpotifyRemoteP
        )

    private val autoDetectApps =
        mainPrefs.data.map { it.autoDetectAppsP }
            .stateIn(scope, SharingStarted.Eagerly, Stuff.mainPrefsInitialValue.autoDetectAppsP)

    init {
        scope.launch {
            combine(
                allowedPackages,
                blockedPackages,
                autoDetectApps,
                scrobblerEnabled,
            ) { allowed, blocked, autoDetect, scrobbleEnabled ->
                onActiveSessionsChanged(platformControllers)
                val tokensToKeep = controllersMap.values
                    .map { it.first }
                    .filter { shouldScrobble(it.packageName) }
                    .map { it.sessionToken }
                    .toSet()
                removeSessions(tokensToKeep)
            }.collect()
        }

        scope.launch {
            scrobblerEnabled.collectLatest {
                try {
                    TileService.requestListeningState(
                        AndroidStuff.applicationContext,
                        ComponentName(AndroidStuff.applicationContext, MasterSwitchQS::class.java)
                    )
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to update QS tile state" }
                }
            }
        }
    }

    override fun shouldScrobble(rawAppId: String): Boolean {
        val should = scrobblerEnabled.value && rawAppId in allowedPackages.value
        return should
    }

    @Synchronized
    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        this.platformControllers = controllers
        Logger.d { "controllers: " + controllers?.joinToString { it.packageName } }

        if (!scrobblerEnabled.value || controllers == null)
            return

        val controllersFiltered = controllers.mapNotNull {
            if (shouldScrobble(it.packageName) && it.sessionToken !in controllersMap)
                MediaController(AndroidStuff.applicationContext, it.sessionToken)
            else null
        }

//        val tokens = mutableSetOf<MediaSession.Token>()
        for (controller in controllersFiltered) {
//            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
//                if (controller.sessionToken !in controllersMap) {

            val sessionId =
                if (controllersFiltered.count { it.packageName == controller.packageName && it.tagCompat == controller.tagCompat } > 1)
                // if there are multiple sessions with same appId and tag, append session token hash
                    controller.tagCompat + "@" + controller.sessionToken.hashCode().toHexString()
                else
                    controller.tagCompat

            val uniqueId = controller.packageName + "|" + sessionId

            val playingTrackInfo = findTrackInfoByKey(uniqueId)
                ?: PlayingTrackInfo(controller.packageName, uniqueId).also {
                    putTrackInfo(uniqueId, it)
                }

            val cb = ControllerCallback(playingTrackInfo, controller.sessionToken)

            controller.registerCallback(cb)

            controller.playbackState?.let { cb.onPlaybackStateChanged(it) }
            controller.metadata?.let { cb.onMetadataChanged(it) }
            controller.extras?.let { cb.onExtrasChanged(it) }
            cb.onAudioInfoChanged(controller.playbackInfo)

            controllersMap[controller.sessionToken] = controller to cb
        }

        // Now remove old sessions that are no longer active.
//        removeSessions(tokens)
    }

    @Synchronized
    override fun removeSessions(
        tokensToKeep: Set<*>,
    ) {
        val it = controllersMap.iterator()
        while (it.hasNext()) {
            val (token, pair) = it.next()
            val (controller, callback) = pair
            if (token !in tokensToKeep) {
                callback.pause()
                controller.unregisterCallback(callback)
                it.remove()
            }
        }
    }

    override fun isMediaPlaying() =
        platformControllers?.any { it.isMediaPlaying() } == true

    private fun findCallbackByHash(hash: Int) =
        controllersMap.values.firstOrNull { it.second.trackInfo.hash == hash }?.second

    fun findControllersByHash(hash: Int) =
        controllersMap.values.filter { it.second.trackInfo.hash == hash }
            .map { it.first }

    private val MediaController.tagCompat
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> tag
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 -> tag
            else -> "greylist-max-o"
        }

    override fun mute(hash: Int) {
        // if pano didnt mute this, dont unmute later
        if (mutedHash == null && audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
            return

        val callback = findCallbackByHash(hash)
        if (callback != null) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
            Logger.i { "mute: done" }

            mutedHash = hash
            callback.setMuted(true)

            scope.launch(Dispatchers.Main) {
                AndroidStuff.applicationContext.toast(getString(Res.string.mute))
            }
        }
    }

    override fun unmute(clearMutedHash: Boolean) {
        if (mutedHash != null) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE,
                0
            )
            Logger.i { "unmute: done" }

            val callback = findCallbackByHash(mutedHash!!)
            callback?.setMuted(false)

            if (clearMutedHash)
                mutedHash = null
        }
    }

    override fun skip(hash: Int) {
        val controllers = findControllersByHash(hash)
        controllers.skip()

        scope.launch(Dispatchers.Main) {
            AndroidStuff.applicationContext.toast(getString(Res.string.skip))
        }
    }


    override fun love(hash: Int) {
        val controllers = findControllersByHash(hash)
        controllers.love()
    }

    override fun unlove(hash: Int) {
        val controllers = findControllersByHash(hash)
        controllers.unlove()
    }

    //        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
    // MediaController.getTag() exists on Android 10 and lower but is marked as @hide

    private fun MediaController.isMediaPlaying() =
        playbackState?.state in arrayOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING
        ) && !metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrEmpty()


    override fun hasOtherPlayingControllers(appId: String): Boolean {
        return controllersMap.values.count { (controller, cb) ->
            controller.packageName == appId && controller.isMediaPlaying()
//                        && !cb.trackInfo.hasBlockedTag
        } > 1
    }

    inner class ControllerCallback(
        val trackInfo: PlayingTrackInfo,
        private val token: MediaSession.Token,
    ) : MediaController.Callback() {
        private val sessionTracker = SessionTracker(trackInfo)
        private var isRemotePlayback = false

        @Synchronized
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return

            if (metadata.getLong(METADATA_KEY_ADVERTISEMENT) != 0L) {
                trackInfo.resetMeta()
                return
            }

//            if (BuildKonfig.DEBUG)
//                metadata.dump()

            val (metadataInfo, extras) = transformMediaMetadata(trackInfo, metadata)

            sessionTracker.metadataChanged(metadataInfo, extras)
        }

        @Synchronized
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state ?: return

            val options = TransformMetadataOptions(
                scrobbleSpotifyRemote = scrobbleSpotifyRemote.value
            )
            val (playbackInfo, ignoreScrobble) = transformPlaybackState(trackInfo, state, options)

            sessionTracker.playbackStateChanged(playbackInfo, ignoreScrobble)
        }

        override fun onSessionDestroyed() {
            Logger.d { "onSessionDestroyed ${trackInfo.appId}" }
            sessionTracker.pause()
            synchronized(this@SessListener) {
                controllersMap.remove(token)
                    ?.first
                    ?.unregisterCallback(this)
            }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            Logger.d { "extras updated ${trackInfo.appId}: ${extras.dump()}" }
        }

        override fun onSessionEvent(event: String, extras: Bundle?) {
            Logger.d { "onSessionEvent ${trackInfo.appId}: $event ${extras.dump()}" }
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
            Logger.d { "audioinfo updated ${trackInfo.appId}: $info" }

            isRemotePlayback =
                info.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE
        }

        fun setMuted(muted: Boolean) {
            sessionTracker.isMuted = muted
        }

        fun pause() {
            sessionTracker.pause()
        }
    }

    companion object {
        private const val METADATA_KEY_ADVERTISEMENT = "android.media.metadata.ADVERTISEMENT"
    }
}
