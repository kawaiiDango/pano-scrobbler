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

/**
 * Created by arn on 04/07/2017.
 */

class SessListener constructor(private val pref: SharedPreferences,
                               private val handler: NLService.ScrobbleHandler) :
        OnActiveSessionsChangedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val controllersMap = mutableMapOf<MediaSession.Token, Pair<MediaController, MyCallback>>()
    private var controllers : List<MediaController>? = null

    private var blackList = pref.getStringSet(Stuff.PREF_BLACKLIST, setOf())
    private var whiteList = pref.getStringSet(Stuff.PREF_WHITELIST, setOf())
    private var autoDetectApps = pref.getBoolean(Stuff.PREF_AUTO_DETECT, true)

    init {
        pref.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        this.controllers = controllers
        val tokens = mutableSetOf<MediaSession.Token>()
        if (pref.getBoolean(Stuff.PREF_MASTER, true) && controllers != null) {
            for (controller in controllers) {
                if (shouldScrobble(controller.packageName)) {
                    tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
                    if (!controllersMap.containsKey(controller.sessionToken)) {
                        Stuff.log("onActiveSessionsChanged: " + controller.packageName +
                                " #" + controller.sessionToken.describeContents())
                        val cb = MyCallback(pref, handler, controller.packageName)
                        controller.registerCallback(cb)
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
                pair.first.unregisterCallback(pair.second)
                synchronized(controllersMap) {
                    it.remove()
                    handler.remove(lastHash)
                }
            }
        }
        numSessions = controllersMap.size
    }

    class MyCallback(private val pref: SharedPreferences, private val handler: NLService.ScrobbleHandler,
                     private val packageName: String) : Callback() {
        var metadata: MediaMetadata? = null
        //        var lastHash = 0
        var lastScrobblePos: Long = 1
        var lastScrobbleTime: Long = 0
        var lastState = -1
        val isIgnoreArtistMeta = Stuff.APPS_IGNORE_ARTIST_META.contains(packageName)

        private val stateHandler = object : Handler() {
            override fun handleMessage(msg: Message?) {
                val state: Int = msg?.arg1!!
                val pos: Long = msg.arg2.toLong()

                Stuff.log("onPlaybackStateChanged=" + state + " laststate=" + lastState +
                        " pos=" + pos + " duration=" +
                        metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION))

                val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1
                val isPossiblyAtStart = pos == 0.toLong() ||
                        (pos < 1500 && duration > 0 && System.currentTimeMillis() - lastScrobbleTime >= duration)

                if (lastState == state /* bandcamp does this */ &&
                        !(state == PlaybackState.STATE_PLAYING && isPossiblyAtStart))
                    return

                if (state != PlaybackState.STATE_BUFFERING)
                    lastState = state

                val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
                val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                val artist = (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?:
                        metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)) ?: return

                if (title == "")
                    return

                if (state == PlaybackState.STATE_PAUSED) {
//                    if (duration != 0.toLong() && pos == 0.toLong()) //this breaks phonograph
//                        return
                    if (handler.hasMessages(lastHash)) //if it wasnt scrobbled, consider scrobling again
                        lastScrobblePos = 1
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

                    Stuff.log(state.toString() + " playing: pos=$pos, lastScrobblePos=$lastScrobblePos $title")
                    if (isPossiblyAtStart || (lastScrobblePos == 1.toLong())) {
                        scrobble(artist, album, title, duration)
                        lastScrobblePos = pos
                        lastScrobbleTime = System.currentTimeMillis()
                    }
                } else if (state == PlaybackState.STATE_CONNECTING || state == PlaybackState.STATE_BUFFERING) {
                    Stuff.log("$state connecting $pos")
                } else {
                    Stuff.log("other ($state) : $title")
                }
            }
        }

        fun scrobble(artist: String, album: String, title: String, duration: Long) {
            val isWhitelisted = pref.getStringSet(Stuff.PREF_WHITELIST, setOf()).contains(packageName)
//            val isBlacklisted = pref.getStringSet(Stuff.PREF_BLACKLIST, setOf()).contains(packageName)
            val packageNameParam = if (!isWhitelisted) packageName else null

            if (isIgnoreArtistMeta)
                lastHash = handler.scrobble(title, duration, packageNameParam)
            else
                lastHash = handler.scrobble(artist, album, title, duration, packageNameParam)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
//            super.onMetadataChanged(metadata)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val artist2 = this.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val title2 = this.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
            val album2 = this.metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1
            val sameAsOld = (artist == artist2 && title == title2 && album == album2)
            Stuff.log("onMetadataChanged $artist [$album] ~ $title, sameAsOld=$sameAsOld,"+
                    "lastState=$lastState, package=$packageName")
            if (!sameAsOld) {
                this.metadata = metadata

                // for cases:
                // - meta is sent after play
                // - "gapless playback", where playback state never changes
                if (artist != "" && title != "" &&
                        !handler.hasMessages(artist.hashCode() + title.hashCode()) &&
                        lastState == PlaybackState.STATE_PLAYING)
                    scrobble(artist, album, title, duration)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState) {
//            super.onPlaybackStateChanged(state)
            lastStateChangedTime = System.currentTimeMillis()
            val msg = stateHandler.obtainMessage(0, state.state, state.position.toInt())
            stateHandler.sendMessageDelayed(msg, Stuff.META_WAIT)
        }
    }

    fun shouldScrobble(packageName: String): Boolean {
        val shouldScrobble = whiteList.contains(packageName) ||
                (autoDetectApps && !blackList.contains(packageName))

        return shouldScrobble
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        when (key){
            Stuff.PREF_WHITELIST -> whiteList = pref.getStringSet(key, setOf())
            Stuff.PREF_BLACKLIST -> blackList = pref.getStringSet(key, setOf())
            Stuff.PREF_AUTO_DETECT -> autoDetectApps = pref.getBoolean(key, true)
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
        var lastStateChangedTime:Long = 0
        var lastHash = 0
    }
}
