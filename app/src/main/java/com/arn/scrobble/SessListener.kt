package com.arn.scrobble

import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaController.Callback
import android.media.session.MediaSession
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import com.arn.scrobble.Stuff.dump
import com.arn.scrobble.pref.MainPrefs
import java.util.*

/**
 * Created by arn on 04/07/2017.
 */

class SessListener(
    private val prefs: MainPrefs,
    private val handler: NLService.ScrobbleHandler,
    private val audioManager: AudioManager,
) : OnActiveSessionsChangedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val controllersMap =
        mutableMapOf<MediaSession.Token, Pair<MediaController, ControllerCallback>>()
    private var controllers: List<MediaController>? = null

    private val blockedPackages = mutableSetOf<String>()
    private val allowedPackages = mutableSetOf<String>()
    private val loggedIn
        get() = prefs.lastfmSessKey != null
    val packageTrackMap = mutableMapOf<String, PlayingTrackInfo>()
    private var mutedHash: Int? = null

    init {
        prefs.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        allowedPackages.addAll(prefs.allowedPackages)
        blockedPackages.addAll(prefs.blockedPackages)
    }

    // this list of controllers is unreliable esp. with yt and yt music
    // it may be empty even if there are active sessions
    @Synchronized
    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        this.controllers = controllers
        Stuff.log("controllers: " + controllers?.joinToString { it.packageName })

        if (!prefs.scrobblerEnabled || controllers == null)
            return
//        val tokens = mutableSetOf<MediaSession.Token>()
        for (controller in controllers) {
            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
                if (controller.sessionToken !in controllersMap) {
                    var numControllersForPackage = 0
                    var hasOtherTokensForPackage = false
                    controllersMap.forEach { (token, pair) ->
                        if (pair.first.packageName == controller.packageName) {
                            numControllersForPackage++
                            if (token != controller.sessionToken)
                                hasOtherTokensForPackage = true
                        }
                    }
                    val hasMultipleSessions =
                        numControllersForPackage > 1 || hasOtherTokensForPackage
                    var playingTrackInfo = packageTrackMap[controller.packageName]
                    if (playingTrackInfo == null || hasMultipleSessions) {
                        playingTrackInfo = PlayingTrackInfo(controller.packageName)
                        packageTrackMap[controller.packageName] = playingTrackInfo
                    }
                    val cb =
                        ControllerCallback(
                            playingTrackInfo,
                            controller.sessionToken
                        )
                    controller.registerCallback(cb)

                    //Medoly needs this
                    controller.playbackState?.let { cb.onPlaybackStateChanged(it) }
                    controller.metadata?.let { cb.onMetadataChanged(it) }
                    controller.playbackInfo?.let { cb.onAudioInfoChanged(it) }

                    controllersMap[controller.sessionToken] = controller to cb
                }
            }
        }
        // Now remove old sessions that are not longer active.
//        removeSessions(tokens)
    }

    fun findControllersByPackage(packageName: String) =
        controllersMap.values.filter { it.first.packageName == packageName }.map { it.first }

    private fun findCallbackByHash(hash: Int) =
        controllersMap.values.firstOrNull { it.second.playingTrackInfo.lastScrobbleHash == hash }?.second

    fun findControllersByHash(hash: Int) =
        controllersMap.values.filter { it.second.playingTrackInfo.lastScrobbleHash == hash }
            .map { it.first }

    fun findTrackInfoByHash(hash: Int) =
        packageTrackMap.values.find { it.hash == hash }

    fun mute(hash: Int) {
        // if pano didnt mute this, dont unmute later
        // lollipop requires reflection, and i dont want to use that
        if (mutedHash == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager.isStreamMute(
                AudioManager.STREAM_MUSIC
            )
        )
            return

        val callback = findCallbackByHash(hash)
        if (callback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    0
                )
                Stuff.log("mute: done")

            } else {
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
            }

            mutedHash = hash
            callback.isMuted = true
        }
    }

    @Synchronized
    fun removeSessions(
        tokensToKeep: Set<MediaSession.Token>,
        packageNamesToKeep: Set<String>? = null
    ) {
        val it = controllersMap.iterator()
        while (it.hasNext()) {
            val (token, pair) = it.next()
            val (controller, callback) = pair
            if (token !in tokensToKeep || packageNamesToKeep?.contains(pair.first.packageName) == false) {
                callback.stop()
                controller.unregisterCallback(callback)
                it.remove()
            }
        }
    }

    fun unregisterPrefsChangeListener() {
        prefs.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    inner class ControllerCallback(
        val playingTrackInfo: PlayingTrackInfo,
        private val token: MediaSession.Token
    ) : Callback() {

        private var lastState = -1
        private var isRemotePlayback = false
        var isMuted = false

        private val syntheticStateHandler = Handler(handler.looper)

        fun scrobble() {
//            if (BuildConfig.DEBUG && isRemotePlayback)
//                return

            Stuff.log("playing: timePlayed=${playingTrackInfo.timePlayed} ${playingTrackInfo.title}")

            scheduleSyntheticStateIfNeeded()

            playingTrackInfo.playStartTime = System.currentTimeMillis()
            handler.remove(playingTrackInfo.lastScrobbleHash, false)

            handler.nowPlaying(playingTrackInfo)

            playingTrackInfo.lastScrobbleHash = playingTrackInfo.hash
            playingTrackInfo.lastSubmittedScrobbleHash = 0

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)
        }

        private fun scheduleSyntheticStateIfNeeded() {
            if (playingTrackInfo.packageName in Stuff.needSyntheticStates && playingTrackInfo.durationMillis > 0) {
                syntheticStateHandler.removeCallbacksAndMessages(null)
                syntheticStateHandler.postDelayed({
                    onPlaybackStateChanged(
                        PlaybackState.Builder()
                            .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                            .setErrorMessage("synthetic")
                            .build()
                    )
                }, playingTrackInfo.durationMillis)
            }
        }

        @Synchronized
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return

            var albumArtist =
                metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim() ?: ""
            var artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim()
                ?: albumArtist // do not scrobble empty artists, ads will get scrobbled
            var album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim() ?: ""
            var title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: ""
//            val genre = metadata?.getString(MediaMetadata.METADATA_KEY_GENRE)?.trim() ?: ""
            // The genre field is not used by google podcasts and podcast addict
            var durationMillis = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            if (durationMillis < -1)
                durationMillis = -1

            when (playingTrackInfo.packageName) {
                Stuff.PACKAGE_XIAMI -> {
                    artist = artist.replace(";", "; ")
                }
                Stuff.PACKAGE_PANDORA -> {
                    artist = artist.replace("^Ofln - ".toRegex(), "")
                    albumArtist = ""
                }
                Stuff.PACKAGE_PODCAST_ADDICT -> {
                    val idx = artist.lastIndexOf(" • ")
                    if (idx != -1)
                        artist = artist.substring(0, idx)
                }
                Stuff.PACKAGE_SONOS,
                Stuff.PACKAGE_SONOS2 -> {
                    metadata.getString(MediaMetadata.METADATA_KEY_COMPOSER)?.let {
                        artist = it
                        albumArtist = ""
                    }
                }
                Stuff.PACKAGE_DIFM -> {
                    val extra = " - $album"
                    if (artist.endsWith(extra))
                        artist = artist.substring(0, artist.length - extra.length)
                    title = album
                    album = ""
                    albumArtist = ""
                }
                Stuff.PACKAGE_HUAWEI_MUSIC -> {
                    if (Build.MANUFACTURER.lowercase(Locale.ENGLISH) == Stuff.MANUFACTURER_HUAWEI) {
                        // Extra check for the manufacturer, because 'com.android.mediacenter' could match other music players.
                        val extra = " - $album"
                        if (artist.endsWith(extra))
                            artist = artist.substring(0, artist.length - extra.length)
                        albumArtist = ""
                    }
                }
                Stuff.PACKAGE_NICOBOX -> {
                    artist = "Unknown"
                }
                Stuff.PACKAGE_SPOTIFY -> {
                    // ads look like these:
                    // e.g. GCTC () [] ~ Free Spring Tuition
                    // Beef Its Whats For Dinner () [] ~
                    // Kroger () [] ~ Advertisement
                    if (albumArtist.isEmpty() && album.isEmpty() && title.isNotEmpty()) {
                        resetMeta()
                        return
                    }

                    if (BuildConfig.DEBUG) {
                        val circles = prefs.touhouCircles.split('\n')
                        if (artist in circles) {
                            val titleArtist = title.split(" feat\\.? ".toRegex())
                            if (titleArtist.size == 2) {
                                title = titleArtist[0]
                                artist = titleArtist[1]
                            }
                        }
                    }
                }
            }

            val sameAsOld =
                artist == playingTrackInfo.artist && title == playingTrackInfo.title && album == playingTrackInfo.album
                        && albumArtist == playingTrackInfo.albumArtist
            val onlyDurationUpdated = sameAsOld && durationMillis != playingTrackInfo.durationMillis

            Stuff.log(
                "onMetadataChanged $artist ($albumArtist) [$album] ~ $title, sameAsOld=$sameAsOld, " +
                        "duration=$durationMillis lastState=$lastState, package=${playingTrackInfo.packageName} isRemotePlayback=$isRemotePlayback cb=${this.hashCode()} sl=${this@SessListener.hashCode()}"
            )

//            metadata.dump()

            if (artist == "" || title == "")
                return

            if (!sameAsOld || onlyDurationUpdated) {
                playingTrackInfo.artist = artist
                playingTrackInfo.album = album
                playingTrackInfo.title = title
                playingTrackInfo.albumArtist = albumArtist
                playingTrackInfo.durationMillis = durationMillis
                playingTrackInfo.hash =
                    Stuff.genHashCode(artist, album, title, playingTrackInfo.packageName)

                if (mutedHash != null && playingTrackInfo.hash != mutedHash && lastState == PlaybackState.STATE_PLAYING)
                    unmute(clearMutedHash = isMuted)

                // scrobbled when ad was playing
//                if (onlyDurationUpdated && packageName in arrayOf(Stuff.PACKAGE_YOUTUBE_MUSIC)) {
//                    scheduleSyntheticStateIfNeeded()
//                    if (!handler.hasMessages(currHash))
//                        return
//                }

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if ((!handler.has(playingTrackInfo.hash) || onlyDurationUpdated) &&
                    lastState == PlaybackState.STATE_PLAYING
                ) {
                    playingTrackInfo.timePlayed = 0
                    scrobble()
                }
            }
        }

        @Synchronized
        override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
            playbackState ?: return

            val state = playbackState.state
            val pos = playbackState.position // can be -1

            Stuff.log("onPlaybackStateChanged=$state laststate=$lastState pos=$pos cb=${this@ControllerCallback.hashCode()} sl=${this@SessListener.hashCode()}")

            val isPossiblyAtStart = pos < Stuff.START_POS_LIMIT

            if (lastState == state /* bandcamp does this */ &&
                playbackState.errorMessage != "synthetic" &&
                !(state == PlaybackState.STATE_PLAYING &&
                        isPossiblyAtStart
//                        && packageName !in arrayOf(Stuff.PACKAGE_YOUTUBE_MUSIC)
                        )
            )
                return

            when (state) {
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_STOPPED,
                PlaybackState.STATE_NONE,
                PlaybackState.STATE_ERROR -> {
                    pause()
                    Stuff.log("paused timePlayed=${playingTrackInfo.timePlayed}")
                }
                PlaybackState.STATE_PLAYING -> {
                    if (mutedHash != null && playingTrackInfo.hash != mutedHash)
                        unmute(clearMutedHash = isMuted)

                    if (playingTrackInfo.title != "" && playingTrackInfo.artist != "") {

                        if (!isMuted && playingTrackInfo.hash == mutedHash)
                            mute(playingTrackInfo.hash)
                        // ignore state=playing, pos=lowValue spam
                        if (lastState == state && playingTrackInfo.lastScrobbleHash == playingTrackInfo.hash &&
                            System.currentTimeMillis() - playingTrackInfo.playStartTime < Stuff.START_POS_LIMIT * 2
                        )
                            return

                        if (playingTrackInfo.hash != playingTrackInfo.lastScrobbleHash || (pos >= 0L && isPossiblyAtStart))
                            playingTrackInfo.timePlayed = 0

                        if (!handler.has(playingTrackInfo.hash) &&
                            ((pos >= 0L && isPossiblyAtStart) ||
                                    playingTrackInfo.hash != playingTrackInfo.lastSubmittedScrobbleHash)
                        ) {
                            if (playbackState.errorMessage == "synthetic")
                                Stuff.log("synthetic")
                            scrobble()
                        }
                    }
                }
                else -> {
                    Stuff.log("other ($state) : ${playingTrackInfo.title}")
                }
            }
            if (state != PlaybackState.STATE_BUFFERING)
                lastState = state

        }

        override fun onSessionDestroyed() {
            Stuff.log("onSessionDestroyed ${playingTrackInfo.packageName}")
            stop()
            synchronized(this@SessListener) {
                controllersMap.remove(token)
                    ?.first
                    ?.unregisterCallback(this)
            }
        }

        fun pause() {
            if (lastState == PlaybackState.STATE_PLAYING) {
                if (handler.has(playingTrackInfo.lastScrobbleHash))
                    playingTrackInfo.timePlayed += System.currentTimeMillis() - playingTrackInfo.playStartTime
                else
                    playingTrackInfo.timePlayed = 0
            }
            if (playingTrackInfo.packageName in Stuff.needSyntheticStates)
                syntheticStateHandler.removeCallbacksAndMessages(null)
            handler.remove(playingTrackInfo.lastScrobbleHash)
            if (isMuted)
                unmute(clearMutedHash = false)
        }

        fun stop() {
            pause()
            syntheticStateHandler.removeCallbacksAndMessages(null)
        }

        private fun unmute(clearMutedHash: Boolean) {
            if (mutedHash != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    Stuff.log("unmute: done")
                } else {
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
                }
                if (clearMutedHash)
                    mutedHash = null
                isMuted = false
            }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            if (BuildConfig.DEBUG)
                Stuff.log("extras updated ${playingTrackInfo.packageName}: ${extras.dump()}")
        }

        override fun onSessionEvent(event: String, extras: Bundle?) {
            if (BuildConfig.DEBUG)
                Stuff.log("onSessionEvent ${playingTrackInfo.packageName}: $event ${extras.dump()}")
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo?) {
            if (BuildConfig.DEBUG)
                Stuff.log("audioinfo updated ${playingTrackInfo.packageName}: $info")

            isRemotePlayback =
                info?.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE
        }

        private fun resetMeta() {
            playingTrackInfo.apply {
                artist = ""
                album = ""
                title = ""
                albumArtist = ""
                durationMillis = 0L
            }
        }
    }

    private fun shouldScrobble(packageName: String): Boolean {

        return prefs.scrobblerEnabled && loggedIn &&
                (packageName in allowedPackages ||
                        (prefs.autoDetectApps && packageName !in blockedPackages))
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        when (key) {
            MainPrefs.PREF_ALLOWED_PACKAGES -> synchronized(allowedPackages) {
                allowedPackages.clear()
                allowedPackages.addAll(pref.getStringSet(key, setOf())!!)
            }
            MainPrefs.PREF_BLOCKED_PACKAGES -> synchronized(blockedPackages) {
                blockedPackages.clear()
                blockedPackages.addAll(pref.getStringSet(key, setOf())!!)
            }
        }
        if (key == MainPrefs.PREF_ALLOWED_PACKAGES ||
            key == MainPrefs.PREF_BLOCKED_PACKAGES ||
            key == MainPrefs.PREF_AUTO_DETECT ||
            key == MainPrefs.PREF_MASTER
        ) {

            onActiveSessionsChanged(controllers)
            val pkgsToKeep = controllersMap.values
                .map { it.first.packageName }
                .filter { shouldScrobble(it) }
                .toSet()
            removeSessions(controllersMap.keys.toSet(), pkgsToKeep)
        }
    }
}
