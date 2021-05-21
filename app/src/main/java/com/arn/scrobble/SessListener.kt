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
import android.os.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Created by arn on 04/07/2017.
 */

class SessListener (
    private val pref: SharedPreferences,
    private val handler: NLService.ScrobbleHandler,
    private val audioManager: AudioManager,
    private val defaultScope: CoroutineScope
) : OnActiveSessionsChangedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val controllersMap = mutableMapOf<MediaSession.Token, Pair<MediaController, MyCallback>>()
    private var controllers : List<MediaController>? = null

    private val blackList = mutableSetOf<String>()
    private val whiteList = mutableSetOf<String>()
    private val autoDetectApps
        get() = pref.getBoolean(Stuff.PREF_AUTO_DETECT, true)
    private val scrobblingEnabled
        get() = pref.getBoolean(Stuff.PREF_MASTER, true)
    private val loggedIn
        get() = pref.getString(Stuff.PREF_LASTFM_SESS_KEY, null) != null
    lateinit var browserPackages: Set<String>
    val packageMap = mutableMapOf<String, HashesAndTimes>()

    init {
        pref.registerOnSharedPreferenceChangeListener(this)
        whiteList.addAll(pref.getStringSet(Stuff.PREF_WHITELIST, setOf())!!)
        blackList.addAll(pref.getStringSet(Stuff.PREF_BLACKLIST, setOf())!!)
    }

    // this list of controllers is unreliable esp. with yt and yt music
    // it may be empty even if there are active sessions
    @Synchronized
    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        this.controllers = controllers
        Stuff.log("controllers: " + controllers?.joinToString { it.packageName })

        if (!scrobblingEnabled || controllers == null)
            return

        val tokens = mutableSetOf<MediaSession.Token>()
        for (controller in controllers) {
            if (shouldScrobble(controller.packageName)) {
                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
                if (controller.sessionToken !in controllersMap) {
                    val ignoreArtistMeta =  controller.packageName in Stuff.IGNORE_ARTIST_META ||
                            controller.packageName in browserPackages
                    var hashesAndTimes = packageMap[controller.packageName]
                    if (hashesAndTimes == null) {
                        hashesAndTimes = HashesAndTimes()
                        packageMap[controller.packageName] = hashesAndTimes
                    }
                    val cb = MyCallback(controller.packageName, hashesAndTimes, controller.sessionToken, ignoreArtistMeta)
                    controller.registerCallback(cb)
                    //Medoly needs this
                    controller.playbackState.let { cb.onPlaybackStateChanged(it) }
                    controller.metadata.let { cb.onMetadataChanged(it) }

                    controllersMap[controller.sessionToken] = controller to cb
                }
            }
        }
        // Now remove old sessions that are not longer active.
//        removeSessions(tokens)
    }
    @Synchronized
    fun removeSessions(tokensToKeep: Set<MediaSession.Token>, packageNamesToKeep: Set<String>? = null) {
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

    inner class MyCallback(private val packageName: String,
                     private val hashesAndTimes: HashesAndTimes,
                     private val token: MediaSession.Token,
                     private val isIgnoreArtistMeta: Boolean) : Callback() {

        private var lastState = -1
        private var currHash = 0

        private var artist = ""
        private var album = ""
        private var title = ""
        private var albumArtist = ""
        private var duration = -1L

        private val stateHandler = object : Handler(handler.looper) {
            override fun handleMessage(msg: Message) {
                val state = msg.arg1
                val pos = msg.arg2.toLong()

                Stuff.log("onPlaybackStateChanged=$state laststate=$lastState pos=$pos cb=${this@MyCallback.hashCode()}")

                val isPossiblyAtStart = pos < Stuff.START_POS_LIMIT //wynk puts -1

                if (lastState == state /* bandcamp does this */ &&
                        !(state == PlaybackState.STATE_PLAYING && isPossiblyAtStart))
                    return

                when (state) {
                    PlaybackState.STATE_PAUSED,
                    PlaybackState.STATE_STOPPED,
                    PlaybackState.STATE_NONE,
                    PlaybackState.STATE_ERROR -> {
                        pause()
                        Stuff.log("paused")
                    }
                    PlaybackState.STATE_PLAYING -> {
                        if (title != "" && artist != "") {
                            if (currHash != hashesAndTimes.lastScrobbleHash || (pos >= 0L && isPossiblyAtStart))
                                hashesAndTimes.timePlayed = 0

                            if (!handler.hasMessages(currHash) && ((pos >= 0L && isPossiblyAtStart) ||
                                            currHash != hashesAndTimes.lastScrobbledHash)) {
                                Stuff.log("playing: timePlayed=${hashesAndTimes.timePlayed} $title")
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
        }

        fun scrobble() {
            if ((packageName == Stuff.PACKAGE_BLACKPLAYER || packageName == Stuff.PACKAGE_BLACKPLAYEREX) && duration > 0) {
                stateHandler.removeCallbacksAndMessages("bp")
                stateHandler.sendMessageDelayed(
                        stateHandler.obtainMessage(0, PlaybackState.STATE_PLAYING, 0, "bp"),
                        duration
                )
            }
            hashesAndTimes.lastScrobbleTime = System.currentTimeMillis()
            val isWhitelisted = packageName in whiteList
            val packageNameParam = if (!isWhitelisted) packageName else null
            handler.removeMessages(hashesAndTimes.lastScrobbleHash)

            defaultScope.launch {
                if (isIgnoreArtistMeta) {
                    val splits = MetadataUtils.sanitizeTitle(title)
                    handler.nowPlaying(
                        splits[0],
                        "",
                        splits[1],
                        "",
                        hashesAndTimes.timePlayed,
                        duration,
                        currHash,
                        false,
                        packageNameParam
                    )
                } else
                    handler.nowPlaying(
                        artist,
                        album,
                        title,
                        albumArtist,
                        hashesAndTimes.timePlayed,
                        duration,
                        currHash,
                        true,
                        packageNameParam
                    )
            }
            hashesAndTimes.lastScrobbleHash = currHash
            hashesAndTimes.lastScrobbledHash = 0
        }

        @Synchronized
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            var albumArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim() ?: ""
            var artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?:
                    albumArtist // do not scrobble empty artists, ads will get scrobbled
            var album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim() ?: ""
            var title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: ""
//            val genre = metadata?.getString(MediaMetadata.METADATA_KEY_GENRE)?.trim() ?: ""
            // The genre field is not used by google podcasts and podcast addict
            var duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1
            if (duration < -1)
                duration = -1

            when (packageName) {
                Stuff.PACKAGE_XIAMI -> artist = artist.replace(";", "; ")
                Stuff.PACKAGE_PANDORA -> {
                    artist = artist.replace("Ofln - ", "")
                    albumArtist = ""
                }
                Stuff.PACKAGE_PODCAST_ADDICT -> {
                    val idx = artist.lastIndexOf(" • ")
                    if (idx != -1)
                        artist = artist.substring(0, idx)
                }
                Stuff.PACKAGE_SONOS,
                Stuff.PACKAGE_SONOS2 -> {
                    metadata?.getString(MediaMetadata.METADATA_KEY_COMPOSER)?.let{
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
            }

            val sameAsOld = artist == this.artist && title == this.title && album == this.album && albumArtist == this.albumArtist

            Stuff.log("onMetadataChanged $artist ($albumArtist) [$album] ~ $title, sameAsOld=$sameAsOld, " +
                    "duration=$duration lastState=$lastState, package=$packageName cb=${this.hashCode()}")
            if (!sameAsOld) {
                this.artist = artist
                this.album = album
                this.title = title
                this.albumArtist = albumArtist
                this.duration = duration
                currHash = Stuff.genHashCode(artist, album, title, packageName)

                // hack for buggy youtubes until they fix it
                if (packageName in (Stuff.IGNORE_ARTIST_META.take(5) + Stuff.PACKAGE_YOUTUBE_MUSIC) &&
                        lastState == PlaybackState.STATE_STOPPED &&
                        audioManager.isMusicActive)
                    lastState = PlaybackState.STATE_PLAYING

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if (artist != "" && title != "" &&
                        !handler.hasMessages(currHash) &&
                        lastState == PlaybackState.STATE_PLAYING)
                    scrobble()
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state != null) {
                val msg = stateHandler.obtainMessage(0, state.state, state.position.toInt())
                stateHandler.sendMessageDelayed(msg, Stuff.META_WAIT)
            }
        }

        //Do not use
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
            if (packageName == Stuff.PACKAGE_BLACKPLAYER || packageName == Stuff.PACKAGE_BLACKPLAYEREX)
                stateHandler.removeCallbacksAndMessages("bp")
            handler.remove(hashesAndTimes.lastScrobbleHash)
        }

        fun stop() {
            pause()
            stateHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun shouldScrobble(packageName: String): Boolean {

        return scrobblingEnabled && loggedIn &&
                (packageName in whiteList ||
                (autoDetectApps && packageName !in blackList))
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        when (key){
            Stuff.PREF_WHITELIST -> synchronized(whiteList) {
                whiteList.clear()
                whiteList.addAll(pref.getStringSet(key, setOf())!!)
            }
            Stuff.PREF_BLACKLIST -> synchronized(blackList) {
                blackList.clear()
                blackList.addAll(pref.getStringSet(key, setOf())!!)
            }
        }
        if (key == Stuff.PREF_WHITELIST ||
                key == Stuff.PREF_BLACKLIST ||
                key == Stuff.PREF_AUTO_DETECT ||
                key == Stuff.PREF_MASTER) {

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
