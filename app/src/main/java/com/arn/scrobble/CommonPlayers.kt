package com.arn.scrobble

import android.content.Context
import android.content.IntentFilter
import com.arn.scrobble.receivers.TrackMetaListener

/**
 * Created by arn on 04/09/2017.
 */

object CommonPlayers{
    fun regIntents(c: Context): TrackMetaListener {
        val iF = IntentFilter()
        intentPrefixes.forEach { pre ->
            intentSuffixes.forEach { suf -> iF.addAction(pre+ "." + suf) }
        }

        val tReceiver = TrackMetaListener()
        c.registerReceiver(tReceiver, iF)
        return tReceiver
    }

    val intentPrefixes = arrayOf(
            "com.android.music",
            "fm.last.android",
            "com.sec.android.app.music",
            "com.real.IMP",

            "com.amazon.mp3",
            "com.andrew.appolo",
            "com.htc.music",
            "com.lge.music",
            "com.miui.player",
            "com.jetappfactory.jetaudio",
            "com.tbig.playerpro",
            "com.tbig.playerprotrial",
            "com.rdio.android",
            "com.nullsoft.winamp",

            "com.samsung.sec",
            "com.samsung.sec.android",
            "com.samsung.sec.android.MusicPlayer",
            "com.samsung.music",
            "com.samsung.MusicPlayer"

//            "com.maxmpz.audioplayer" needs Intent.hasExtra("com.maxmpz.audioplayer.source")
    //"com.sonyericsson.music" needs a . and idk


    )
    val intentSuffixes = arrayOf("metachanged", "playstatechanged")
}
