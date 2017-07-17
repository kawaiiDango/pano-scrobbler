package com.arn.ytscrobble;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaController.Callback;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.media.session.PlaybackState;
import android.support.annotation.NonNull;
import android.preference.PreferenceManager;
import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by arn on 04/07/2017.
 */

class SessListener implements OnActiveSessionsChangedListener{
    private SharedPreferences pref=null;
    private Context c;
    private NLService.ScrobbleHandler handler;
    private YtCallback ytCallback = new YtCallback();

    private final Map<MediaSession.Token, Pair<MediaController, Callback>> mControllers = new HashMap<>();


    SessListener(Context c, NLService.ScrobbleHandler h){
        this.c = c;
        handler = h;
        pref = PreferenceManager.getDefaultSharedPreferences(c);
    }

    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        int controllerCount = ((null == controllers) ? 0 : controllers.size());
        Set<MediaSession.Token> tokens = new HashSet<>(controllerCount);
        for (int i = 0; i < controllerCount; i++) {
            MediaController controller = controllers.get(i);
            if (pref.getBoolean("scrobble_youtube", true) &&
                    controller.getPackageName().equals( NLService.YOUTUBE_PACKAGE)) {
                tokens.add(controller.getSessionToken());
                // Only add tokens that we don't already have.
                if (!mControllers.containsKey(controller.getSessionToken())) {
                    controller.registerCallback(ytCallback);
                    Pair<MediaController, Callback> pair = Pair.create(controller, (Callback)ytCallback);
                    synchronized (mControllers) {
                        mControllers.put(controller.getSessionToken(), pair);
                    }
                }
            }
        }
        // Now remove old sessions that are not longer active.
        for (Map.Entry<MediaSession.Token, Pair<MediaController, Callback>> entry : mControllers.entrySet()) {
            MediaSession.Token token = entry.getKey();
            if (!tokens.contains(token)) {
                Pair<MediaController, MediaController.Callback> pair = entry.getValue();
                pair.first.unregisterCallback(pair.second);
                synchronized (mControllers) {
                    mControllers.remove(token);
                    handler.remove(ytCallback.lastHash);
                }
            }
        }
    }

    private class YtCallback extends Callback {
        MediaMetadata metadata= null;
        int lastHash = 0;
        long lastPos = 1;

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            Stuff.log(c, "metadata changed " );
            this.metadata = metadata;
            lastPos = 1;
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (metadata == null)
                return;
            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
//                String artist =  metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

            if (title.equals(""))
                return;
            if (state.getState()==PlaybackState.STATE_PAUSED) {
//                    cancel scrobbling if within time
                lastPos = state.getPosition();
                handler.remove(lastHash);
                Stuff.log(c, "paused");
            } else if ( state.getState()==PlaybackState.STATE_STOPPED){
                // a replay should count as another scrobble
                lastPos = 1;
                handler.remove(lastHash);
                Stuff.log(c, "stopped");
            } else if (state.getState()==PlaybackState.STATE_PLAYING || state.getState()==PlaybackState.STATE_BUFFERING){
                if (state.getState()==PlaybackState.STATE_BUFFERING && state.getPosition() == 0)
                    return; //dont scrobble first buffering

                Stuff.log(c, "playing: "+ state.getPosition()+ " < " + lastPos + " " + title);
                if (pref.getBoolean("scrobble_youtube", true) && state.getPosition() < lastPos || lastPos == 1) {
//                    lastPos = state.getPosition();
                    lastHash = handler.scrobble(title);
                }
            } else if (state.getState()==PlaybackState.STATE_CONNECTING){
                Stuff.log(c, "connecting "+ state.getPosition());
            } else
                Stuff.log(c, "other ("+state.getState()+") : " + title);

        }
    }
}
