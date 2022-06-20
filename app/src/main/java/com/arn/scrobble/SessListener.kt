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
import android.os.Handler
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
        mutableMapOf<MediaSession.Token, Pair<MediaController, MyCallback>>()
    private var controllers: List<MediaController>? = null

    private val blockedPackages = mutableSetOf<String>()
    private val allowedPackages = mutableSetOf<String>()
    private val loggedIn
        get() = prefs.lastfmSessKey != null
    val packageMap = mutableMapOf<String, HashesAndTimes>()
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
                    var hashesAndTimes = packageMap[controller.packageName]
                    if (hashesAndTimes == null || hasMultipleSessions) {
                        hashesAndTimes = HashesAndTimes()
                        packageMap[controller.packageName] = hashesAndTimes
                    }
                    val cb =
                        MyCallback(controller.packageName, hashesAndTimes, controller.sessionToken)
                    controller.registerCallback(cb)
                    //Medoly needs this
                    controller.playbackState.let { cb.onPlaybackStateChanged(it) }
                    controller.metadata.let { cb.onMetadataChanged(it) }
                    controller.playbackInfo.let { cb.onAudioInfoChanged(it) }

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
        controllersMap.values.firstOrNull { it.second.hashesAndTimes.lastScrobbleHash == hash }?.second

    fun findControllersByHash(hash: Int) =
        controllersMap.values.filter { it.second.hashesAndTimes.lastScrobbleHash == hash }
            .map { it.first }

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
            if (
//                pair.first.packageName !in arrayOf(Stuff.IGNORE_ARTIST_META[0], Stuff.IGNORE_ARTIST_META[1], Stuff.PACKAGE_YOUTUBE_MUSIC) &&
                (token !in tokensToKeep || packageNamesToKeep?.contains(pair.first.packageName) == false)
            ) {
                callback.stop()
                controller.unregisterCallback(callback)
                it.remove()
            }
        }
    }

    fun unregisterPrefsChangeListener() {
        prefs.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    inner class MyCallback(
        private val packageName: String,
        val hashesAndTimes: HashesAndTimes,
        private val token: MediaSession.Token
    ) : Callback() {

        private var lastState = -1
        private var currHash = 0

        private var artist = ""
        private var album = ""
        private var title = ""
        private var albumArtist = ""
        private var duration = -1L
        private var isRemotePlayback = false
        var isMuted = false

        private val syntheticStateHandler = Handler(handler.looper)

        fun scrobble() {
            Stuff.log("playing: timePlayed=${hashesAndTimes.timePlayed} $title")

            scheduleSyntheticStateIfNeeded()

            hashesAndTimes.lastScrobbleTime = System.currentTimeMillis()
            handler.removeMessages(hashesAndTimes.lastScrobbleHash)

            handler.nowPlaying(
                artist,
                album,
                title,
                albumArtist,
                hashesAndTimes.timePlayed,
                duration,
                currHash,
                packageName
            )

            hashesAndTimes.lastScrobbleHash = currHash
            hashesAndTimes.lastScrobbledHash = 0

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)
        }

        private fun scheduleSyntheticStateIfNeeded() {
            if (packageName in Stuff.needSyntheticStates && duration > 0) {
                syntheticStateHandler.removeCallbacksAndMessages(null)
                syntheticStateHandler.postDelayed({
                    onPlaybackStateChanged(
                        PlaybackState.Builder()
                            .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                            .setErrorMessage("synthetic")
                            .build()
                    )
                }, duration)
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
            var duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            if (duration < -1)
                duration = -1

            when (packageName) {
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
                Stuff.PACKAGE_SPOTIFY -> {
                    // ads look like these:
                    // e.g. GCTC () [] ~ Free Spring Tuition
                    // Beef Its Whats For Dinner () [] ~
                    // Kroger () [] ~ Advertisement
                    if (albumArtist.isEmpty() && album.isEmpty() && title.isNotEmpty()) {
                        resetMeta()
                        if (BuildConfig.DEBUG) { // this probably also mutes podcasts but thats fine for me
                            mutedHash = currHash
                            mute(currHash)
                        }
                        return
                    }
                }
            }

            val sameAsOld = artist == this.artist && title == this.title && album == this.album
                    && albumArtist == this.albumArtist
            val onlyDurationUpdated = sameAsOld && duration != this.duration

            Stuff.log(
                "onMetadataChanged $artist ($albumArtist) [$album] ~ $title, sameAsOld=$sameAsOld, " +
                        "duration=$duration lastState=$lastState, package=$packageName isRemotePlayback=$isRemotePlayback cb=${this.hashCode()} sl=${this@SessListener.hashCode()}"
            )
            if (!sameAsOld || onlyDurationUpdated) {
                this.artist = artist
                this.album = album
                this.title = title
                this.albumArtist = albumArtist
                this.duration = duration
                currHash = Stuff.genHashCode(artist, album, title, packageName)

                if (mutedHash != null && currHash != mutedHash && lastState == PlaybackState.STATE_PLAYING)
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
                if (artist != "" && title != "" &&
                    (!handler.hasMessages(currHash) || !onlyDurationUpdated) &&
                    lastState == PlaybackState.STATE_PLAYING
                ) {
                    hashesAndTimes.timePlayed = 0
                    scrobble()
                }
            }
        }

        @Synchronized
        override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
            playbackState ?: return

            val state = playbackState.state
            val pos = playbackState.position // can be -1

            Stuff.log("onPlaybackStateChanged=$state laststate=$lastState pos=$pos cb=${this@MyCallback.hashCode()} sl=${this@SessListener.hashCode()}")

            val isPossiblyAtStart = pos < Stuff.START_POS_LIMIT

            if (lastState == state /* bandcamp does this */ &&
                playbackState.errorMessage != "synthetic"
//                && !(state == PlaybackState.STATE_PLAYING &&
//                        isPossiblyAtStart &&
//                        packageName !in arrayOf(Stuff.PACKAGE_YOUTUBE_MUSIC)
//                        )
            )
                return

            when (state) {
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_STOPPED,
                PlaybackState.STATE_NONE,
                PlaybackState.STATE_ERROR -> {
                    pause()
                    Stuff.log("paused timePlayed=${hashesAndTimes.timePlayed}")
                }
                PlaybackState.STATE_PLAYING -> {
                    if (mutedHash != null && currHash != mutedHash)
                        unmute(clearMutedHash = isMuted)

                    if (title != "" && artist != "") {

                        if (!isMuted && currHash == mutedHash)
                            mute(currHash)
                        // ignore state=playing, pos=lowValue spam
                        if (lastState == state && hashesAndTimes.lastScrobbleHash == currHash &&
                            System.currentTimeMillis() - hashesAndTimes.lastScrobbleTime < Stuff.START_POS_LIMIT * 2
                        )
                            return

                        if (currHash != hashesAndTimes.lastScrobbleHash || (pos >= 0L && isPossiblyAtStart))
                            hashesAndTimes.timePlayed = 0

                        if (!handler.hasMessages(currHash) &&
                            ((pos >= 0L && isPossiblyAtStart) ||
                                    currHash != hashesAndTimes.lastScrobbledHash)
                        ) {
                            if (playbackState.errorMessage == "synthetic")
                                Stuff.log("synthetic")
                            scrobble()
                        }
                    }
                }
                else -> {
                    Stuff.log("other ($state) : $title")
                }
            }
            if (state != PlaybackState.STATE_BUFFERING)
                lastState = state

        }

        override fun onSessionDestroyed() {
            Stuff.log("onSessionDestroyed $packageName")
            stop()
            synchronized(this@SessListener) {
                controllersMap.remove(token)
                    ?.first
                    ?.unregisterCallback(this)
            }
        }

        fun pause() {
            if (lastState == PlaybackState.STATE_PLAYING) {
                if (handler.hasMessages(hashesAndTimes.lastScrobbleHash))
                    hashesAndTimes.timePlayed += System.currentTimeMillis() - hashesAndTimes.lastScrobbleTime
                else
                    hashesAndTimes.timePlayed = 0
            }
            if (packageName in Stuff.needSyntheticStates)
                syntheticStateHandler.removeCallbacksAndMessages(null)
            handler.remove(hashesAndTimes.lastScrobbleHash)
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

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo?) {
            if (BuildConfig.DEBUG)
                Stuff.log("audioinfo updated $packageName: $info")

            isRemotePlayback =
                info?.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE
        }

        private fun resetMeta() {
            artist = ""
            album = ""
            title = ""
            albumArtist = ""
            duration = -1L
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

    class HashesAndTimes {
        var lastScrobbleHash = 0
        var lastScrobbledHash = 0
        var lastScrobbleTime = 0L
        var timePlayed = 0L
    }
}
