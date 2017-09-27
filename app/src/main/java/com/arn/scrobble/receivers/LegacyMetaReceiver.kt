package com.arn.scrobble.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.arn.scrobble.Stuff



/**
 * Created by arn on 11-03-2017.
 */

class LegacyMetaReceiver : BroadcastReceiver() {
    private var lastHash: Int = 0

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val cmd = intent.getStringExtra("command")
        val artist = intent.getStringExtra("artist")
        val album = intent.getStringExtra("album")
        val track = intent.getStringExtra("track")
        val duration = intent.getLongExtra("duration", 0)
        Stuff.log( "LegacyMetaReceiver Action $action")
        Stuff.log( "LegacyMetaReceiver Extras: " + intent.extras)
        processIntent(intent)
//        lastHash = NLService.handler.scrobble(artist, track)
    }

    private fun processIntent(intent: Intent) {
        if (intent.action.indexOf('.') == -1)
            Stuff.log("intent action is corrupted: " + intent.action)
        else if (intent.extras == null || intent.extras.size() == 0)
            Stuff.log("intent extras are null or empty, skipping intent")
        else if (isInitialStickyBroadcast)
            Stuff.log("received cached sticky broadcast, won't process it")
        else {
            val isPlaying = getBoolOrNumberAsBoolExtra(intent, null, "playing", "playstate", "isPlaying", "isplaying", "is_playing")
            if (isPlaying == null) {
                Stuff.log("does not contain playing state, ignoring")
            } else {
                /*
                handleTrackIntent.putExtra(EXTRA_PLAYING, isPlaying)
                handleTrackIntent.putExtra(EXTRA_ALBUM_ID, IntentUtil.getLongOrIntExtra(originalIntent, -1, "albumid", "albumId"))
                handleTrackIntent.putExtra(EXTRA_TRACK, originalIntent.getStringExtra("track"))
                handleTrackIntent.putExtra(EXTRA_ARTIST, originalIntent.getStringExtra("artist"))
                handleTrackIntent.putExtra(EXTRA_ALBUM, originalIntent.getStringExtra("album"))

                var duration = IntentUtil.getLongOrIntExtra(originalIntent, -1, "duration")

                if (duration != -1) {
                    if (duration < 30000) { // it is in seconds, we should convert it to millis
                        duration *= 1000
                    }
                }

                handleTrackIntent.putExtra(EXTRA_DURATION, duration)
                */
            }
        }
    }

    private fun getBoolOrNumberAsBoolExtra(intent: Intent?, defaultValue: Boolean?, vararg possibleExtraNames: String): Boolean? {
        if (intent == null || possibleExtraNames.isEmpty()) return defaultValue

        val extras = intent.extras

        if (extras == null || extras.isEmpty) return defaultValue

        for (possibleExtraName in possibleExtraNames) {
            if (extras.containsKey(possibleExtraName)) {
                val obj = extras.get(possibleExtraName)

                when (obj) {
                    is Boolean -> return obj
                    is Int -> return obj > 0
                    is Long -> return obj > 0
                    is Short -> return obj > 0
                    is Byte -> return obj > 0
                    else -> {
                    }
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

        val intentStrings = arrayOf(
                "com.android.music.metachanged",
                "com.android.music.playstatechanged"
        )
    }
}
