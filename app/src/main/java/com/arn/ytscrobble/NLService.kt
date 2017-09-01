package com.arn.ytscrobble

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.preference.PreferenceManager

class NLService : NotificationListenerService() {
    private var activeIDs = mutableListOf<Int>()
    lateinit private var pref: SharedPreferences
    lateinit private var nm: NotificationManager

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter()
        filter.addAction(pNLS)
        filter.addAction(pCANCEL)
        filter.addAction(pLOVE)
        filter.addAction(pUNLOVE)
        registerReceiver(nlservicereciver, filter)

        pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        handler = ScrobbleHandler()

        // Media session manager leaks/holds the context for too long.
        // Don't let it to leak the activity, better lak the whole app.
        val c = applicationContext
        val sessListener = SessListener(c, handler)

        val sessManager = c.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        try {
            sessManager.addOnActiveSessionsChangedListener(sessListener, ComponentName(this, NLService::class.java))
        } catch (exception: SecurityException) {
            Stuff.log(c, "Failed to start media controller: " + exception.message)
            // Try to unregister it, just it case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener)
            } catch (e: Exception) { /* unused */ }
            // Media controller needs notification listener service
            // permissions to be granted.
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null)
            return
        var s = "onNotificationPosted  (" + sbn.id + ") :" + sbn.packageName + "\n"
        val n = sbn.notification

        for (key in n.extras.keySet())
            s += "\nBundle: " + key + " = " + n.extras.get(key)

        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)

        if (text != null && title != null) {
            var found = false
            val songTitle: String
            if (pref.getBoolean("scrobble_mxmFloatingLyrics", false) &&
                    sbn.packageName == MXM_PACKAGE && NOTI_TEXT.contains(text)) {
                songTitle = title.toString()
                found = true
                handler.scrobble(songTitle)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null)
            return

        val s = "onNotificationRemoved (" + sbn.id + ") :" + sbn.packageName + "\n"
        val n = sbn.notification
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)

        if (text != null)
            if (pref.getBoolean("scrobble_mxmFloatingLyrics", false) &&
                    sbn.packageName == MXM_PACKAGE && NOTI_TEXT.contains(text)) {
                val idx = activeIDs.indexOf(sbn.id)
                if (idx != -1)
                    activeIDs.removeAt(idx)
                handler.removeMessages(title!!.toString().hashCode())
            }
    }

    private val nlservicereciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Stuff.log(context, "int " + intent.action!!)
            if (intent.action == pCANCEL) {

                handler.remove(intent.getIntExtra("id", 0))
            } else if (intent.action == pLOVE) {
                Stuff.log(context, "lolo")
                Scrobbler(applicationContext, handler).execute(Stuff.LOVE,
                        intent.getStringExtra("artist"), intent.getStringExtra("title"))
                handler.notification(intent.getStringExtra("artist"), intent.getStringExtra("title"), getString(R.string.state_scrobbled), 0, false)
            } else if (intent.action == pUNLOVE) {
                Scrobbler(applicationContext, handler).execute(Stuff.UNLOVE,
                        intent.getStringExtra("artist"), intent.getStringExtra("title"))
                handler.notification(intent.getStringExtra("artist"), intent.getStringExtra("title"), getString(R.string.state_scrobbled), 0)
            } else if (intent.getStringExtra("command") == "list") {

                Stuff.log(applicationContext, "notifications list")
                var i = 1
                for (sbn in this@NLService.activeNotifications) {
                    Stuff.log(applicationContext, sbn.packageName)
                    i++
                }
            }
        }
    }

    inner class ScrobbleHandler : Handler() {
        private var lastNotiIcon = 0
        override fun handleMessage(m: Message) {
            //TODO: handle
            val title = m.data.getString(B_TITLE)
            val artist = m.data.getString(B_ARTIST)
            //            int hash = title.hashCode() + artist.hashCode();
            Scrobbler(applicationContext, handler).execute(Stuff.SCROBBLE, artist, title)
            notification(artist, title, getString(R.string.state_scrobbled), 0)
        }

        fun scrobble(songTitle: String): Int {
            val splits = Stuff.sanitizeTitle(songTitle)
            val hash = splits[0].hashCode() + splits[1].hashCode()
            if (!activeIDs.contains(hash))
                activeIDs.add(hash)
            else
                removeMessages(hash)
            if (!hasMessages(hash)) {

                if (splits.size == 2 && splits[0] != "" && splits[1] != "") {
                    Scrobbler(applicationContext, handler)
                            .execute(Stuff.NOW_PLAYING, splits[0], splits[1])
                    val m = obtainMessage()
                    val b = Bundle()
                    b.putString(B_ARTIST, splits[0])
                    b.putString(B_TITLE, splits[1])
                    b.putLong(B_TIME, System.currentTimeMillis())
                    m.data = b
                    m.what = hash
                    val delay = pref.getInt("delay_secs", 30) * 1000

                    sendMessageDelayed(m, delay.toLong())
                    notification(splits[0], splits[1], getString(R.string.state_scrobbling), 0)
                } else {
                    notification(getString(R.string.parse_error), splits[0] + " " + splits[1], getString(R.string.not_scrobling), NOTI_ERR_ICON)
                }
            }
            return hash
        }

        @JvmOverloads
        fun notification(title1: String, title2: String?, state: String, iconId: Int, love: Boolean = true) {
            var title2 = title2
            var iconId = iconId
            if (!pref.getBoolean("show_notifications", true))
                return
            if (iconId == 0)
                iconId = R.drawable.ic_noti
            lastNotiIcon = iconId

            var title = title1
            var hash = title1.hashCode()
            if (title2 != null) {
                hash += title2.hashCode()
                title += " - " + title2
            } else {
                title2 = ""
            }

            val loveText = if (love) "❤ Love it" else "\uD83D\uDC94 Unlove it"
            val loveAction = if (love) pLOVE else pUNLOVE

            var intent = Intent(applicationContext, Main::class.java)
            val launchIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

            intent = Intent(pCANCEL)
                    .putExtra("id", hash)
            val cancelIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            intent = Intent(loveAction)
                    .putExtra("artist", title1)
                    .putExtra("title", title2)
            val loveIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)


            val nb = Notification.Builder(applicationContext)
                    .setContentTitle(state)
                    .setContentText(title)
                    .setSmallIcon(iconId)
                    .setContentIntent(launchIntent)
                    .setAutoCancel(true)
                    .setPriority(if (iconId == NOTI_ERR_ICON) Notification.PRIORITY_MIN else Notification.PRIORITY_LOW)

            if (state == getString(R.string.state_scrobbling))
                nb.addAction(R.drawable.ic_transparent, loveText, loveIntent)
                        .addAction(R.drawable.ic_transparent, "❌ Unscrobble", cancelIntent)

            if (state == getString(R.string.state_scrobbled))
                nb.addAction(R.drawable.ic_transparent, loveText, loveIntent)
            val n = nb.build()
            nm.notify(NOTI_ID, n)
        }

        fun notification(title1: String, state: String, iconId: Int) {
            notification(title1, null, state, iconId, true)
        }

        fun remove(hash: Int) {
            Stuff.log(applicationContext, hash.toString() + " canceled")
            removeMessages(hash)
            if (lastNotiIcon != NOTI_ERR_ICON)
                nm.cancel(NOTI_ID)

        }
    }



    companion object {

        lateinit private var handler : NLService.ScrobbleHandler

        val pNLS = "com.arn.ytscrobble.NLS"
        val pCANCEL = "com.arn.ytscrobble.CANCEL"
        val pLOVE = "com.arn.ytscrobble.LOVE"
        val pUNLOVE = "com.arn.ytscrobble.UNLOVE"
        val MXM_PACKAGE = "com.musixmatch.android.lyrify"
        val YOUTUBE_PACKAGE = "com.google.android.youtube"
        val XIAMI_PACKAGE = "fm.xiami.main"
        val NOTI_TEXT = arrayOf("Tap to show lyrics", "Tap to hide lyrics")
        val B_TITLE = "title"
        val B_TIME = "time"
        val B_ARTIST = "artist"
        val NOTI_ID = 5
        val NOTI_ERR_ICON = R.drawable.ic_transparent
    }
}