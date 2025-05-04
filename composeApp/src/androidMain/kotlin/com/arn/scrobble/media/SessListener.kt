package com.arn.scrobble.media

import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import co.touchlab.kermit.Logger
import com.arn.scrobble.R
import com.arn.scrobble.media.PlayerActions.love
import com.arn.scrobble.media.PlayerActions.skip
import com.arn.scrobble.media.PlayerActions.unlove
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.AndroidStuff.dump
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by arn on 04/07/2017.
 */
class SessListener(
    scope: CoroutineScope,
    scrobbleQueue: ScrobbleQueue,
    private val audioManager: AudioManager,
) : MediaListener(scope, scrobbleQueue), OnActiveSessionsChangedListener {

    private val mainPrefs = PlatformStuff.mainPrefs
    private val controllersMap =
        mutableMapOf<MediaSession.Token, Pair<MediaController, ControllerCallback>>()
    private var platformControllers: List<MediaController>? = null

    private val scrobbleSpotifyRemote = mainPrefs.data.map { it.scrobbleSpotifyRemote }
        .stateIn(
            scope, SharingStarted.Lazily,
            Stuff.mainPrefsInitialValue.scrobbleSpotifyRemote
        )

    init {
        scope.launch {
            combine(
                allowedPackages,
                blockedPackages,
                autoDetectApps,
                scrobblerEnabled,
            ) { allowed, blocked, autoDetect, scrobbleEnabled ->
                arrayOf(allowed, blocked, autoDetect, scrobbleEnabled)
            }
                .collectLatest { (allowed, blocked, autoDetect, scrobbleEnabled) ->

                    onActiveSessionsChanged(platformControllers)
                    val pkgsToKeep = controllersMap.values
                        .map { it.first }
                        .filter { shouldScrobble(it.packageName) }
                        .map { it.packageName }
                        .toSet()
                    removeSessions(controllersMap.keys.toSet(), pkgsToKeep)
                }
        }
    }

    @Synchronized
    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        this.platformControllers = controllers
        Logger.d { "controllers: " + controllers?.joinToString { it.packageName } }

        if (!scrobblerEnabled.value || controllers == null)
            return

        val controllersFiltered = controllers.mapNotNull {
            if (shouldScrobble(it.packageName) && it.sessionToken !in controllersMap)
                MediaController(AndroidStuff.application, it.sessionToken)
            else null
        }

//        val tokens = mutableSetOf<MediaSession.Token>()
        for (controller in controllersFiltered) {
//            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
//                if (controller.sessionToken !in controllersMap) {

            val playingTrackInfo =
                findTrackInfoByKey(controller.packageName + "|" + controller.tagCompat)
                    ?: PlayingTrackInfo(controller.packageName, controller.tagCompat).also {
                        putTrackInfo(controller.packageName + "|" + controller.tagCompat, it)
                    }

            val cb = ControllerCallback(playingTrackInfo, controller.sessionToken)

            controller.registerCallback(cb)

            controller.playbackState?.let { cb.onPlaybackStateChanged(it) }
            controller.metadata?.let { cb.onMetadataChanged(it) }
            controller.extras?.let { cb.onExtrasChanged(it) }
            cb.onAudioInfoChanged(controller.playbackInfo)

            controllersMap[controller.sessionToken] = controller to cb
        }
//            }
//        }
        // Now remove old sessions that are not longer active.
//        removeSessions(tokens)
    }

    @Synchronized
    override fun removeSessions(
        tokensToKeep: Set<*>,
        appIdsToKeep: Set<String>?,
    ) {
        val it = controllersMap.iterator()
        while (it.hasNext()) {
            val (token, pair) = it.next()
            val (controller, callback) = pair
            if (token !in tokensToKeep || appIdsToKeep?.contains(pair.first.packageName) == false) {
                callback.pause()
                controller.unregisterCallback(callback)
                it.remove()
            }
        }
    }

    override fun isMediaPlaying() =
        platformControllers?.any { it.isMediaPlaying() } == true

    fun findControllersByAppId(appId: String) =
        controllersMap.values.filter { it.first.packageName == appId }.map { it.first }

    private fun findCallbackByHash(hash: Int) =
        controllersMap.values.firstOrNull { it.second.trackInfo.lastScrobbleHash == hash }?.second

    fun findControllersByHash(hash: Int) =
        controllersMap.values.filter { it.second.trackInfo.lastScrobbleHash == hash }
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
            AndroidStuff.application.toast(R.string.mute)

            mutedHash = hash
            callback.setMuted(true)
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
        AndroidStuff.application.toast(R.string.skip)
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


    override fun hasOtherPlayingControllers(thisTrackInfo: PlayingTrackInfo): Boolean {
        return controllersMap.values.any { (controller, cb) ->
            controller.packageName == thisTrackInfo.appId
                    && controller.tagCompat != thisTrackInfo.sessionId &&
                    controller.isMediaPlaying()
//                        && !cb.trackInfo.hasBlockedTag
        }
    }

    override fun shouldIgnoreOrigArtist(trackInfo: PlayingTrackInfo): Boolean {
        return if (
            trackInfo.appId in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK && trackInfo.album.isNotEmpty() ||
            trackInfo.appId == Stuff.PACKAGE_YOUTUBE_TV && trackInfo.album.isNotEmpty() ||
            trackInfo.appId == Stuff.PACKAGE_YMUSIC &&
            trackInfo.album.replace("YMusic", "").isNotEmpty()
        )
            false
        else (trackInfo.appId in Stuff.IGNORE_ARTIST_META &&
                !trackInfo.artist.endsWith("- Topic"))
//                (trackInfo.appId in Stuff.browserPackages &&
//                        trackInfo.artist.isUrlOrDomain())
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

            if (metadata.getLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT) != 0L) {
                sessionTracker.resetMeta()
                return
            }

//            if (PlatformStuff.isDebug)
//                metadata.dump()

            val (metadataInfo, canDoFallbackScrobble) = transformMediaMetadata(trackInfo, metadata)

            Logger.i {
                "onMetadataChanged ${metadataInfo.artist} (${metadataInfo.album_artist}) [${metadataInfo.album}] ~ ${metadataInfo.title} " +
                        "duration=${metadataInfo.duration} lastState=${sessionTracker.lastPlaybackState}, isRemotePlayback=$isRemotePlayback cb=${this.hashCode()}}"
            }



            sessionTracker.metadataChanged(metadataInfo, canDoFallbackScrobble)
        }

        @Synchronized
        override fun onPlaybackStateChanged(state: PlaybackState?) {

            state ?: return

            val playingState = state.state
            val pos = state.position // can be -1

            Logger.i { "onPlaybackStateChanged=$playingState laststate=${sessionTracker.lastPlaybackState} pos=$pos cb=${this@ControllerCallback.hashCode()} sl=${this@SessListener.hashCode()}" }

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
}
