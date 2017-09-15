package com.arn.scrobble

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaController.Callback
import android.media.session.MediaSession
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import android.preference.PreferenceManager
import android.util.Pair

/**
 * Created by arn on 04/07/2017.
 */

class SessListener internal constructor(private val c: Context, private val handler: NLService.ScrobbleHandler) : OnActiveSessionsChangedListener {
    private var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(c)
//    private val ytCallback = YtCallback()

    private val mControllers = mutableMapOf<MediaSession.Token, Pair<MediaController, YtCallback>>()

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        val tokens = mutableSetOf<MediaSession.Token>()
        if (pref.getBoolean("master", true) && controllers != null) {
            for (controller in controllers) {
                val isWhitelisted = pref.getStringSet(Stuff.APP_WHITELIST, setOf()).contains(controller.packageName)
                val isBlacklisted = pref.getStringSet(Stuff.APP_BLACKLIST, setOf()).contains(controller.packageName)
                if (isWhitelisted || (pref.getBoolean(Stuff.AUTO_DETECT_PREF, false) && !isBlacklisted)) {
                    tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
                    if (!mControllers.containsKey(controller.sessionToken)) {
//                        controller.m
                        val cb = YtCallback(controller.packageName)
                        controller.registerCallback(cb)
                        val pair = Pair.create(controller, cb)
                        synchronized(mControllers) {
                            mControllers.put(controller.sessionToken, pair)
                        }
                    }
                }
            }
        }
        // Now remove old sessions that are not longer active.
        val it = mControllers.iterator()
        while (it.hasNext()) {
            val (token, pair) = it.next()
            if (!tokens.contains(token)) {
                pair.first.unregisterCallback(pair.second)
                synchronized(mControllers) {
                    it.remove()
                    handler.remove(pair.second.lastHash)
                }
            }
        }
    }

    private inner class YtCallback(val packageName: String) : Callback() {
        val DEBOUNCE_TIME = 100
        var metadata: MediaMetadata? = null
        var lastHash = 0
        var lastPos: Long = 1
        var lastScrobbleTime: Long = 1
        var lastState = -1
        val isIgnoreArtistMeta = Stuff.APPS_IGNORE_ARTIST_META.contains(packageName)

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            Stuff.log("onMetadataChanged " + metadata?.getString(MediaMetadata.METADATA_KEY_TITLE))
            this.metadata = metadata
            lastPos = 1
            lastScrobbleTime = 1
            lastState = -1
            //TODO: sometimes onMetadataChanged gets called after onPlaybackStateChanged. use settimeout for everything if onMetadataChanged time is far less than onPlaybackStateChanged time
        }

        override fun onPlaybackStateChanged(state: PlaybackState) {
            Stuff.log("onPlaybackStateChanged "+ state.state + " laststate "+ lastState +
                    " pos "+ state.position + " extras " + state.extras +" duration "+
                    metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION))

            super.onPlaybackStateChanged(state)
            val isAtStart = state.position == 0.toLong()
            if (lastState == state.state && !(state.state == PlaybackState.STATE_PLAYING && isAtStart)) // bandcamp does this
                return

            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: return
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1

            if (title == "")
                return
            if (state.state != PlaybackState.STATE_BUFFERING)
                lastState = state.state

            val isWhitelisted = pref.getStringSet(Stuff.APP_WHITELIST, setOf()).contains(packageName)
            val isBlacklisted = pref.getStringSet(Stuff.APP_BLACKLIST, setOf()).contains(packageName)
            val packageNameParam = if (!(isWhitelisted || isBlacklisted)) packageName else null

            if (state.state == PlaybackState.STATE_PAUSED) {
                if (duration != 0.toLong() && state.position == 0.toLong())
                    return
                //cancel scrobbling if within time
                lastPos = state.position
                handler.remove(lastHash)
                Stuff.log("paused")
            } else if (state.state == PlaybackState.STATE_STOPPED) {
                // a replay should count as another scrobble. Replay (in youtube app) is stop, buffer, then play
                lastPos = 1
                lastScrobbleTime = 1
                handler.remove(lastHash)
                Stuff.log("stopped")
            } else if (state.state == PlaybackState.STATE_PLAYING ||
//                    state.state == PlaybackState.STATE_BUFFERING ||
                    state.state == PlaybackState.STATE_NONE) {
//                if (state.state == PlaybackState.STATE_BUFFERING && state.position == 0.toLong())
//                    return  //dont scrobble first buffering

                Stuff.log(state.state.toString() + " playing: " + state.position + " < " + lastPos + " " + title)
                if (isAtStart ||
                        (state.position - lastPos < DEBOUNCE_TIME && state.position - lastScrobbleTime < DEBOUNCE_TIME)){
                    if(pref.getBoolean(Stuff.OFFLINE_SCROBBLE_PREF, true) || Stuff.isNetworkAvailable(c)) {
                        if (isAtStart) //scrobble replays
                            handler.remove(lastHash)
                        if (isIgnoreArtistMeta)
                            lastHash = handler.scrobble(title, duration, packageNameParam)
                        else
                            lastHash = handler.scrobble(artist, title, duration, packageNameParam)
                    }
                    lastScrobbleTime = state.position
                }
            } else if (state.state == PlaybackState.STATE_CONNECTING || state.state == PlaybackState.STATE_BUFFERING) {
                Stuff.log(state.state.toString() +"connecting " + state.position)
            } else {
                //TODO: assume non standard state to scrobble tg. it always gives state 0, and a onMetadataChanged on play/pause
                //TODO: onMetadataChanged (non null), if state==0, and lastHash (excluding null) was submitted > scrobbleDelay then submit again
                //TODO: if onMetadataChanged, data becomes null within scrobbleDelay, cancel it
                Stuff.log("other (" + state.state + ") : " + title)
            }
        }
    }
}
