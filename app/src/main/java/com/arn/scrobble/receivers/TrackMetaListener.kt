package com.arn.scrobble.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arn.scrobble.Stuff

/**
 * Created by arn on 11-03-2017.
 */

class TrackMetaListener : BroadcastReceiver() {
    private var lastHash: Int = 0

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val cmd = intent.getStringExtra("command")

        val artist = intent.getStringExtra("artist")
        val album = intent.getStringExtra("album")
        val track = intent.getStringExtra("track")
        val duration = intent.getLongExtra("duration", 0)
        Stuff.log( context, "*****Action $action / $cmd | $artist:$album:$track:$duration")
        Stuff.log( context, "*****Data: " + intent.extras)

//        lastHash = NLService.handler.scrobble(artist, track)
    }

}
