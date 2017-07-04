package com.arn.ytscrobble;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.media.session.PlaybackState;
import android.support.annotation.NonNull;
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
    Context c;
    NLService.ScrobbleHandler handler;
    Map<MediaSession.Token, Pair<MediaController, MediaController.Callback>> mControllers = new HashMap<>();

    public SessListener(Context c, NLService.ScrobbleHandler h){
        this.c = c;
        handler = h;
    }

    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        int controllerCount = ((null == controllers) ? 0 : controllers.size());
        Set<MediaSession.Token> tokens = new HashSet<>(controllerCount);
        for (int i = 0; i < controllerCount; i++) {
            MediaController controller = controllers.get(i);
            if (controller.getPackageName().equals( NLService.YOUTUBE_PACKAGE)) {
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
            MediaMetadata metadata= null;
            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
                super.onMetadataChanged(metadata);
                this.metadata = metadata;
                String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                String artist =  metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

                if (!title.equals("")) {
                    handler.scrobble(title, title.hashCode());
                }
            }

            @Override
            public void onPlaybackStateChanged(@NonNull PlaybackState state) {
                super.onPlaybackStateChanged(state);
                if (state.getState()==PlaybackState.STATE_PAUSED || state.getState()==PlaybackState.STATE_STOPPED) {
//                    cancel scrobbling if within time
                    String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);

                    if (!title.equals(""))
                        handler.removeMessages(title.hashCode());
                }
            }
        };
}
