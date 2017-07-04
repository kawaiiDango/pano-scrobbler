package com.arn.ytscrobble;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.webkit.WebSettings;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.scrobble.ScrobbleResult;
import de.umass.lastfm.Track;


/**
 * Created by arn on 18-03-2017.
 */

public class Scrobbler extends AsyncTask<String, String, String> {
    Context con;
    static String token  = "";

    public Scrobbler(Context c){
        con = c;
    }

    @Override
    protected String doInBackground(String... s) {
        boolean reAuthNeeded = false;
        Session session = null;
        Caller.getInstance().setUserAgent(WebSettings.getDefaultUserAgent(con));
        Caller.getInstance().setDebugMode(true);

        String key = con.getSharedPreferences(Stuff.PREFS, Context.MODE_PRIVATE).getString("sesskey","");

        if (key.length() < 5 && token.length() >5)
            session = Authenticator.getSession(token, Stuff.LAST_KEY, Stuff.LAST_SECRET);
        else if (key.length() > 5 )
            session = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, key);
        else
            reAuthNeeded = true;
        if (session == null)
            reAuthNeeded = true;

        if(!reAuthNeeded) {
            //publishProgress("sess_key: " + session.getKey());
            con.getSharedPreferences(Stuff.PREFS, Context.MODE_PRIVATE)
                    .edit().putString("sesskey", session.getKey()).apply();

            if (s[0].equals(Stuff.CHECKAUTH))
                return null;

            ScrobbleResult result = null;
            String splits[] = s[1].split(" [-/] ");
            if (splits.length == 2){
                int now = (int) (System.currentTimeMillis() / 1000);
                if (s[0].equals(Stuff.NOW_PLAYING))
                    result = Track.updateNowPlaying(splits[0], splits[1], session);
                else if (s[0].equals(Stuff.SCROBBLE))
                    result = Track.scrobble(splits[0], splits[1], now, session);

                publishProgress(s[0]+ " was ok: " + (result.isSuccessful() && !result.isIgnored()));
            } else
                publishProgress("couldnt parse " + splits[0]);
        } else
            reAuth();
        // adb shell am start -W -a android.intent.action.VIEW -d "http://maare.ga:10003/auth" com.arn.ytscrobble
        return null;
    }

    private void reAuth() {
        publishProgress("Deleting key");
        con.getSharedPreferences(Stuff.PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove("sesskey")
                .apply();
        token = Authenticator.getToken(Stuff.LAST_KEY);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/api/auth?api_key=" +
                Stuff.LAST_KEY + "&token=" + token));
        con.startActivity(browserIntent);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Stuff.log(con, "progress: " + values[0]);
    }

}
