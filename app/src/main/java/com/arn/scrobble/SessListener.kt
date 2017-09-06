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
    private var appListPref: SharedPreferences = c.getSharedPreferences(Stuff.APP_LIST_PREFS, Context.MODE_PRIVATE)
    private val ytCallback = YtCallback()

    private val mControllers = HashMap<MediaSession.Token, Pair<MediaController, Callback>>()

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        if (!pref.getBoolean("master", false))
            return
        //TODO: remove all sessions when turned off
        val controllerCount = controllers?.size ?: 0
        val tokens = HashSet<MediaSession.Token>(controllerCount)
        for (i in 0 until controllerCount) {
            val controller = controllers!![i]
            if (appListPref.getStringSet(Stuff.APP_LIST_PREFS, setOf()).contains(controller.packageName)) {
//                NLService.YOUTUBE_PACKAGE
                tokens.add(controller.sessionToken)
                // Only add tokens that we don't already have.
                if (!mControllers.containsKey(controller.sessionToken)) {
                    controller.registerCallback(ytCallback)
                    val pair = Pair.create(controller, ytCallback as Callback)
                    synchronized(mControllers) {
                        mControllers.put(controller.sessionToken, pair)
                    }
                }
            }
        }
        // Now remove old sessions that are not longer active.
        for ((token, pair) in mControllers) {
            if (!tokens.contains(token)) {
                pair.first.unregisterCallback(pair.second)
                synchronized(mControllers) {
                    mControllers.remove(token)
                    handler.remove(ytCallback.lastHash)
                }
            }
        }
    }
    //TODO: for youtube, youtube music etc (an array in Stuff) and those without artist, parseTitle, else, pass it raw
    private inner class YtCallback : Callback() {
        internal var metadata: MediaMetadata? = null
        internal var lastHash = 0
        internal var lastPos: Long = 1

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

            //                String artist =  metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

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
                    lastHash = handler.scrobble(title)
                }
            } else if (state.state == PlaybackState.STATE_CONNECTING) {
                Stuff.log(c, "connecting " + state.position)
            } else
                Stuff.log(c, "other (" + state.state + ") : " + title)

        }
    }
}
