package com.arn.scrobble

import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaController.Callback
import android.media.session.MediaSession
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Pair
import com.arn.scrobble.receivers.LegacyMetaReceiver

/**
 * Created by arn on 04/07/2017.
 */

class SessListener constructor(private val pref: SharedPreferences,
                               private val handler: NLService.ScrobbleHandler) :
        OnActiveSessionsChangedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val controllersMap = mutableMapOf<MediaSession.Token, Pair<MediaController, MyCallback>>()
    private var controllers : List<MediaController>? = null

    private var blackList = pref.getStringSet(Stuff.PREF_BLACKLIST, setOf())!!
    private val whiteList = mutableSetOf<String>()
    private var autoDetectApps = pref.getBoolean(Stuff.PREF_AUTO_DETECT, true)
    private var scrobblingEnabled = pref.getBoolean(Stuff.PREF_MASTER, true)
    private var loggedIn = pref.getString(Stuff.PREF_LASTFM_SESS_KEY, null) != null

    init {
        pref.registerOnSharedPreferenceChangeListener(this)
        whiteList.addAll(pref.getStringSet(Stuff.PREF_WHITELIST, setOf())!!)
    }

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        this.controllers = controllers
        val tokens = mutableSetOf<MediaSession.Token>()
        if (scrobblingEnabled && controllers != null) {
            for (controller in controllers) {
                if (shouldScrobble(controller.packageName)) {
                    tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
                    if (!controllersMap.containsKey(controller.sessionToken)) {
                        Stuff.log("onActiveSessionsChanged [" + controllers.size + "] : " + controller.packageName)
                        val cb = MyCallback(whiteList, handler, controller.packageName, controller.sessionToken.toString() + ", " + hashCode())
                        controller.registerCallback(cb)
                        if (controller.playbackState != null)
                            cb.onPlaybackStateChanged(controller.playbackState!!) //Melody needs this
                        if (controller.metadata != null)
                            cb.onMetadataChanged(controller.metadata)

                        val pair = Pair.create(controller, cb)
                        synchronized(controllersMap) {
                            controllersMap.put(controller.sessionToken, pair)
                        }
                    }
                }
            }
        }
        // Now remove old sessions that are not longer active.
        removeSessions(tokens)
    }

    fun removeSessions(tokens: MutableSet<*>? = null, packageNames: Set<String>? = null) {
        val it = controllersMap.iterator()
        while (it.hasNext()) {
            val (token, pair) = it.next()
            if ((tokens != null && !tokens.contains(token)) ||
                    (packageNames != null && packageNames.contains(pair.first.packageName))) {
                pair.second.stop()
                pair.first.unregisterCallback(pair.second)
                synchronized(controllersMap) {
                    it.remove()
                }
            }
        }
        numSessions = controllersMap.size
    }

    class MyCallback(private val whiteList: MutableSet<String>, private val handler: NLService.ScrobbleHandler,
                     private val packageName: String, private val who: String) : Callback() {
        var currHash = 0
        var lastScrobblePos = 1L
        var lastScrobbleTime = 0L
        var lastState = -1
        val isIgnoreArtistMeta = Stuff.IGNORE_ARTIST_META.contains(packageName)

        var artist = ""
        var album = ""
        var title = ""
        var duration = -1L

        init {
            lastSessEventTime = System.currentTimeMillis()
        }

        private val stateHandler = object : Handler() {
            override fun handleMessage(msg: Message?) {
                val state: Int = msg?.arg1!!
                val pos: Long = msg.arg2.toLong()

                Stuff.log("onPlaybackStateChanged=$state laststate=$lastState pos=$pos duration=$duration who=$who")

                val isPossiblyAtStart = pos == 0.toLong() ||
                        (pos < Stuff.START_POS_LIMIT && duration > 0 &&
                        System.currentTimeMillis() - lastScrobbleTime - Stuff.START_POS_LIMIT >= duration)

                if (lastState == state /* bandcamp does this */ &&
                        !(state == PlaybackState.STATE_PLAYING && isPossiblyAtStart))
                    return

                if (state != PlaybackState.STATE_BUFFERING)
                    lastState = state

                if (title == "" || artist == "")
                    return

                if (state == PlaybackState.STATE_PAUSED) {
//                    if (duration != 0.toLong() && pos == 0.toLong()) //this breaks phonograph
//                        return
                    if (handler.hasMessages(lastHash)) //if it wasnt scrobbled, consider scrobling again
                        lastScrobblePos = 1
                    else if (pos > Stuff.START_POS_LIMIT){
                        lastScrobblePos = pos
                        //save pos, to use it later in resume
                    }
                    handler.remove(lastHash)
                    Stuff.log("paused")
                } else if (state == PlaybackState.STATE_STOPPED) {
                    // a replay should count as another scrobble. Replay (in youtube app) is stop, buffer, then play
                    lastScrobblePos = 1
                    handler.remove(lastHash)
                    Stuff.log("stopped")
                } else if (state == PlaybackState.STATE_PLAYING
                        // state.state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_NONE
                        ) {
//                if (state.state == PlaybackState.STATE_BUFFERING && state.position == 0.toLong())
//                    return  //dont scrobble first buffering

                    Stuff.log(state.toString() + " playing: pos=$pos, lastScrobblePos=$lastScrobblePos $isPossiblyAtStart $title")
                    if ((isPossiblyAtStart || lastScrobblePos == 1.toLong()) &&
                            (!handler.hasMessages(currHash) ||
                            System.currentTimeMillis() - lastScrobbleTime > 2000)) {
                        Stuff.log("scrobbleit")
                        scrobble(artist, album, title, duration)
                        lastScrobblePos = pos
                    } else
                        handler.removeCallbacksAndMessages(LegacyMetaReceiver.TOKEN)
                } else if (state == PlaybackState.STATE_CONNECTING || state == PlaybackState.STATE_BUFFERING) {
                    Stuff.log("$state connecting $pos")
                } else {
                    Stuff.log("other ($state) : $title")
                }
            }
        }

        fun scrobble(artist: String, album: String, title: String, duration: Long) {
            lastScrobbleTime = System.currentTimeMillis()
            val isWhitelisted = whiteList.contains(packageName)
            val packageNameParam = if (!isWhitelisted) packageName else null
            handler.removeCallbacksAndMessages(LegacyMetaReceiver.TOKEN) //remove all from legacy receiver, to prevent duplicates
            if (isIgnoreArtistMeta)
                lastHash = handler.scrobble(title, duration, packageNameParam)
            else
                lastHash = handler.scrobble(artist, album, title, duration, packageNameParam)
        }

        @Synchronized override fun onMetadataChanged(metadata: MediaMetadata?) {
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?:
                    metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim() ?: ""
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim() ?: ""
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: ""
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1
            val sameAsOld = (artist == this.artist && title == this.title && album == this.album)

            Stuff.log("onMetadataChanged $artist [$album] ~ $title, sameAsOld=$sameAsOld,"+
                    "lastState=$lastState, package=$packageName who=$who")
            if (!sameAsOld) {
                this.artist = artist
                this.album = album
                this.title = title
                this.duration = duration

//                lastSessEventTime = System.currentTimeMillis()
                currHash = artist.hashCode() + title.hashCode()
                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if (artist != "" && title != "" &&
                        !handler.hasMessages(currHash) &&
                        lastState == PlaybackState.STATE_PLAYING)
                    scrobble(artist, album, title, duration)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState) {
            lastSessEventTime = System.currentTimeMillis()
            val msg = stateHandler.obtainMessage(0, state.state, state.position.toInt())
            stateHandler.sendMessageDelayed(msg, Stuff.META_WAIT)
        }

        override fun onSessionDestroyed() {
            Stuff.log("onSessionDestroyed")
            stop()
        }

        fun stop() {
            stateHandler.removeCallbacksAndMessages(null)
            handler.remove(lastHash)
        }
    }

    fun shouldScrobble(packageName: String): Boolean {
        val shouldScrobble = scrobblingEnabled && loggedIn &&
                (whiteList.contains(packageName) ||
                (autoDetectApps && !blackList.contains(packageName)))

        return shouldScrobble
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        when (key){
            Stuff.PREF_WHITELIST -> {
                whiteList.clear()
                whiteList.addAll(pref.getStringSet(key, setOf())!!)
            }
            Stuff.PREF_BLACKLIST -> blackList = pref.getStringSet(key, setOf())!!
            Stuff.PREF_AUTO_DETECT -> autoDetectApps = pref.getBoolean(key, true)
            Stuff.PREF_MASTER -> scrobblingEnabled = pref.getBoolean(key, true)
            Stuff.PREF_LASTFM_SESS_KEY -> loggedIn = pref.getString(Stuff.PREF_LASTFM_SESS_KEY, null) != null
        }
        if (key == Stuff.PREF_WHITELIST ||
                key == Stuff.PREF_BLACKLIST ||
                key == Stuff.PREF_AUTO_DETECT ||
                key == Stuff.PREF_MASTER) {
            onActiveSessionsChanged(controllers)
            Stuff.log("SessListener prefs changed: $key")
        }
    }

    companion object {
        var numSessions = 0
        var lastSessEventTime:Long = 0
        var lastHash = 0
    }
}
