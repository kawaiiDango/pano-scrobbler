package com.arn.ytscrobble;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
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

public class SessListener implements OnActiveSessionsChangedListener{
    private SharedPreferences pref=null;
    Context c;
    NLService.ScrobbleHandler handler;
    Map<MediaSession.Token, Pair<MediaController, MediaController.Callback>> mControllers = new HashMap<>();


    public SessListener(Context c, NLService.ScrobbleHandler h){
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
                    MediaController.Callback callback = mCallback;
                    controller.registerCallback(callback);
                    Pair<MediaController, MediaController.Callback> pair = Pair.create(controller, callback);
                    synchronized (mControllers) {
                        mControllers.put(controller.getSessionToken(), pair);
                    }
                }
            }
        }
        // Now remove old sessions that are not longer active.
        for (Map.Entry<MediaSession.Token, Pair<MediaController, MediaController.Callback>> entry : mControllers.entrySet()) {
            MediaSession.Token token = entry.getKey();
            if (!tokens.contains(token)) {
                Pair<MediaController, MediaController.Callback> pair = entry.getValue();
                pair.first.unregisterCallback(pair.second);
                synchronized (mControllers) {
                    mControllers.remove(token);
                }
            }
        }
    }

    private final MediaController.Callback mCallback =
        new MediaController.Callback() {
            MediaMetadata metadata= null, metadataScrobbled= null;
            int lastHash = 0;
            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
                super.onMetadataChanged(metadata);
                Stuff.log(c, "metadata changed " );
                this.metadata = metadata;
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
                if (state.getState()==PlaybackState.STATE_PAUSED || state.getState()==PlaybackState.STATE_STOPPED) {
//                    cancel scrobbling if within time
                    Stuff.log(c, "PAUSED/Stopped: " + state.getPosition()+ " " + title);

                    handler.remove(lastHash);
                } else if (state.getState()==PlaybackState.STATE_PLAYING){
                    Stuff.log(c, "playing: "+ state.getPosition()+ " " + title);
                    if (pref.getBoolean("scrobble_youtube", true) && !Scrobbler.scrobbledHashes.contains(lastHash))
                       lastHash = handler.scrobble(title, title.hashCode());
                } else
                    Stuff.log(c, "other ("+state.getState()+") : " + title);

            }
        };
}
