package com.arn.ytscrobble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by arn on 11-03-2017.
 */

class TrackMetaListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String cmd = intent.getStringExtra("command");

        String artist = intent.getStringExtra("artist");
        String album = intent.getStringExtra("album");
        String track = intent.getStringExtra("track");
        Log.i("tag ", "*****Action " + action + " / " + cmd +" | "+ artist + ":" + album + ":" + track);
        Toast.makeText(context, "TR:" + track, Toast.LENGTH_SHORT).show();
    }
}
