@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.app.Notification
import android.app.Notification.VISIBILITY_SECRET
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import com.arn.scrobble.receivers.TrackMetaListener
import android.app.NotificationChannel
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.widget.Toast


class NLService : NotificationListenerService() {
    lateinit private var pref: SharedPreferences
    lateinit private var nm: NotificationManager
    private var sessListener: SessListener? = null
    private var tReceiver: TrackMetaListener? = null


    override fun onCreate() {
        super.onCreate()
        Stuff.log("onCreate")

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        // lollipop and mm bug
        if (Looper.myLooper() == null){
            Handler(mainLooper).post{ init() }
        } else
            init()
    }

    private fun init(){
        val filter = IntentFilter()
        filter.addAction(pNLS)
        filter.addAction(pCANCEL)
        filter.addAction(pCANCEL_TOAST)
        filter.addAction(pLOVE)
        filter.addAction(pUNLOVE)
        filter.addAction(pWHITELIST)
        filter.addAction(pBLACKLIST)
        registerReceiver(nlservicereciver, filter)

        pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Media session manager leaks/holds the context for too long.
        // Don't let it to leak the activity, better lak the whole app.
        val c = applicationContext
        handler = ScrobbleHandler()
        val sessManager = c.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        sessListener = SessListener(applicationContext, handler)
        try {
            sessManager.addOnActiveSessionsChangedListener(sessListener, ComponentName(this, this::class.java))
            Stuff.log("onListenerConnected")
        } catch (exception: SecurityException) {
            Stuff.log("Failed to start media controller: " + exception.message)
            // Try to unregister it, just it case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Media controller needs notification listener service
            // permissions to be granted.
        }
//        tReceiver = CommonPlayers.regIntents(applicationContext)
        initChannels(applicationContext)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Stuff.log("onListenerDisconnected")
        unregisterReceiver(nlservicereciver)
        if (sessListener != null)
            (applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                    .removeOnActiveSessionsChangedListener(sessListener)
        if (tReceiver != null)
            unregisterReceiver(tReceiver)
    }
    override fun onDestroy() {
        Stuff.log("onDestroy")
        super.onDestroy()
    }

/*
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
*/
    private val nlservicereciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Stuff.log("nlservicereciver intent " + intent.action!!)
            when (intent.action){
                 pCANCEL -> handler.remove(intent.getIntExtra("id", 0))
                 pCANCEL_TOAST -> {
                     handler.remove(intent.getIntExtra("id", 0))
                     Stuff.toast(applicationContext, "Un-scrobbled", Toast.LENGTH_LONG)
                 }
                pLOVE -> {
                    LFMRequester(applicationContext, handler).execute(Stuff.LOVE,
                            intent.getStringExtra("artist"), intent.getStringExtra("title"))
                    handler.notification(intent.getStringExtra("artist"),
                            intent.getStringExtra("title"), getString(R.string.state_scrobbled), 0, false)
                }
                pUNLOVE -> {
                    LFMRequester(applicationContext, handler).execute(Stuff.UNLOVE,
                            intent.getStringExtra("artist"), intent.getStringExtra("title"))
                    handler.notification(intent.getStringExtra("artist"),
                            intent.getStringExtra("title"), getString(R.string.state_scrobbled), 0)
                }
                pWHITELIST, pBLACKLIST -> {
                    val wSet = pref.getStringSet(Stuff.APP_WHITELIST, mutableSetOf())
                    val bSet = pref.getStringSet(Stuff.APP_BLACKLIST, mutableSetOf())

                    if (intent.action == pWHITELIST)
                        wSet.add(intent.getStringExtra("packageName"))
                    else
                        bSet.add(intent.getStringExtra("packageName"))
                    bSet.removeAll(wSet) //whitelist takes over blacklist for conflicts
                    pref.edit()
                            .putStringSet(Stuff.APP_WHITELIST, wSet)
                            .putStringSet(Stuff.APP_BLACKLIST,  bSet)
                            .apply()
                    nm.cancel(NOTI_ID_APP, 0)
                }
            }
        }
    }

    inner class ScrobbleHandler : Handler {
        constructor() : super()
        constructor(looper: Looper) : super(looper)

        private var lastNotiIcon = 0
        override fun handleMessage(m: Message) {
            //TODO: corrected artist/title
            val title = m.data.getString(B_TITLE)
            val artist = m.data.getString(B_ARTIST)
            val time = m.data.getLong(B_TIME)
            val duration = m.data.getLong(B_DURATION)
            //            int hash = title.hashCode() + artist.hashCode();
            LFMRequester(applicationContext, this).execute(Stuff.SCROBBLE, artist, title, time.toString(), duration.toString())
            notification(artist, title, getString(R.string.state_scrobbled), 0)
        }

        fun scrobble(artist:String, title: String, duration:Long, packageName: String? = null): Int {
            val hash = artist.hashCode() + title.hashCode()

//            if (!activeIDs.contains(hash))
//                activeIDs.add(hash)
//            else
//                removeMessages(hash)
            removeCallbacksAndMessages(null)
            if (!hasMessages(hash)) {

                if (artist != "" && title != "") {
                    val now = System.currentTimeMillis()
                    LFMRequester(applicationContext, this)
                            .execute(Stuff.NOW_PLAYING, artist, title, now.toString(), duration.toString())
                    val m = obtainMessage()
                    val b = Bundle()
                    b.putString(B_ARTIST, artist)
                    b.putString(B_TITLE, title)
                    b.putLong(B_TIME, now)
                    b.putLong(B_DURATION, duration)
                    m.data = b
                    m.what = hash
                    val delaySecs = pref.getInt("delay_secs", 50).toLong() * 1000
                    val delayPer = pref.getInt("delay_per", 30).toLong()
                    var delay = delaySecs
                    if (duration > 10000 && duration*delayPer/100 < delay){ //dont scrobble <10 sec songs?
                        delay = duration*delayPer/100
                    }

                    sendMessageDelayed(m, delay)
                    notification(artist, title, getString(R.string.state_scrobbling), 0)
                    //display ignore thing only on successful parse
                    if (packageName != null) {
                        val n = buildAppNotification(packageName)
                        if (n != null)
                            nm.notify(NOTI_ID_APP, 0, n)
                    }
                } else {
                    notification(getString(R.string.parse_error), artist + " " + title, getString(R.string.not_scrobling), NOTI_ERR_ICON)
                }
            }
            return hash
        }
        fun scrobble(songTitle: String, duration:Long, packageName:String? = null): Int {
            val splits = Stuff.sanitizeTitle(songTitle)
            return scrobble(splits[0], splits[1], duration, packageName)
        }
        private fun buildAppNotification(packageName:String): Notification?{
            val appName: String
            try {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                appName = packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: Exception) {
                //eat up all NPEs and stuff
                return null
            }

            var intent = Intent(pBLACKLIST)
                    .putExtra("packageName", packageName)
            val ignoreIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            intent = Intent(pWHITELIST)
                    .putExtra("packageName", packageName)
            val okayIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            intent = Intent(applicationContext, Main::class.java)
                    .putExtra(Stuff.DEEP_LINK_KEY, Stuff.DL_APP_LIST)
            val launchIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

            val nb = NotificationCompat.Builder(applicationContext, NOTI_ID_SCR)
                    .setContentTitle(getString(R.string.new_player)+ appName)
                    .setContentText(getString(R.string.new_player_prompt))
                    .setSmallIcon(R.drawable.ic_noti)
                    .setColor(resources.getColor(R.color.colorAccent))
                    .setContentIntent(launchIntent)
                    .addAction(getAction(R.drawable.vd_ban, "\uD83D\uDEAB", getString(R.string.ignore_app), ignoreIntent))
                    .addAction(getAction(R.drawable.vd_check, "✔", getString(R.string.ok_cool), okayIntent))
                    .setAutoCancel(true)
                    .setStyle(MediaStyle().setShowActionsInCompactView(0,1))
                    .setCustomBigContentView(null)

            return nb.build()
        }
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

            val loveAction: NotificationCompat.Action

            if (love){
                val i = Intent(pLOVE)
                        .putExtra("artist", title1)
                        .putExtra("title", title2)
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 0, i,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                loveAction = getAction(R.drawable.vd_heart, "❤", getString(R.string.love), loveIntent)
            } else {
                val i = Intent(pUNLOVE)
                        .putExtra("artist", title1)
                        .putExtra("title", title2)
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 0, i,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                loveAction = getAction(R.drawable.vd_heart_break, "\uD83D\uDC94", getString(R.string.unlove), loveIntent)
            }

            var intent = Intent(applicationContext, Main::class.java)
            val launchIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

            intent = Intent(pCANCEL_TOAST)
                    .putExtra("id", hash)
            val cancelToastIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val nb = NotificationCompat.Builder(applicationContext,
                    if (iconId == NOTI_ERR_ICON) NOTI_ID_ERR else NOTI_ID_SCR)
                    .setSmallIcon(iconId)
                    .setColor(resources.getColor(R.color.colorPrimary))
                    .setContentIntent(launchIntent)
                    .setVisibility(VISIBILITY_SECRET)
                    .setAutoCancel(true)
                    .setShowWhen(false)
                    .setPriority(if (iconId == NOTI_ERR_ICON) Notification.PRIORITY_MIN else Notification.PRIORITY_LOW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                nb.setContentTitle(title2)
                        .setContentText(title1)
                        .setSubText(state)
            } else {
                nb.setContentTitle(state)
                        .setContentText(title2)
                        .setSubText(state)
                if (state == getString(R.string.state_scrobbling))
                    nb.setUsesChronometer(true)
            }

            if (state == getString(R.string.state_scrobbling) || state == getString(R.string.state_scrobbled)) {
                val style = MediaStyle()
                nb.addAction(loveAction)
                        .setCustomBigContentView(null)
                if (state == getString(R.string.state_scrobbling)) {
                    nb.addAction(getAction(R.drawable.vd_cancel, "❌", getString(R.string.unscrobble), cancelToastIntent))
//                    nb.setDeleteIntent(cancelToastIntent)
                    style.setShowActionsInCompactView(0, 1)
                } else
                    style.setShowActionsInCompactView(0)
                nb.setStyle(style)
            }
            val n = nb.build()

            nm.notify(NOTI_ID_SCR, 0, n)
        }

        fun notification(title1: String, state: String, iconId: Int) {
            notification(title1, null, state, iconId, true)
        }

        private fun getAction(icon:Int, emoji:String, text:String, pIntent:PendingIntent): NotificationCompat.Action {
//            return if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.M)
                return NotificationCompat.Action(icon, text, pIntent)
//            else
//                NotificationCompat.Action(R.drawable.ic_transparent, emoji + " "+ text, pIntent)
        }

        fun remove(hash: Int) {
            Stuff.log(hash.toString() + " canceled")
            removeMessages(hash)
            if (lastNotiIcon != NOTI_ERR_ICON)
                nm.cancel(NOTI_ID_SCR, 0)
        }
    }

    companion object {
        fun initChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(NotificationChannel(NOTI_ID_SCR,
                    context.getString(R.string.channel_scrobbling), NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(NOTI_ID_ERR,
                    context.getString(R.string.channel_err), NotificationManager.IMPORTANCE_MIN))
            nm.createNotificationChannel(NotificationChannel(NOTI_ID_APP,
                    context.getString(R.string.channel_new_app), NotificationManager.IMPORTANCE_LOW))
        }
        lateinit var handler: ScrobbleHandler
        val pNLS = "com.arn.scrobble.NLS"
        val pCANCEL = "com.arn.scrobble.CANCEL"
        val pCANCEL_TOAST = "com.arn.scrobble.CANCEL_TOAST"
        val pLOVE = "com.arn.scrobble.LOVE"
        val pUNLOVE = "com.arn.scrobble.UNLOVE"
        val pBLACKLIST = "com.arn.scrobble.BLACKLIST"
        val pWHITELIST = "com.arn.scrobble.WHITELIST"
        val B_TITLE = "title"
        val B_TIME = "time"
        val B_ARTIST = "artist"
        val B_DURATION = "duration"
        val NOTI_ID_SCR = "scrobble_success"
        val NOTI_ID_ERR = "err"
        val NOTI_ID_APP = "new_app"
        val NOTI_ERR_ICON = R.drawable.ic_transparent
    }
}