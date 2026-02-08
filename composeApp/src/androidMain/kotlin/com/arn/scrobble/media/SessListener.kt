package com.arn.scrobble.media

import android.annotation.SuppressLint
import android.content.ComponentName
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.media.PlayerActions.love
import com.arn.scrobble.media.PlayerActions.unlove
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.AndroidStuff.dump
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    private var platformControllers = emptyList<MediaController>()

    private val scrobbleSpotifyRemote =
        mainPrefs.data.stateInWithCache(scope) { it.scrobbleSpotifyRemoteP }

    private val autoDetectApps =
        mainPrefs.data.stateInWithCache(scope) { it.autoDetectAppsP }

    private val blockedPackages =
        mainPrefs.data.stateInWithCache(scope) { it.blockedPackages }

    init {
        scope.launch {
            combine(
                allowedPackages,
                blockedPackages,
                autoDetectApps,
                scrobblerEnabled,
            ) { allowed, blocked, autoDetect, scrobbleEnabled ->
                onActiveSessionsChanged(platformControllers)
                val tokensToKeep = sessionTrackers
                    .filter { (k, v) ->
                        shouldScrobble(v.trackInfo.appId)
                    }
                    .keys
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
        val should = scrobblerEnabled.value &&
                (rawAppId in allowedPackages.value ||
                        (autoDetectApps.value && rawAppId !in blockedPackages.value))
        return should
    }

    fun isAppAllowListed(appId: String): Boolean {
        return appId in allowedPackages.value
    }

    @Synchronized
    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {

        Logger.i {
            "controllers: " + controllers?.joinToString {
                "${it.packageName}|${it.tagCompat}@" + it.sessionToken.hashCode().toHexString()
            }
        }

        if (controllers == null)
            return

        this.platformControllers = controllers

        if (!scrobblerEnabled.value)
            return

        val controllersFiltered = controllers.mapNotNull {
            if (shouldScrobble(it.packageName) &&
                it.sessionToken !in sessionTrackers
            )
                MediaController(AndroidStuff.applicationContext, it.sessionToken)
            else null
        }

//        val tokens = mutableSetOf<MediaSession.Token>()
        for (controller in controllersFiltered) {
//            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
//                if (controller.sessionToken !in controllersMap) {

            val appId = controller.packageName
            val sessionId = controller.tagCompat

            val uniqueId = "$appId|$sessionId@" + controller.sessionToken.hashCode().toHexString()

            val playingTrackInfo = createTrackInfo(controller.packageName, uniqueId)

            val sessionTracker = AndroidSessionTracker(controller, playingTrackInfo)

            controller.registerCallback(sessionTracker.callback)

            sessionTracker.callback.onPlaybackStateChanged(controller.playbackState)
            sessionTracker.callback.onMetadataChanged(controller.metadata)

            if (BuildKonfig.DEBUG) {
                sessionTracker.callback.onExtrasChanged(controller.extras)
                sessionTracker.callback.onAudioInfoChanged(controller.playbackInfo)
            }

            sessionTrackers[controller.sessionToken] = sessionTracker
        }

        // Now remove old sessions that are no longer active.
//        removeSessions(tokens)
    }

    override fun isMediaPlaying() = platformControllers.any { it.isMediaPlaying() }

    private val MediaController.tagCompat
        @SuppressLint("NewApi")
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> tag
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 -> tag
            else -> "greylist-max-o"
        }

    override fun mute(hash: Int) {
        // if pano didnt mute this, dont unmute later
        if (mutedHash == null && audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
            return

        val tracker = findTrackerByHash(hash)
        if (tracker != null) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
            Logger.i { "mute: done" }

            mutedHash = hash
            tracker.isMuted = true

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

            val tracker = findTrackerByHash(mutedHash!!)
            tracker?.isMuted = false

            if (clearMutedHash)
                mutedHash = null
        }
    }


    //        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
    // MediaController.getTag() exists on Android 10 and lower but is marked as @hide

    private fun MediaController.isMediaPlaying() =
        playbackState?.state in arrayOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING
        ) && !metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrEmpty()

    inner class AndroidSessionTracker(
        val controller: MediaController,
        trackInfo: PlayingTrackInfo,
    ) : SessionTracker(trackInfo) {

        override fun skip() {
            controller.transportControls.skipToNext()

            scope.launch(Dispatchers.Main) {
                AndroidStuff.applicationContext.toast(getString(Res.string.skip))
            }
        }

        override fun love() {
            controller.love()
        }

        override fun unlove() {
            controller.unlove()
        }

        override fun stop() {
            pause()
            controller.unregisterCallback(callback)
        }

        val callback = object : MediaController.Callback() {

            init {
                controller.registerCallback(this)
            }

            @Synchronized
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                metadata ?: return


//            if (BuildKonfig.DEBUG)
//                metadata.dump()

                val (metadataInfo, ignoreScrobble) = transformMediaMetadata(trackInfo, metadata)

                metadataChanged(metadataInfo, ignoreScrobble)
            }

            @Synchronized
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                state ?: return

                val options = TransformMetadataOptions(
                    scrobbleSpotifyRemote = scrobbleSpotifyRemote.value
                )
                val (playbackInfo, ignoreScrobble) = transformPlaybackState(
                    trackInfo,
                    state,
                    options
                )

                playbackStateChanged(playbackInfo, ignoreScrobble)
            }

            override fun onSessionDestroyed() {
                Logger.d { "onSessionDestroyed ${trackInfo.appId}" }
                stop()
            }

            override fun onExtrasChanged(extras: Bundle?) {
                Logger.d { "extras updated ${trackInfo.appId}: ${extras.dump()}" }
            }

            override fun onSessionEvent(event: String, extras: Bundle?) {
                Logger.d { "onSessionEvent ${trackInfo.appId}: $event ${extras.dump()}" }
            }

            override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
                Logger.d { "audioinfo updated ${trackInfo.appId}: $info" }
            }
        }
    }
}
