package com.arn.scrobble

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaController.Callback
import android.media.session.MediaSession
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.util.Pair

/**
 * Created by arn on 04/07/2017.
 */

class SessListener internal constructor(private val c: Context, private val handler: NLService.ScrobbleHandler) : OnActiveSessionsChangedListener {
    private var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(c)

    private val mControllers = mutableMapOf<MediaSession.Token, Pair<MediaController, MyCallback>>()

    override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        val tokens = mutableSetOf<MediaSession.Token>()
        if (pref.getBoolean("master", true) && controllers != null) {
            for (controller in controllers) {
                val isWhitelisted = pref.getStringSet(Stuff.APP_WHITELIST, setOf()).contains(controller.packageName)
                val isBlacklisted = pref.getStringSet(Stuff.APP_BLACKLIST, setOf()).contains(controller.packageName)
                if (isWhitelisted || (pref.getBoolean(Stuff.AUTO_DETECT_PREF, false) && !isBlacklisted)) {
                    tokens.add(controller.sessionToken) // Only add tokens that we don't already have.
                    if (!mControllers.containsKey(controller.sessionToken)) {
                        Stuff.log("onActiveSessionsChanged: "+controller.packageName+" #"+controller.sessionToken)
                        val cb = MyCallback(controller.packageName)
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
        removeSessions(tokens)
    }

    fun removeSessions(tokens:MutableSet<*>? = null, packageName: String? = null){
        val it = mControllers.iterator()
        while (it.hasNext()) {
            val (token, pair) = it.next()
            if ((tokens != null && !tokens.contains(token)) ||
                    (packageName != null && pair.first.packageName == packageName)) {
                pair.first.unregisterCallback(pair.second)
                synchronized(mControllers) {
                    it.remove()
                    handler.remove(pair.second.lastHash)
                }
            }
        }
        numSessions = mControllers.size
    }

    private inner class MyCallback(val packageName: String) : Callback() {
        var metadata: MediaMetadata? = null
        var lastHash = 0
        var lastScrobblePos: Long = 1
        var lastState = -1
        val isIgnoreArtistMeta = Stuff.APPS_IGNORE_ARTIST_META.contains(packageName)

        private val stateHandler = object : Handler(){
            override fun handleMessage(msg: Message?) {
                val state: Int = msg?.arg1!!
                val pos: Long = msg.arg2.toLong()

                Stuff.log("onPlaybackStateChanged="+ state + " laststate="+ lastState +
                        " pos="+ pos +" duration="+
                        metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION))

                val isAtStart = pos == 0.toLong()
                if (lastState == state /* bandcamp does this */ &&
                        !(state == PlaybackState.STATE_PLAYING && isAtStart))
                    return

                val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
                val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
                val artist = (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?:
                        metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)) ?: return
                val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1

                if (title == "")
                    return
                if (state != PlaybackState.STATE_BUFFERING)
                    lastState = state

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
                } else if (state == PlaybackState.STATE_PLAYING ||
                        //                    state.state == PlaybackState.STATE_BUFFERING ||
                        state == PlaybackState.STATE_NONE) {
//                if (state.state == PlaybackState.STATE_BUFFERING && state.position == 0.toLong())
//                    return  //dont scrobble first buffering

                    Stuff.log(state.toString() + " playing: pos=$pos, lastScrobblePos=$lastScrobblePos $title")
                    if (isAtStart || (lastScrobblePos == 1.toLong())){
                        if(pref.getBoolean(Stuff.OFFLINE_SCROBBLE_PREF, true) || Stuff.isNetworkAvailable(c)) {
                            scrobble(artist, album, title, duration)
                        }
                        lastScrobblePos = pos
                    }
                } else if (state == PlaybackState.STATE_CONNECTING || state == PlaybackState.STATE_BUFFERING) {
                    Stuff.log("$state connecting $pos")
                } else {
                    Stuff.log("other ($state) : $title")
                }
            }
        }

        fun scrobble(artist:String, album:String, title:String, duration:Long){
            val isWhitelisted = pref.getStringSet(Stuff.APP_WHITELIST, setOf()).contains(packageName)
            val isBlacklisted = pref.getStringSet(Stuff.APP_BLACKLIST, setOf()).contains(packageName)
            val packageNameParam = if (!(isWhitelisted || isBlacklisted)) packageName else null

            if (isIgnoreArtistMeta)
                lastHash = handler.scrobble(title, duration, packageNameParam)
            else
                lastHash = handler.scrobble(artist, album, title, duration, packageNameParam)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val artist2 = this.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val title2 = this.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            val album2 = this.metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: -1
            val sameAsOld = (artist == artist2 && title == title2 && album == album2)
            Stuff.log("onMetadataChanged $artist ~ $title, sameAsOld=$sameAsOld, package=$packageName")
            if (!sameAsOld) {
                this.metadata = metadata
                lastScrobblePos = 1
                lastState = -1

                //for cases when meta is sent after play
                if (artist != null && album != null && title != null && handler.hasMessages(lastHash))
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
    companion object {
        var numSessions = 0
        var lastStateChangedTime:Long = 0
    }
}
