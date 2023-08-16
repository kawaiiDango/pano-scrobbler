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
import android.support.v4.media.MediaMetadataCompat
import com.arn.scrobble.Stuff.dump
import com.arn.scrobble.Stuff.isUrlOrDomain
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Scrobblables
import java.util.Locale
import java.util.Objects

/**
 * Created by arn on 04/07/2017.
 */

class SessListener(
    private val scrobbleHandler: NLService.ScrobbleHandler,
    private val audioManager: AudioManager,
    private val browserPackages: Set<String>
) : OnActiveSessionsChangedListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs = App.prefs
    private val controllersMap =
        mutableMapOf<MediaSession.Token, Pair<MediaController, ControllerCallback>>()
    private var controllers: List<MediaController>? = null

    private val blockedPackages = mutableSetOf<String>()
    private val allowedPackages = mutableSetOf<String>()
    private val loggedIn
        get() = Stuff.isLoggedIn()
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

    fun isMediaPlaying() =
        controllers?.any {
            it.playbackState?.state == PlaybackState.STATE_PLAYING &&
                    !it.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).isNullOrEmpty() &&
                    !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrEmpty()
        } ?: false

    fun findControllersByPackage(packageName: String) =
        controllersMap.values.filter { it.first.packageName == packageName }.map { it.first }

    private fun findCallbackByHash(hash: Int) =
        controllersMap.values.firstOrNull { it.second.trackInfo.lastScrobbleHash == hash }?.second

    fun findControllersByHash(hash: Int) =
        controllersMap.values.filter { it.second.trackInfo.lastScrobbleHash == hash }
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
        val trackInfo: PlayingTrackInfo,
        private val token: MediaSession.Token
    ) : Callback() {

        private var lastState = -1
        private var isRemotePlayback = false
        var isMuted = false

        private val syntheticStateHandler = Handler(scrobbleHandler.looper)

        fun scrobble() {
            Stuff.log("playing: timePlayed=${trackInfo.timePlayed} ${trackInfo.title}")

            scheduleSyntheticStateIfNeeded()

            trackInfo.playStartTime = System.currentTimeMillis()
            scrobbleHandler.remove(trackInfo.lastScrobbleHash)

            scrobbleHandler.nowPlaying(trackInfo)

            trackInfo.lastScrobbleHash = trackInfo.hash
            trackInfo.lastSubmittedScrobbleHash = 0

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)
        }

        private fun scheduleSyntheticStateIfNeeded() {
            if (trackInfo.packageName in Stuff.needSyntheticStates && trackInfo.durationMillis > 0) {
                syntheticStateHandler.removeCallbacksAndMessages(null)
                syntheticStateHandler.postDelayed({
                    onPlaybackStateChanged(
                        PlaybackState.Builder()
                            .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                            .setErrorMessage("synthetic")
                            .build()
                    )
                }, trackInfo.durationMillis)
            }
        }

        @Synchronized
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return

            // spotify ads look like these:
            // e.g. GCTC () [] ~ Free Spring Tuition
            // Beef Its Whats For Dinner () [] ~
            // Kroger () [] ~ Advertisement

            if (metadata.getLong(MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT) != 0L) {
                resetMeta()
                return
            }

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
            val youtubeHeight =
                metadata.getLong("com.google.android.youtube.MEDIA_METADATA_VIDEO_WIDTH_PX")
            val youtubeWidth =
                metadata.getLong("com.google.android.youtube.MEDIA_METADATA_VIDEO_HEIGHT_PX")

            when (trackInfo.packageName) {
                Stuff.PACKAGE_XIAMI -> {
                    artist = artist.replace(";", "; ")
                }

                Stuff.PACKAGE_PANDORA -> {
                    artist = artist.replace("^Ofln - ".toRegex(), "")
                    albumArtist = ""
                }

                Stuff.PACKAGE_PODCAST_ADDICT -> {
                    if (albumArtist != "") {
                        artist = albumArtist
                        albumArtist = ""
                    }
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

                Stuff.PACKAGE_GOOGLE -> {
                    // google podcasts
                    if (artist == "") {
                        artist =
                            metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.trim()
                                ?: ""
                    }
                }

                Stuff.PACKAGE_YANDEX_MUSIC -> {
                    albumArtist = ""
                }

                Stuff.PACKAGE_YAMAHA_MUSIC_CAST -> {
                    if (artist.contains(" - ")) {
                        val (first, second) = artist.split(" - ", limit = 2)
                        artist = first
                        album = second
                    }
                }

                Stuff.PACKAGE_SPOTIFY -> {
                    // goddamn spotify
                    if (albumArtist.isNotEmpty() && albumArtist != artist &&
                        !MetadataUtils.isVariousArtists(albumArtist)
                    )
                        artist = albumArtist

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
                artist == trackInfo.origArtist && title == trackInfo.origTitle && album == trackInfo.origAlbum
                        && albumArtist == trackInfo.origAlbumArtist
            val onlyDurationUpdated = sameAsOld && durationMillis != trackInfo.durationMillis

            Stuff.log(
                "onMetadataChanged $artist ($albumArtist) [$album] ~ $title, sameAsOld=$sameAsOld, " +
                        "duration=$durationMillis lastState=$lastState, package=${trackInfo.packageName} isRemotePlayback=$isRemotePlayback cb=${this.hashCode()} sl=${this@SessListener.hashCode()}"
            )
            if (BuildConfig.DEBUG)
                metadata.dump()

            if (artist == "" || title == "")
                return

            if (!sameAsOld || onlyDurationUpdated) {
                trackInfo.putOriginals(artist, title, album, albumArtist)

                trackInfo.ignoreOrigArtist = shouldIgnoreOrigArtist(trackInfo)

                trackInfo.canDoFallbackScrobble = trackInfo.ignoreOrigArtist && (
                        trackInfo.packageName in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK ||
                                youtubeHeight > 0 && youtubeWidth > 0 && youtubeHeight == youtubeWidth
                        )
                // auto generated artist channels usually have square videos

                trackInfo.durationMillis = durationMillis
                trackInfo.hash = Objects.hash(artist, album, title, trackInfo.packageName)

                if (trackInfo.packageName in Stuff.IGNORE_ARTIST_META)
                    trackInfo.artist = trackInfo.artist.substringBeforeLast(" - Topic")

                if (mutedHash != null && trackInfo.hash != mutedHash && lastState == PlaybackState.STATE_PLAYING)
                    unmute(clearMutedHash = isMuted)

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if ((!scrobbleHandler.has(trackInfo.hash) || onlyDurationUpdated) &&
                    lastState == PlaybackState.STATE_PLAYING
                ) {
                    trackInfo.timePlayed = 0
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
                !(state == PlaybackState.STATE_PLAYING && isPossiblyAtStart)
            )
                return

            when (state) {
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_STOPPED,
                PlaybackState.STATE_NONE,
                PlaybackState.STATE_ERROR -> {
                    pause()
                    Stuff.log("paused timePlayed=${trackInfo.timePlayed}")
                }

                PlaybackState.STATE_PLAYING -> {
                    if (mutedHash != null && trackInfo.hash != mutedHash)
                        unmute(clearMutedHash = isMuted)

                    if (trackInfo.title != "" && trackInfo.artist != "") {

                        if (!isMuted && trackInfo.hash == mutedHash)
                            mute(trackInfo.hash)
                        // ignore state=playing, pos=lowValue spam
                        if (lastState == state && trackInfo.lastScrobbleHash == trackInfo.hash &&
                            System.currentTimeMillis() - trackInfo.playStartTime < Stuff.START_POS_LIMIT * 2
                        )
                            return

                        if (trackInfo.hash != trackInfo.lastScrobbleHash || (pos >= 0L && isPossiblyAtStart))
                            trackInfo.timePlayed = 0

                        if (!scrobbleHandler.has(trackInfo.hash) &&
                            ((pos >= 0L && isPossiblyAtStart) ||
                                    trackInfo.hash != trackInfo.lastSubmittedScrobbleHash)
                        ) {
                            if (playbackState.errorMessage == "synthetic")
                                Stuff.log("synthetic")
                            scrobble()
                        }
                    }
                }

                else -> {
                    Stuff.log("other ($state) : ${trackInfo.title}")
                }
            }
            if (state != PlaybackState.STATE_BUFFERING)
                lastState = state

        }

        override fun onSessionDestroyed() {
            Stuff.log("onSessionDestroyed ${trackInfo.packageName}")
            stop()
            synchronized(this@SessListener) {
                controllersMap.remove(token)
                    ?.first
                    ?.unregisterCallback(this)
            }
        }

        fun pause() {
            if (lastState == PlaybackState.STATE_PLAYING) {
                if (scrobbleHandler.has(trackInfo.lastScrobbleHash))
                    trackInfo.timePlayed += System.currentTimeMillis() - trackInfo.playStartTime
                else
                    trackInfo.timePlayed = 0
            }
            if (trackInfo.packageName in Stuff.needSyntheticStates)
                syntheticStateHandler.removeCallbacksAndMessages(null)
            scrobbleHandler.remove(trackInfo.lastScrobbleHash, trackInfo.packageName)
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
                Stuff.log("extras updated ${trackInfo.packageName}: ${extras.dump()}")
        }

        override fun onSessionEvent(event: String, extras: Bundle?) {
            if (BuildConfig.DEBUG)
                Stuff.log("onSessionEvent ${trackInfo.packageName}: $event ${extras.dump()}")
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo?) {
            if (BuildConfig.DEBUG)
                Stuff.log("audioinfo updated ${trackInfo.packageName}: $info")

            isRemotePlayback =
                info?.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE
        }

        private fun resetMeta() {
            trackInfo.apply {
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

    private fun shouldIgnoreOrigArtist(trackInfo: PlayingTrackInfo): Boolean {
        return if (
            trackInfo.packageName in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK && trackInfo.album.isNotEmpty() ||
            trackInfo.packageName == Stuff.PACKAGE_YOUTUBE_TV && trackInfo.album.isNotEmpty() ||
            trackInfo.packageName == Stuff.PACKAGE_YMUSIC &&
            trackInfo.album.replace("YMusic", "").isNotEmpty()
        )
            false
        else (trackInfo.packageName in Stuff.IGNORE_ARTIST_META &&
                !trackInfo.artist.endsWith("- Topic")) ||
                (trackInfo.packageName in browserPackages &&
                        trackInfo.artist.isUrlOrDomain())
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String?) {
        when (key) {
            MainPrefs.PREF_ALLOWED_PACKAGES -> synchronized(allowedPackages) {
                allowedPackages.clear()
                allowedPackages.addAll(pref.getStringSet(key, setOf())!!)
            }

            MainPrefs.PREF_BLOCKED_PACKAGES -> synchronized(blockedPackages) {
                blockedPackages.clear()
                blockedPackages.addAll(pref.getStringSet(key, setOf())!!)
            }

            MainPrefs.PREF_SCROBBLE_ACCOUNTS -> {
                Scrobblables.updateScrobblables()
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
