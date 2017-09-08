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
        if (pref.getBoolean("master", false) && controllers != null) {
            for (controller in controllers) {
                val isWhitelisted = pref.getStringSet(Stuff.APP_WHITELIST, setOf()).contains(controller.packageName)
                val isBlacklisted = pref.getStringSet(Stuff.APP_BLACKLIST, setOf()).contains(controller.packageName)
                if (isWhitelisted || (pref.getBoolean(Stuff.AUTO_DETECT_PREF, false) && !isBlacklisted)) {
                    tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
                    if (!mControllers.containsKey(controller.sessionToken)) {
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
        var metadata: MediaMetadata? = null
        var lastHash = 0
        var lastPos: Long = 1
        val isIgnoreArtistMeta = Stuff.APPS_IGNORE_ARTIST_META.contains(packageName)

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            Stuff.log("onMetadataChanged " + metadata?.getString(MediaMetadata.METADATA_KEY_TITLE))
            this.metadata = metadata
            lastPos = 1
        }

        override fun onPlaybackStateChanged(state: PlaybackState) {
            super.onPlaybackStateChanged(state)
            Stuff.log("onPlaybackStateChanged "+ state.state)
            if (metadata == null)
                return

            val title = metadata!!.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata!!.getString(MediaMetadata.METADATA_KEY_ARTIST)

            if (title == "")
                return

            val isWhitelisted = pref.getStringSet(Stuff.APP_WHITELIST, setOf()).contains(packageName)
            val isBlacklisted = pref.getStringSet(Stuff.APP_BLACKLIST, setOf()).contains(packageName)
            val packageNameParam = if (!(isWhitelisted || isBlacklisted)) packageName else null

            if (state.state == PlaybackState.STATE_PAUSED) {
                //                    cancel scrobbling if within time
                lastPos = state.position
                handler.remove(lastHash)
                Stuff.log("paused")
            } else if (state.state == PlaybackState.STATE_STOPPED) {
                // a replay should count as another scrobble
                lastPos = 1
                handler.remove(lastHash)
                Stuff.log("stopped")
            } else if (state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING) {
                if (state.state == PlaybackState.STATE_BUFFERING && state.position.toInt() == 0)
                    return  //dont scrobble first buffering

                Stuff.log("playing: " + state.position + " < " + lastPos + " " + title)
                if (state.position < lastPos || lastPos.toInt() == 1) {
                    //                    lastPos = state.getPosition();
                    if (isIgnoreArtistMeta)
                        lastHash = handler.scrobble(title, packageNameParam)
                    else
                        lastHash = handler.scrobble(artist,title, packageNameParam)
                }
            } else if (state.state == PlaybackState.STATE_CONNECTING) {
                Stuff.log("connecting " + state.position)
            } else {
                //TODO: assume non standard state to scrobble tg. it always gives state 0, and a onMetadataChanged on play/pause
                //TODO: onMetadataChanged (non null), if state==0, and lastHash (excluding null) was submitted > scrobbleDelay then submit again
                //TODO: if onMetadataChanged, data becomes null within scrobbleDelay, cancel it
                Stuff.log("other (" + state.state + ") : " + title)
            }
        }
    }
}
