package com.arn.ytscrobble;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.webkit.WebSettings;

import java.util.ArrayList;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.User;
import de.umass.lastfm.scrobble.ScrobbleResult;


/**
 * Created by arn on 18-03-2017.
 */

class Scrobbler extends AsyncTask<String, Object, Object> {
    private SharedPreferences prefs = null;
    private Handler handler = null;
    private Context c;
    private static String token  = "";
    public static ArrayList<Integer> scrobbledHashes= new ArrayList<>();

    Scrobbler(Context c){
        this(c, null);
    }
    Scrobbler(Context c, Handler h){
        this.c = c;
        this.handler = h;
        prefs = PreferenceManager.getDefaultSharedPreferences(c);
    }
    @Override
    protected Object doInBackground(String... s) {
        boolean reAuthNeeded = false;
        Session session = null;
        Caller.getInstance().setUserAgent(WebSettings.getDefaultUserAgent(c));
        Caller.getInstance().setDebugMode(true);

        String key = prefs.getString("sesskey","");
        String username = prefs.getString("username", null);

        if (key.length() < 5 && token.length() >5) {
            session = Authenticator.getSession(token, Stuff.LAST_KEY, Stuff.LAST_SECRET);
            if (session != null) {
                username = session.getUsername();
                prefs.edit()
                        .putString("username", username)
                        .apply();
            }
        } else if (key.length() > 5 )
            session = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, key);
        else
            reAuthNeeded = true;

        if (session == null || username == null)
            reAuthNeeded = true;

        if(!reAuthNeeded) {
            //publishProgress("sess_key: " + session.getKey());
            prefs.edit().putString("sesskey", session.getKey()).apply();

            if (s[0].equals(Stuff.CHECKAUTH))
                return null;
            else if (s[0].equals(Stuff.GET_RECENTS)) {
                publishProgress(User.getRecentTracks(username, Stuff.LAST_KEY));
                return User.getLovedTracks(username, Stuff.LAST_KEY);
            } else if (s[0].equals(Stuff.GET_LOVED)){
                return User.getLovedTracks(username, Stuff.LAST_KEY);
            } else if (s[0].equals(Stuff.LOVE)){
                return Track.love(s[1], s[2], session);
            } else if (s[0].equals(Stuff.UNLOVE)){
                return Track.unlove(s[1], s[2], session);
            }

            //for scrobble or love data: s[0] = tag, s[1] = artist, s[2] = song

            ScrobbleResult result = null;
            int now = (int) (System.currentTimeMillis() / 1000);
            if (s[0].equals(Stuff.NOW_PLAYING))
                result = Track.updateNowPlaying(s[1], s[2], session);
            else if (s[0].equals(Stuff.SCROBBLE))
                result = Track.scrobble(s[1], s[2], now, session);
            try {
                if (result != null && !(result.isSuccessful() && !result.isIgnored())) {
                    int hash = s[1].hashCode() + s[2].hashCode();
                    scrobbledHashes.add(hash);
                    ((NLService.ScrobbleHandler) handler)
                            .notification(s[1], s[2], hash, Stuff.STATE_NETWORK_ERR, android.R.drawable.stat_notify_error);
                }
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
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);
        Object val = values[0];
        if (val instanceof PaginatedResult)
            RecentsFragment.adapter.populate((PaginatedResult<Track>) val);
        else if (val instanceof String)
            Stuff.log(c, values[0].toString());
    }

    @Override
    protected void onPostExecute(Object res) {
        //do stuff
        if (res instanceof PaginatedResult) {
            RecentsFragment.adapter.markLoved((PaginatedResult<Track>) res);
        } else if (res instanceof Result){
            if (((Result) res).isSuccessful())
                Stuff.log(c, "ok");
            else
                Stuff.log(c, "failed");
        }
    }
}
