package com.arn.ytscrobble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Created by arn on 11-03-2017.
 */

internal class TrackMetaListener : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val cmd = intent.getStringExtra("command")

        val artist = intent.getStringExtra("artist")
        val album = intent.getStringExtra("album")
        val track = intent.getStringExtra("track")
        Log.i("tag ", "*****Action $action / $cmd | $artist:$album:$track")
        Toast.makeText(context, "TR:" + track, Toast.LENGTH_SHORT).show()
    }
}
