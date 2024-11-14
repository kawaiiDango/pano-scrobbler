package com.arn.scrobble

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
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.dump
import com.arn.scrobble.utils.Stuff.isUrlOrDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Objects

/**
 * Created by arn on 04/07/2017.
 */
class SessListener(
    private val scope: CoroutineScope,
    private val scrobbleHandler: NLService.ScrobbleQueue,
    private val audioManager: AudioManager,
) : OnActiveSessionsChangedListener {

    private val mainPrefs = PlatformStuff.mainPrefs
    private val controllersMap =
        mutableMapOf<MediaSession.Token, Pair<MediaController, ControllerCallback>>()
    private var platformControllers: List<MediaController>? = null

    private val blockedPackages =
        mainPrefs.data.map { it.blockedPackages }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    private val allowedPackages =
        mainPrefs.data.map { it.allowedPackages }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    private val seenPackages =
        mainPrefs.data.map { it.seenPackages }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    private val scrobblerEnabled =
        mainPrefs.data.map { it.scrobblerEnabled }.stateIn(scope, SharingStarted.Eagerly, false)
    private val autoDetectApps =
        mainPrefs.data.map { it.autoDetectAppsP }.stateIn(scope, SharingStarted.Eagerly, false)
    private val scrobbleSpotifyRemote = mainPrefs.data.map { it.scrobbleSpotifyRemote }
        .stateIn(scope, SharingStarted.Eagerly, false)
    private val loggedIn
        get() = Stuff.isLoggedIn()
    val packageTagTrackMap = mutableMapOf<String, PlayingTrackInfo>()
    private var mutedHash: Int? = null

    init {
        scope.launch {
            combine(
                allowedPackages,
                blockedPackages,
                autoDetectApps,
                scrobblerEnabled,
            ) { allowed, blocked, autoDetect, scrobbleEnabled ->
                onActiveSessionsChanged(platformControllers)
                val pkgsToKeep = controllersMap.values
                    .map { it.first }
                    .filter { shouldScrobble(it) }
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
            if (shouldScrobble(it) && it.sessionToken !in controllersMap)
                MediaController(PlatformStuff.application, it.sessionToken)
            else null
        }

//        val tokens = mutableSetOf<MediaSession.Token>()
        for (controller in controllersFiltered) {
//            if (shouldScrobble(controller.packageName)) {
//                tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
//                if (controller.sessionToken !in controllersMap) {

            val playingTrackInfo =
                packageTagTrackMap[controller.packageName + "|" + controller.tagCompat]
                    ?: PlayingTrackInfo(controller.packageName, controller.tagCompat).also {
                        packageTagTrackMap[controller.packageName + "|" + controller.tagCompat] = it
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

    fun isMediaPlaying() =
        platformControllers?.any { it.isMediaPlaying() } ?: false

    fun findControllersByPackage(packageName: String) =
        controllersMap.values.filter { it.first.packageName == packageName }.map { it.first }

    private fun findCallbackByHash(hash: Int) =
        controllersMap.values.firstOrNull { it.second.trackInfo.lastScrobbleHash == hash }?.second

    fun findControllersByHash(hash: Int) =
        controllersMap.values.filter { it.second.trackInfo.lastScrobbleHash == hash }
            .map { it.first }

    fun findTrackInfoByHash(hash: Int) =
        packageTagTrackMap.values.find { it.hash == hash }

    private val MediaController.tagCompat
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> tag
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 -> tag
            else -> "greylist-max-o"
        }

    fun mute(hash: Int) {
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
                callback.pause()
                controller.unregisterCallback(callback)
                it.remove()
            }
        }
    }

    //        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
    // MediaController.getTag() exists on Android 10 and lower but is marked as @hide

    private fun MediaController.isMediaPlaying() =
        playbackState?.state in arrayOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING
        ) && !metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrEmpty()


    fun hasOtherPlayingControllers(thisTrackInfo: PlayingTrackInfo): Boolean {
        return controllersMap.values.any { (controller, cb) ->
            controller.packageName == thisTrackInfo.packageName
                    && controller.tagCompat != thisTrackInfo.sessionTag &&
                    controller.isMediaPlaying()
//                        && !cb.trackInfo.hasBlockedTag
        }
    }

    private fun shouldScrobble(platformController: MediaController): Boolean {
        val should = scrobblerEnabled.value && loggedIn &&
                (platformController.packageName in allowedPackages.value ||
                        (autoDetectApps.value && platformController.packageName !in blockedPackages.value))

        return should
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
                (trackInfo.packageName in Stuff.browserPackages &&
                        trackInfo.artist.isUrlOrDomain())
    }

    inner class ControllerCallback(
        val trackInfo: PlayingTrackInfo,
        private val token: MediaSession.Token
    ) : MediaController.Callback() {

        private var lastPlayingState = -1
        private var lastState: PlaybackState? = null
        private var isRemotePlayback = false
        var isMuted = false

        private suspend fun scrobble() {
            if (hasOtherPlayingControllers(trackInfo) && trackInfo.hasBlockedTag) {
                Logger.d { "multiple scrobblable controllers for ${trackInfo.packageName}, ignoring ${trackInfo.sessionTag}" }
                pause()
                return
            }

            Logger.d { "playing: timePlayed=${trackInfo.timePlayed} ${trackInfo.title}" }

            trackInfo.playStartTime = System.currentTimeMillis()
            scrobbleHandler.remove(trackInfo.lastScrobbleHash)

            scrobbleHandler.nowPlaying(trackInfo)

            trackInfo.lastScrobbleHash = trackInfo.hash
            trackInfo.lastSubmittedScrobbleHash = 0

            // if another player tried to scrobble, unmute whatever was muted
            // if self was muted, clear the muted hash too
            unmute(clearMutedHash = isMuted)

            // add to seen packages
            if (trackInfo.packageName !in seenPackages.value) {
                mainPrefs.updateData { it.copy(seenPackages = (it.seenPackages + trackInfo.packageName)) }
            }
        }

        @Synchronized
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return

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

                Stuff.PACKAGE_YANDEX_MUSIC -> {
                    albumArtist = ""
                }

                Stuff.PACKAGE_SPOTIFY -> {
                    // goddamn spotify
                    if (albumArtist.isNotEmpty() && albumArtist != artist &&
                        !MetadataUtils.isVariousArtists(albumArtist)
                    )
                        artist = albumArtist
                }

                Stuff.PACKAGE_NINTENDO_MUSIC -> {
                    if (artist.isEmpty())
                        artist = Stuff.ARTIST_NINTENDO_MUSIC
                }
            }

            if (trackInfo.packageName in Stuff.IGNORE_ARTIST_META)
                trackInfo.artist = trackInfo.artist.substringBeforeLast(" - Topic")

            val sameAsOld =
                artist == trackInfo.origArtist && title == trackInfo.origTitle && album == trackInfo.origAlbum
                        && albumArtist == trackInfo.origAlbumArtist
            val onlyDurationUpdated = sameAsOld && durationMillis != trackInfo.durationMillis

            Logger.i {
                "onMetadataChanged $artist ($albumArtist) [$album] ~ $title, sameAsOld=$sameAsOld, " +
                        "duration=$durationMillis lastState=$lastPlayingState, isRemotePlayback=$isRemotePlayback cb=${this.hashCode()}}"
            }

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

                if (mutedHash != null && trackInfo.hash != mutedHash && lastPlayingState == PlaybackState.STATE_PLAYING)
                    unmute(clearMutedHash = isMuted)

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if ((!scrobbleHandler.has(trackInfo.hash) || onlyDurationUpdated) &&
                    lastPlayingState == PlaybackState.STATE_PLAYING &&
                    artist.isNotEmpty() && title.isNotEmpty()
                ) {
                    trackInfo.timePlayed = 0
                    scope.launch {
                        scrobble()
                    }
                }
            }
        }

        private fun ignoreScrobble() {
            // scrobbling may have already started from onMetadataChanged
            scrobbleHandler.remove(trackInfo.lastScrobbleHash, trackInfo.packageName)
            // do not scrobble again
            lastPlayingState = PlaybackState.STATE_NONE
        }

        @Synchronized
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            lastState = state

            state ?: return

            val playingState = state.state
            val pos = state.position // can be -1

            Logger.i { "onPlaybackStateChanged=$playingState laststate=$lastPlayingState pos=$pos cb=${this@ControllerCallback.hashCode()} sl=${this@SessListener.hashCode()}" }

//            extras?.let { Napier.dLazy { "state extras: " + it.dump() } }

            // do not scrobble spotify remote playback
            if (!scrobbleSpotifyRemote.value &&
                trackInfo.packageName == Stuff.PACKAGE_SPOTIFY
                && state.extras?.getBoolean("com.spotify.music.extra.ACTIVE_PLAYBACK_LOCAL") == false
            ) {
                Logger.i { "ignoring spotify remote playback" }
                ignoreScrobble()
                return
            }

            // do not scrobble youtube music ads (they are not seekable)
            if (trackInfo.packageName in arrayOf(
                    Stuff.PACKAGE_YOUTUBE_MUSIC,
                    Stuff.PACKAGE_YOUTUBE_TV
                ) &&
                trackInfo.durationMillis > 0 &&
                state.actions and PlaybackState.ACTION_SEEK_TO == 0L
            ) {
                Logger.i { "ignoring youtube music ad" }
                ignoreScrobble()
                return
            }

            val isPossiblyAtStart = pos < Stuff.START_POS_LIMIT

            if (lastPlayingState == playingState /* bandcamp does this */ &&
                !(playingState == PlaybackState.STATE_PLAYING && isPossiblyAtStart)
            )
                return

            when (playingState) {
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_STOPPED,
                PlaybackState.STATE_NONE,
                PlaybackState.STATE_ERROR -> {
                    pause()
                    Logger.d { "paused timePlayed=${trackInfo.timePlayed}" }
                }

                PlaybackState.STATE_PLAYING -> {
                    if (mutedHash != null && trackInfo.hash != mutedHash)
                        unmute(clearMutedHash = isMuted)

                    if (trackInfo.title != "" && trackInfo.artist != "") {

                        if (!isMuted && trackInfo.hash == mutedHash)
                            mute(trackInfo.hash)
                        // ignore state=playing, pos=lowValue spam
                        if (lastPlayingState == playingState && trackInfo.lastScrobbleHash == trackInfo.hash &&
                            System.currentTimeMillis() - trackInfo.playStartTime < Stuff.START_POS_LIMIT * 2
                        )
                            return

                        if (trackInfo.hash != trackInfo.lastScrobbleHash || (pos >= 0L && isPossiblyAtStart))
                            trackInfo.timePlayed = 0

                        if (!scrobbleHandler.has(trackInfo.hash) &&
                            ((pos >= 0L && isPossiblyAtStart) ||
                                    trackInfo.hash != trackInfo.lastSubmittedScrobbleHash)
                        ) {
                            scope.launch {
                                scrobble()
                            }
                        }
                    }
                }

                else -> {
                    Logger.d { "other ($playingState) : ${trackInfo.title}" }
                }
            }
            if (playingState != PlaybackState.STATE_BUFFERING)
                lastPlayingState = playingState

        }

        override fun onSessionDestroyed() {
            Logger.d { "onSessionDestroyed ${trackInfo.packageName}" }
            pause()
            synchronized(this@SessListener) {
                controllersMap.remove(token)
                    ?.first
                    ?.unregisterCallback(this)
            }
        }

        fun pause() {
            if (lastPlayingState == PlaybackState.STATE_PLAYING) {
                if (scrobbleHandler.has(trackInfo.lastScrobbleHash))
                    trackInfo.timePlayed += System.currentTimeMillis() - trackInfo.playStartTime
                else
                    trackInfo.timePlayed = 0
            }

            scrobbleHandler.remove(
                trackInfo.lastScrobbleHash,
                if (hasOtherPlayingControllers(trackInfo))
                    null
                else
                    trackInfo.packageName
            )
            if (isMuted)
                unmute(clearMutedHash = false)
        }

        private fun unmute(clearMutedHash: Boolean) {
            if (mutedHash != null) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
                Logger.i { "unmute: done" }
                if (clearMutedHash)
                    mutedHash = null
                isMuted = false
            }
        }

        override fun onExtrasChanged(extras: Bundle?) {
            Logger.d { "extras updated ${trackInfo.packageName}: ${extras.dump()}" }
        }

        override fun onSessionEvent(event: String, extras: Bundle?) {
            Logger.d { "onSessionEvent ${trackInfo.packageName}: $event ${extras.dump()}" }
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) {
            Logger.d { "audioinfo updated ${trackInfo.packageName}: $info" }

            isRemotePlayback =
                info.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE
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
}
