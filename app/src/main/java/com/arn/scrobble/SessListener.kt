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

import java.util.HashMap
import java.util.HashSet

/**
 * Created by arn on 04/07/2017.
 */

class SessListener internal constructor(private val c: Context, private val handler: NLService.ScrobbleHandler) : OnActiveSessionsChangedListener {
    private var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(c)
//    private val ytCallback = YtCallback()

    private val mControllers = mutableMapOf<MediaSession.Token, Pair<MediaController, YtCallback>>()

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        if (!pref.getBoolean("master", false))
            return
        //TODO: remove all sessions when turned off
        val controllerCount = controllers?.size ?: 0
        val tokens = mutableSetOf<MediaSession.Token>()
        for (i in 0 until controllerCount) {
            val controller = controllers!![i]
            if (pref.getStringSet(Stuff.APP_LIST_PREFS, setOf()).contains(controller.packageName)) {
                tokens.add(controller.sessionToken)
                // Only add tokens that we don't already have.
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

    private inner class YtCallback(packageName: String) : Callback() {
        var metadata: MediaMetadata? = null
        var lastHash = 0
        var lastPos: Long = 1
        var isIgnoreArtistMeta = Stuff.APPS_IGNORE_ARTIST_META.contains(packageName)

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            Stuff.log(c, "metadata changed ")
            this.metadata = metadata
            lastPos = 1
        }

        override fun onPlaybackStateChanged(state: PlaybackState) {
            super.onPlaybackStateChanged(state)
            if (metadata == null)
                return
            val title = metadata!!.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata!!.getString(MediaMetadata.METADATA_KEY_ARTIST)

            if (title == "")
                return
            if (state.state == PlaybackState.STATE_PAUSED) {
                //                    cancel scrobbling if within time
                lastPos = state.position
                handler.remove(lastHash)
                Stuff.log(c, "paused")
            } else if (state.state == PlaybackState.STATE_STOPPED) {
                // a replay should count as another scrobble
                lastPos = 1
                handler.remove(lastHash)
                Stuff.log(c, "stopped")
            } else if (state.state == PlaybackState.STATE_PLAYING || state.state == PlaybackState.STATE_BUFFERING) {
                if (state.state == PlaybackState.STATE_BUFFERING && state.position.toInt() == 0)
                    return  //dont scrobble first buffering

                Stuff.log(c, "playing: " + state.position + " < " + lastPos + " " + title)
                if (pref.getBoolean("scrobble_youtube", true) && state.position < lastPos || lastPos.toInt() == 1) {
                    //                    lastPos = state.getPosition();
                    if (isIgnoreArtistMeta)
                        lastHash = handler.scrobble(title)
                    else
                        lastHash = handler.scrobble(artist,title)
                }
            } else if (state.state == PlaybackState.STATE_CONNECTING) {
                Stuff.log(c, "connecting " + state.position)
            } else
                Stuff.log(c, "other (" + state.state + ") : " + title)
        }
    }
}
