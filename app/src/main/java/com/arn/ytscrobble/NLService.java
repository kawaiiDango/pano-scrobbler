package com.arn.ytscrobble;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Arrays;

import android.os.Handler;

public class NLService extends NotificationListenerService {

    public static final String pNLS = "com.arn.ytscrobble.NLS",
        pNOTIFICATION_EVENT = "com.arn.ytscrobble.NOTIFICATION_EVENT",
        MXM_PACKAGE = "com.musixmatch.android.lyrify",
        YOUTUBE_PACKAGE = "com.google.android.youtube",
            XIAMI_PACKAGE = "fm.xiami.main",
        NOTI_TEXT[] = new String[]{
                "Tap to show lyrics", "Tap to hide lyrics"},
        B_TITLE = "title",
        B_TIME = "time";
    static final long SCROBBLE_DELAY = 30000;
    ArrayList<Integer> activeIDs = new ArrayList<>();
    private SessListener sessListener;
    private MediaSessionManager sessManager;
    private boolean sessListening = true;

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(pNLS);
        registerReceiver(nlservicereciver,filter);


        // Media session manager leaks/holds the context for too long.
        // Don't let it to leak the activity, better lak the whole app.
        Context c = getApplicationContext();
        sessListener = new SessListener(c, handler);

        sessManager = (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);

        try {
            sessManager.addOnActiveSessionsChangedListener(sessListener,  new ComponentName(this, NLService.class));
//            sessListener.setMediaController(this);
            sessListening = true;
        } catch (SecurityException exception) {
            Stuff.log(c, "Failed to start media controller: " + exception.getMessage());
            // Try to unregister it, just it case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener);
            } catch (Exception e) { /* unused */ } finally {
                sessManager = null;
                sessListening = false;
            }
            // Media controller needs notification listener service
            // permissions to be granted.
            return;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String s = "onNotificationPosted  (" + sbn.getId() +  ") :" + sbn.getPackageName() + "\n";
        Notification n = sbn.getNotification();

        for (String key : n.extras.keySet())
            s += "\nBundle: " + key + " = " + n.extras.get(key);

        CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT),
                title = n.extras.getCharSequence(Notification.EXTRA_TITLE);

        Intent i = new  Intent(pNOTIFICATION_EVENT);
        i.putExtra("notification_event", s);
        sendBroadcast(i);
        if (text != null && title != null){
            boolean found = false;
            String songTitle = null;
            if (sbn.getPackageName().equals(MXM_PACKAGE) && Arrays.binarySearch(NOTI_TEXT, text) > -1) {
                songTitle = title.toString();
                found = true;
            }

            if (found){
                handler.scrobble(songTitle, sbn.getId());
            }
        }
    }
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        int idx = activeIDs.indexOf(sbn.getId());
        if(idx != -1)
            activeIDs.remove(idx);

        String s = "onNotificationRemoved (" + sbn.getId() +  ") :" + sbn.getPackageName() + "\n";
        Notification n = sbn.getNotification();
        CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT),
                title = n.extras.getCharSequence(Notification.EXTRA_TITLE);

        Intent i = new  Intent(pNOTIFICATION_EVENT);
        i.putExtra("notification_event", s);
        sendBroadcast(i);
        if (text != null)
            if (sbn.getPackageName().equals(MXM_PACKAGE) && text.equals(NOTI_TEXT)){
                handler.removeMessages(text.toString().hashCode());
            }
    }

    private BroadcastReceiver nlservicereciver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            Stuff.log(getApplicationContext(), "got one");
            if(intent.getStringExtra("command").equals("clearall")){
                NLService.this.cancelAllNotifications();
            }
            else if(intent.getStringExtra("command").equals("list")){

                Intent i1 = new  Intent(pNOTIFICATION_EVENT);
                i1.putExtra("notification_event","=====================");
                sendBroadcast(i1);
                int i=1;
                for (StatusBarNotification sbn : NLService.this.getActiveNotifications()) {
                    Intent i2 = new  Intent(pNOTIFICATION_EVENT);
                    i2.putExtra("notification_event",i +" " + sbn.getPackageName() + "n");
                    sendBroadcast(i2);
                    i++;
                }
                Intent i3 = new  Intent(pNOTIFICATION_EVENT);
                i3.putExtra("notification_event","===== Notification List ====");
                sendBroadcast(i3);
            }
        }
    };

    class ScrobbleHandler extends Handler{
        @Override
        public void handleMessage(Message m) {
            //TODO: handle
            String title = m.getData().getString(B_TITLE);
            new Scrobbler(getApplicationContext()).execute(Stuff.SCROBBLE, title);
        }

        public void scrobble(String songTitle, int id){
            int hash = songTitle.hashCode();
            if (!activeIDs.contains(id))
                activeIDs.add(id);
            else
                removeMessages(hash);
            if (!hasMessages(hash)) {

                new Scrobbler(getApplicationContext()).execute(Stuff.NOW_PLAYING, songTitle);
                Message m = obtainMessage();
                Bundle b = new Bundle();
                b.putString(B_TITLE, songTitle);
                b.putLong(B_TIME, System.currentTimeMillis());
                m.setData(b);
                m.what = hash;
                sendMessageDelayed(m, SCROBBLE_DELAY);
            }
        }


    }
    ScrobbleHandler handler = new ScrobbleHandler();
}