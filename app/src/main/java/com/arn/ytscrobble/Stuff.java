package com.arn.ytscrobble;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by arn on 13-03-2017.
 */

class Stuff {
    static final String NOW_PLAYING = "np", SCROBBLE = "scrobble", CHECKAUTH = "auth";
    static String LAST_KEY = Tokens.LAST_KEY,
        LAST_SECRET = Tokens.LAST_SECRET;
    static final String TAG = "ytscrobble", PREFS = "prefs";

    static void log(Context c, String s){
        Log.i(TAG,s);
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

}
