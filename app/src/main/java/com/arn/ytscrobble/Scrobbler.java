package com.arn.ytscrobble;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.preference.PreferenceManager;
import android.webkit.WebSettings;

import android.os.Handler;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.User;
import de.umass.lastfm.scrobble.ScrobbleResult;


/**
 * Created by arn on 18-03-2017.
 */

class Scrobbler extends AsyncTask<String, String, Object> {
    private Handler handler = null;
    private Context c;
    private static String token  = "";

    Scrobbler(Context c){
        this.c = c;
    }

    @Override
    protected Object doInBackground(String... s) {
        boolean reAuthNeeded = false;
        Session session = null;
        Caller.getInstance().setUserAgent(WebSettings.getDefaultUserAgent(c));
        Caller.getInstance().setDebugMode(true);

        String key = PreferenceManager.getDefaultSharedPreferences(c).getString("sesskey","");

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
            PreferenceManager.getDefaultSharedPreferences(c)
                    .edit().putString("sesskey", session.getKey()).apply();

            if (s[0].equals(Stuff.CHECKAUTH))
                return null;
            else if (s[0].equals(Stuff.GET_RECENTS)){
                PaginatedResult<Track> recents = User.getRecentTracks(session.getUsername(), 1, 5, token);
                return recents;
            }

            //for scrobble data: s[0] = tag, s[1] = artist, s[2] = song

            ScrobbleResult result = null;
            int now = (int) (System.currentTimeMillis() / 1000);
            if (s[0].equals(Stuff.NOW_PLAYING))
                result = Track.updateNowPlaying(s[1], s[2], session);
            else if (s[0].equals(Stuff.SCROBBLE))
                result = Track.scrobble(s[1], s[2], now, session);
            try {
                if (!(result.isSuccessful() && !result.isIgnored()))
                    ((NLService.ScrobbleHandler)handler)
                            .notification(s[1], s[1].hashCode(), Stuff.STATE_NETWORK_ERR, android.R.drawable.stat_notify_error);
            }catch (NullPointerException e){
                publishProgress(s[0] + ": NullPointerException");
            }

        } else
            reAuth();
        // adb shell am start -W -a android.intent.action.VIEW -d "http://maare.ga:10003/auth" com.arn.ytscrobble
        return null;
    }

    private void reAuth() {
        publishProgress("Deleting key");
        PreferenceManager.getDefaultSharedPreferences(c)
                .edit()
                .remove("sesskey")
                .apply();
        token = Authenticator.getToken(Stuff.LAST_KEY);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/api/auth?api_key=" +
                Stuff.LAST_KEY + "&token=" + token));
        c.startActivity(browserIntent);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Stuff.log(c, "progress: " + values[0]);
    }

    @Override
    protected void onPostExecute(Object res) {
        //do stuff
        if (res instanceof PaginatedResult) {
            RecentsFragment.adapter.populate((PaginatedResult<Track>) res);
        }
    }
}
