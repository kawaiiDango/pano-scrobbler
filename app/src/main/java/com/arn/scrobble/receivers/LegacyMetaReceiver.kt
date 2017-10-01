package com.arn.scrobble.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.arn.scrobble.NLService
import com.arn.scrobble.SessListener
import com.arn.scrobble.Stuff



/**
 * Created by arn on 11-03-2017.
 */

class LegacyMetaReceiver : BroadcastReceiver() {
    private var lastHash: Int = 0

    override fun onReceive(context: Context, intent: Intent) {
            processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        if (intent.action.indexOf('.') == -1)
            Stuff.log("intent action is corrupted: " + intent.action)
        else if (intent.extras == null || intent.extras.size() == 0)
            Stuff.log("intent extras are null or empty, skipping intent")
        else if (isInitialStickyBroadcast)
            Stuff.log("received cached sticky broadcast, won't process it")
        else {
            Stuff.log( "LegacyMetaReceiver "+ intent.action +": " + Stuff.bundleDump(intent.extras))

            val isPlaying = getBoolOrNumberAsBoolExtra(intent, null, "playing", "playstate", "isPlaying", "isplaying", "is_playing")
            if (isPlaying == null) {
                Stuff.log("does not contain playing state, ignoring")
            } else {
                val artist = intent.getStringExtra("artist")
                val album = intent.getStringExtra("album") ?: ""
                val track = intent.getStringExtra("track")
//                val positionAny: Any? = intent.extras["position"] // position in blackplayer is a lie

                if (artist == null || artist == "" || track == null || track == "" )
                    return

                val durationAny: Any? = intent.extras["duration"]
                var duration: Long = durationAny as? Long ?: if(durationAny is Int)
                    durationAny.toLong()
                else
                    0
                if (duration in 1..30000) { // it is in seconds, we should convert it to millis
                    duration *= 1000
                }

                val hash = artist.hashCode() + track.hashCode()
                try{
                    NLService.handler.postDelayed({
                        if (SessListener.numSessions ==0 || System.currentTimeMillis() - SessListener.lastStateChangedTime < Stuff.META_WAIT*2)
                            return@postDelayed

                        if (isPlaying && !NLService.handler.hasMessages(hash)) {
                            lastHash = NLService.handler.scrobble(artist, album, track, duration)
                            Stuff.log( "LegacyMetaReceiver scrobbling $track")
                            Stuff.log("timeDiff="+ (System.currentTimeMillis() - SessListener.lastStateChangedTime) +
                            ", numSessions="+SessListener.numSessions)

                        } else if (!isPlaying && NLService.handler.hasMessages(hash)) {
                            NLService.handler.remove(lastHash)
                            Stuff.log( "LegacyMetaReceiver cancelled "+ hash)
                        }
                    }, Stuff.META_WAIT+200)
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getBoolOrNumberAsBoolExtra(intent: Intent?, defaultValue: Boolean?, vararg possibleExtraNames: String): Boolean? {
        if (intent == null || possibleExtraNames.isEmpty()) return defaultValue

        val extras = intent.extras

        if (extras == null || extras.isEmpty) return defaultValue

        possibleExtraNames
                .filter { extras.containsKey(it) }
                .map { extras.get(it) }
                .forEach {
                    when (it) {
                        is Boolean -> return it
                        is Int -> return it > 0
                        is Long -> return it > 0
                        is Short -> return it > 0
                        is Byte -> return it > 0
                        else -> {
                        }
                    }
                }

        return defaultValue
    }

    companion object {
        fun regIntents(c: Context): LegacyMetaReceiver {
            val iF = IntentFilter()
            intentStrings.forEach {
                iF.addAction(it)
            }

            val tReceiver = LegacyMetaReceiver()
            c.registerReceiver(tReceiver, iF)
            return tReceiver
        }

        private val intentStrings = arrayOf(
                "com.android.music.metachanged",
                "com.android.music.playstatechanged"
        )
    }
}
