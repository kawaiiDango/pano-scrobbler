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

    public static final String
            pNLS = "com.arn.ytscrobble.NLS",
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
    public static final int NOTI_ID = 5,
            NOTI_ERR_ICON = R.drawable.ic_transparent;
    ArrayList<Integer> activeIDs = new ArrayList<>();
    private SharedPreferences pref=null;
    NotificationManager nm = null;
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(pNLS);
        filter.addAction(pCANCEL);
        filter.addAction(pLOVE);
        filter.addAction(pUNLOVE);
        registerReceiver(nlservicereciver,filter);

        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        // Media session manager leaks/holds the context for too long.
        // Don't let it to leak the activity, better lak the whole app.
        Context c = getApplicationContext();
        SessListener sessListener = new SessListener(c, handler);

        MediaSessionManager sessManager = (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);

        try {
            sessManager.addOnActiveSessionsChangedListener(sessListener,  new ComponentName(this, NLService.class));
        } catch (SecurityException exception) {
            Stuff.log(c, "Failed to start media controller: " + exception.getMessage());
            // Try to unregister it, just it case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener);
            } catch (Exception e) { /* unused */ } finally {
                sessManager = null;
            }
            // Media controller needs notification listener service
            // permissions to be granted.
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
                handler.scrobble(songTitle);
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
            Stuff.log(context, "int "+ intent.getAction());
            if(intent.getAction().equals(pCANCEL)){

                handler.remove(intent.getIntExtra("id", 0));
            } else if (intent.getAction().equals(pLOVE)) {
                Stuff.log(context, "lolo");
                new Scrobbler(getApplicationContext(), handler).execute(Stuff.LOVE,
                        intent.getStringExtra("artist"), intent.getStringExtra("title"));
                handler.notification(intent.getStringExtra("artist"), intent.getStringExtra("title"), Stuff.STATE_SCROBBLED, 0, false);
            }else if (intent.getAction().equals(pUNLOVE)) {
                new Scrobbler(getApplicationContext(), handler).execute(Stuff.UNLOVE,
                        intent.getStringExtra("artist"), intent.getStringExtra("title"));
                handler.notification(intent.getStringExtra("artist"), intent.getStringExtra("title"), Stuff.STATE_SCROBBLED, 0);
            }else if(intent.getStringExtra("command").equals("list")){

                Stuff.log(getApplicationContext(), "notifications list");
                int i=1;
                for (StatusBarNotification sbn : NLService.this.getActiveNotifications()) {
                    Stuff.log(getApplicationContext(), sbn.getPackageName());
                    i++;
                }
            }
        }
    };

    class ScrobbleHandler extends Handler{
        int lastNotiIcon = 0;
        @Override
        public void handleMessage(Message m) {
            //TODO: handle
            String title = m.getData().getString(B_TITLE),
                    artist = m.getData().getString(B_ARTIST);
//            int hash = title.hashCode() + artist.hashCode();
            new Scrobbler(getApplicationContext(), handler).execute(Stuff.SCROBBLE, artist, title);
            notification(artist, title, Stuff.STATE_SCROBBLED, 0);
        }

        public int scrobble(String songTitle){
            String splits[] = Stuff.sanitizeTitle(songTitle);
            int hash = splits[0].hashCode() + splits[1].hashCode();
            if (!activeIDs.contains(hash))
                activeIDs.add(hash);
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
                    notification(splits[0], splits[1], Stuff.STATE_SCROBBLING, 0);
                } else {
                    notification(splits[0], splits[1], Stuff.STATE_PARSE_ERR, NOTI_ERR_ICON);
                }
            }
            return hash;
        }

        void notification(String title1, String title2, String state, int iconId, boolean love){
            if (!pref.getBoolean("show_notifications", true))
                return;
            if (iconId == 0)
                iconId = R.drawable.ic_noti;
            lastNotiIcon = iconId;

            String title = title1;
            int hash = title1.hashCode();
            if (title2 != null) {
                hash += title2.hashCode();
                title += " - " + title2;
            } else {
                title2 = "";
            }

            String loveText = love ? "❤ Love it" : "\uD83D\uDC94 Unlove it";
            String loveAction = love ? pLOVE : pUNLOVE;

            Intent intent = new Intent(getApplicationContext(), Main.class);
            PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

            intent = new Intent(pCANCEL)
                    .putExtra("id", hash);
            PendingIntent cancelIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            intent = new Intent(loveAction)
                    .putExtra("artist", title1)
                    .putExtra("title", title2);
            PendingIntent loveIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);



            Notification.Builder nb = new Notification.Builder(getApplicationContext())
                    .setContentTitle(state)
                    .setContentText(title)
                    .setSmallIcon(iconId)
                    .setContentIntent(launchIntent)
                    .setAutoCancel(true)
                    .setPriority(iconId == NOTI_ERR_ICON ? Notification.PRIORITY_MIN : Notification.PRIORITY_LOW);

            if (state.equals(Stuff.STATE_SCROBBLING))
                    nb.addAction(R.drawable.ic_transparent, loveText, loveIntent)
                            .addAction(R.drawable.ic_transparent, "❌ Cancel", cancelIntent);

            if (state.equals(Stuff.STATE_SCROBBLED))
                nb.addAction(R.drawable.ic_transparent, loveText, loveIntent);
            Notification n = nb.build();
            nm.notify(NOTI_ID, n);
        }

        void notification(String title1, String state, int iconId){
            notification(title1, null, state, iconId, true);
        }

        void notification(String title1, String title2, String state, int iconId){
            notification(title1, title2, state, iconId, true);
        }

        public void remove(int hash){
            Stuff.log(getApplicationContext(), hash+" canceled");
            removeMessages(hash);
            if (lastNotiIcon != NOTI_ERR_ICON)
                nm.cancel(NOTI_ID);

        }
    }
    ScrobbleHandler handler = new ScrobbleHandler();
}