package com.arn.scrobble.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BadParcelableException
import android.os.SystemClock
import com.arn.scrobble.KeepNLSAliveJob
import com.arn.scrobble.NLService
import com.arn.scrobble.SessListener
import com.arn.scrobble.Stuff



/**
 * Created by arn on 11-03-2017.
 */

class LegacyMetaReceiver : BroadcastReceiver() {
//    private var lastHash: Int = 0
    private var serviceRunningCheckTime = 0L

    override fun onReceive(context: Context, intent: Intent) {
        try {
//            scrobbling_source for poweramp
            if (intent.hasExtra(Stuff.IGNORE_LEGAGY_META[0] + ".source"))
                return
        } catch (e: BadParcelableException) {
            return
        }
        if (System.currentTimeMillis() - serviceRunningCheckTime > Stuff.RECENTS_REFRESH_INTERVAL * 4) {
            KeepNLSAliveJob.ensureServiceRunning(context)
            serviceRunningCheckTime = System.currentTimeMillis()
        }
        processIntent(intent, context)
    }

    private fun processIntent(intent: Intent, context: Context) {
        if (intent.action?.indexOf('.') == -1)
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
                val artist = intent.getStringExtra("artist")?.trim()
                val album = intent.getStringExtra("album")?.trim() ?: ""
                val track = intent.getStringExtra("track")?.trim()
//                val positionAny: Any? = intent.extras["position"] // position in blackplayer is a lie

                if (artist == null || artist == "" || track == null || track == "" )
                    return
/*
                val packageName =
                        intent.getStringExtra("package") ?:
                                intent.getStringExtra("packageName") ?:
                                intent.getStringExtra("app-package") ?:
                                intent.getStringExtra("scrobbling_source")
                if (packageName != null){
                    val pref = PreferenceManager.getDefaultSharedPreferences(context)
                    val isWhitelisted = pref.getStringSet(Stuff.PREF_WHITELIST, setOf()).contains(packageName)
//                    val isBlacklisted = pref.getStringSet(Stuff.PREF_BLACKLIST, setOf()).contains(packageName)
                    if (!isWhitelisted)
                        return
                }
*/
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
                    NLService.handler.postAtTime({
                        val timeDiff = System.currentTimeMillis() - SessListener.lastSessEventTime

                        if (SessListener.numSessions == 0 || timeDiff > Stuff.META_WAIT * 2.5 )
                            return@postAtTime

                        Stuff.log("LegacyMetaReceiver numSessions: " + SessListener.numSessions + " timeDiff: " + timeDiff)

                        if (isPlaying && !NLService.handler.hasMessages(hash)) {
                            SessListener.lastHash = NLService.handler.scrobble(artist, album, track, duration)
                            Stuff.log( "LegacyMetaReceiver scrobbling $track")

                        } else if (!isPlaying && NLService.handler.hasMessages(hash)) {
                            NLService.handler.remove(SessListener.lastHash)
                            Stuff.log("LegacyMetaReceiver cancelled $hash")
                        }
                    }, TOKEN,
                            SystemClock.uptimeMillis() + Stuff.META_WAIT * 2)
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
        const val TOKEN = 95
    }
}
