package com.arn.ytscrobble;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;

public class NLService extends NotificationListenerService {

    public static final String pNLS = "com.arn.ytscrobble.NLS",
        pNOTIFICATION_EVENT = "com.arn.ytscrobble.NOTIFICATION_EVENT",
        pCANCEL = "com.arn.ytscrobble.CANCEL",
        pLOVE = "com.arn.ytscrobble.LOVE",
        pUNLOVE = "com.arn.ytscrobble.UNLOVE",
        MXM_PACKAGE = "com.musixmatch.android.lyrify",
        YOUTUBE_PACKAGE = "com.google.android.youtube",
            XIAMI_PACKAGE = "fm.xiami.main",
        NOTI_TEXT[] = new String[]{
                "Tap to show lyrics", "Tap to hide lyrics"},
        B_TITLE = "title",
        B_TIME = "time",
        B_ARTIST = "artist";
    ArrayList<Integer> activeIDs = new ArrayList<>();
    private SessListener sessListener;
    private MediaSessionManager sessManager;
    private boolean sessListening = true;
    private SharedPreferences pref=null;
    NotificationManager nm = null;
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(pNLS);
        filter.addAction(pCANCEL);
        registerReceiver(nlservicereciver,filter);

        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

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
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null)
            return;
        String s = "onNotificationPosted  (" + sbn.getId() +  ") :" + sbn.getPackageName() + "\n";
        Notification n = sbn.getNotification();

        for (String key : n.extras.keySet())
            s += "\nBundle: " + key + " = " + n.extras.get(key);

        CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT),
                title = n.extras.getCharSequence(Notification.EXTRA_TITLE);

        if (text != null && title != null){
            boolean found = false;
            String songTitle = null;
            if (pref.getBoolean("scrobble_mxmFloatingLyrics", false) &&
                    sbn.getPackageName().equals(MXM_PACKAGE) && Arrays.binarySearch(NOTI_TEXT, text) > -1) {
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
        if (sbn == null)
            return;

        String s = "onNotificationRemoved (" + sbn.getId() +  ") :" + sbn.getPackageName() + "\n";
        Notification n = sbn.getNotification();
        CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT),
                title = n.extras.getCharSequence(Notification.EXTRA_TITLE);

        if (text != null)
            if (pref.getBoolean("scrobble_mxmFloatingLyrics", false) &&
                    sbn.getPackageName().equals(MXM_PACKAGE) && Arrays.binarySearch(NOTI_TEXT, text) > -1){
                int idx = activeIDs.indexOf(sbn.getId());
                if(idx != -1)
                    activeIDs.remove(idx);
                handler.removeMessages(title.toString().hashCode());
            }
    }

    private BroadcastReceiver nlservicereciver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(pCANCEL)){
                handler.remove(intent.getIntExtra("id", 0));
            } else if (intent.getAction().equals(pLOVE))
                new Scrobbler(getApplicationContext(), handler).execute(Stuff.LOVE,
                        intent.getStringExtra("artist"), intent.getStringExtra("title"));
            else if (intent.getAction().equals(pUNLOVE))
                new Scrobbler(getApplicationContext(), handler).execute(Stuff.UNLOVE,
                        intent.getStringExtra("artist"), intent.getStringExtra("title"));
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
            String title = m.getData().getString(B_TITLE),
                    artist = m.getData().getString(B_ARTIST);
            int hash = title.hashCode() + artist.hashCode();
            new Scrobbler(getApplicationContext(), handler).execute(Stuff.SCROBBLE, artist, title);
            remove(hash);
            notification(artist, title, hash, Stuff.STATE_SCROBBLED, 0);
        }

        public int scrobble(String songTitle, int id){
            String splits[] = Stuff.sanitizeTitle(songTitle);
            int hash = splits[0].hashCode() + splits[1].hashCode();
            if (!activeIDs.contains(id))
                activeIDs.add(id);
            else
                removeMessages(hash);
            if (!hasMessages(hash)) {

                if (splits.length == 2 && !splits[0].equals("") && !splits[1].equals("")) {
                    new Scrobbler(getApplicationContext(), handler)
                            .execute(Stuff.NOW_PLAYING, splits[0], splits[1]);
                    Message m = obtainMessage();
                    Bundle b = new Bundle();
                    b.putString(B_ARTIST, splits[0]);
                    b.putString(B_TITLE, splits[1]);
                    b.putLong(B_TIME, System.currentTimeMillis());
                    m.setData(b);
                    m.what = hash;
                    int delay = pref.getInt("delay_secs", 30) * 1000;

                    sendMessageDelayed(m, delay);
                    notification(splits[0], splits[1], hash, Stuff.STATE_SCROBBLING, 0);
                } else {
                    notification(songTitle, hash, Stuff.STATE_PARSE_ERR, android.R.drawable.stat_notify_error);
                }
            }
            return hash;
        }

        public void notification(String title1, String title2, int id, String state, int iconId){
            if (!pref.getBoolean("show_notifications", true))
                return;
            if (iconId == 0)
                iconId = R.drawable.ic_app;
            Intent intent = new Intent(pCANCEL)
                    .putExtra("id", id);
            PendingIntent cancelIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

            intent = new Intent(getApplicationContext(), Main.class);
            PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

            intent = new Intent(pLOVE)
                    .putExtra("artist", title1)
                    .putExtra("title", title2);
            PendingIntent loveIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

            String title = title1 + (title2 != null ? " - " + title2 : "");

            Notification.Builder nb = new Notification.Builder(getApplicationContext())
                    .setContentTitle(state)
                    .setContentText(id+" "+title)
                    .setSmallIcon(iconId)
                    .setContentIntent(launchIntent)
                    .setAutoCancel(true);
            if (state.equals(Stuff.STATE_SCROBBLING))
                    nb.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
                    .addAction(R.drawable.ic_heart, "Love", loveIntent);
            if (state.equals(Stuff.STATE_SCROBBLED))
                nb.addAction(R.drawable.ic_heart, "Love", loveIntent);

            Notification n = nb.build();
            nm.notify(id, n);
        }

        public void notification(String title1, int id, String state, int iconId){
            notification(title1, null, id, state, iconId);
        }

        public void remove(int id){
            Stuff.log(getApplicationContext(), id+" ");
            nm.cancel(id);
            removeMessages(id);
        }
    }
    ScrobbleHandler handler = new ScrobbleHandler();
}