@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.app.*
import android.content.*
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import com.arn.scrobble.receivers.LegacyMetaReceiver
import android.graphics.PorterDuff
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.app.MediaStyleMod
import android.widget.Toast
import android.content.Intent
import android.os.IBinder
import android.content.pm.PackageManager
import android.content.ComponentName
import android.os.Process
import android.app.ActivityManager
import com.squareup.leakcanary.LeakCanary


class NLService : NotificationListenerService() {
    lateinit private var pref: SharedPreferences
    lateinit private var nm: NotificationManager
    private var sessListener: SessListener? = null
    private var bReceiver: LegacyMetaReceiver? = null

    override fun onCreate() {
        if (!LeakCanary.isInAnalyzerProcess(this))
            LeakCanary.install(application)
        super.onCreate()
        Stuff.log("onCreate")
        // lollipop and mm bug
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
            init()
        ensureServiceRunning(applicationContext)
    }

    //from https://gist.github.com/xinghui/b2ddd8cffe55c4b62f5d8846d5545bf9
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }


// this prevents the service from starting on N+
//    override fun onBind(intent: Intent): IBinder? {
//        return null
//    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Looper.myLooper() == null) {
                Handler(mainLooper).post { init() }
            } else
                init()
        }
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
        filter.addAction(iPREFS_CHANGED)
        registerReceiver(nlservicereciver, filter)

        pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Media session manager leaks/holds the context for too long.
        // Don't let it to leak the activity, better lak the whole app.
        val c = applicationContext
        handler = ScrobbleHandler()
        val sessManager = c.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        sessListener = SessListener(PreferenceManager.getDefaultSharedPreferences(c), handler)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            bReceiver = LegacyMetaReceiver.regIntents(applicationContext)
        initChannels(applicationContext)
    }

    private fun destroy() {
        Stuff.log("onListenerDisconnected")
        try {
            unregisterReceiver(nlservicereciver)
        } catch(e:IllegalArgumentException) {
            Stuff.log("nlservicereciver wasn't registered")
        }
        if (sessListener != null) {
            (applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                    .removeOnActiveSessionsChangedListener(sessListener)
            sessListener = null
        }
        if (bReceiver != null) {
            unregisterReceiver(bReceiver)
            bReceiver = null
        }
    }

    override fun onListenerDisconnected() { //api 24+ only
        destroy()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        if (sessListener != null)
            destroy()
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
                    else {
                        bSet.add(intent.getStringExtra("packageName"))
                        sessListener?.removeSessions(packageName= intent.getStringExtra("packageName"))
                    }
                    bSet.removeAll(wSet) //whitelist takes over blacklist for conflicts
                    pref.edit()
                            .putStringSet(Stuff.APP_WHITELIST, wSet)
                            .putStringSet(Stuff.APP_BLACKLIST,  bSet)
                            .apply()
                    nm.cancel(NOTI_ID_APP, 0)
                }
                iPREFS_CHANGED -> {
                    val key = intent.getStringExtra("key")
                    val value = intent.extras["value"]
                    val editor = pref.edit()
                    when (value) {
                        is Int -> editor.putInt(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Long -> editor.putLong(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is String -> editor.putString(key, value)
                        is Array<*> -> editor.putStringSet(key, (value as Array<String>).toSet())
                        else -> {
                            Stuff.log("unknown prefs type")
                            return
                        }
                    }
                    editor.apply()
                }
            }
        }
    }

    inner class ScrobbleHandler : Handler {
        constructor() : super()
        constructor(looper: Looper) : super(looper)

        private var lastNotiIcon = 0
        override fun handleMessage(m: Message) {

            val title = m.data.getString(B_TITLE)
            val artist = m.data.getString(B_ARTIST)
            val album = m.data.getString(B_ALBUM)
            val time = m.data.getLong(B_TIME)
            val duration = m.data.getLong(B_DURATION)
            //            int hash = title.hashCode() + artist.hashCode();
            LFMRequester(applicationContext, this).execute(Stuff.SCROBBLE, artist, album, title, time.toString(), duration.toString())
            notification(artist, title, getString(R.string.state_scrobbled), 0)
        }

        fun scrobble(artist:String, album:String, title: String, duration:Long, packageName: String? = null): Int {
            if (!pref.getBoolean("master", true) ||
                    (!pref.getBoolean(Stuff.OFFLINE_SCROBBLE_PREF, true) &&
                            !Stuff.isNetworkAvailable(applicationContext))||
                    !FirstThingsFragment.checkAuthTokenExists(applicationContext))
                return 0
            val hash = artist.hashCode() + title.hashCode()

//            if (!activeIDs.contains(hash))
//                activeIDs.add(hash)
//            else
//                removeMessages(hash)
            removeCallbacksAndMessages(null)
            if (!hasMessages(hash)) {

                if (artist != "" && title != "") {
                    val album = Stuff.sanitizeAlbum(album)
                    val now = System.currentTimeMillis()
                    LFMRequester(applicationContext, this)
                            .execute(Stuff.NOW_PLAYING, artist, album, title, now.toString(), duration.toString())

                    val m = obtainMessage()
                    val b = Bundle()
                    b.putString(B_ARTIST, artist)
                    b.putString(B_ALBUM, album)
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
            return scrobble(splits[0], "", splits[1], duration, packageName)
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
            val ignoreIntent = PendingIntent.getBroadcast(applicationContext, 1, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            intent = Intent(pWHITELIST)
                    .putExtra("packageName", packageName)
            val okayIntent = PendingIntent.getBroadcast(applicationContext, 2, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            intent = Intent(applicationContext, Main::class.java)
                    .putExtra(Stuff.DEEP_LINK_KEY, Stuff.DL_APP_LIST)
            val launchIntent = PendingIntent.getActivity(applicationContext, 9, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val nb = NotificationCompat.Builder(applicationContext, NOTI_ID_SCR)
                    .setContentTitle(getString(R.string.new_player)+ appName)
                    .setContentText(getString(R.string.new_player_prompt))
                    .setSmallIcon(R.drawable.vd_appquestion_noti)
                    .setColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
                    .setContentIntent(launchIntent)
                    .addAction(getAction(R.drawable.vd_ban, "\uD83D\uDEAB", getString(R.string.ignore_app), ignoreIntent))
                    .addAction(getAction(R.drawable.vd_check, "✔", getString(R.string.ok_cool), okayIntent))
                    .setAutoCancel(true)
                    .setCustomBigContentView(null)
                    .setStyle(MediaStyleMod().setShowActionsInCompactView(0,1))
            return buildMediaStyleMod(nb)
        }
        fun notification(title1: String, title2: String?, state: String, iconId: Int, love: Boolean = true) {
            var title2 = title2
            var iconId = iconId
            if (!pref.getBoolean("show_notifications", true))
                return
            if (iconId == 0)
                iconId = R.drawable.vd_noti
            lastNotiIcon = iconId

            var title = title1
            var hash = title1.hashCode()
            if (title2 != null) {
                hash += title2.hashCode()
                title += " - " + title2
            } else {
                title2 = ""
            }

            val loveAction = if (love){
                val i = Intent(pLOVE)
                        .putExtra("artist", title1)
                        .putExtra("title", title2)
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 3, i,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                getAction(R.drawable.vd_heart, "❤", getString(R.string.love), loveIntent)
            } else {
                val i = Intent(pUNLOVE)
                        .putExtra("artist", title1)
                        .putExtra("title", title2)
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 4, i,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                getAction(R.drawable.vd_heart_break, "\uD83D\uDC94", getString(R.string.unlove), loveIntent)
            }

            var intent = Intent(applicationContext, Main::class.java)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT)

            intent = Intent(pCANCEL_TOAST)
                    .putExtra("id", hash)
            val cancelToastIntent = PendingIntent.getBroadcast(applicationContext, 5, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val nb = NotificationCompat.Builder(applicationContext,
                    if (iconId == NOTI_ERR_ICON) NOTI_ID_ERR else NOTI_ID_SCR)
                    .setSmallIcon(iconId)
                    .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                    .setContentIntent(launchIntent)
                    .setVisibility(Notification.VISIBILITY_SECRET)
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
                        .setSubText(title1)
                if (state == getString(R.string.state_scrobbling))
                    nb.setUsesChronometer(true)
            }

            if (state == getString(R.string.state_scrobbling) || state == getString(R.string.state_scrobbled)) {
                val style = MediaStyleMod()
                nb.addAction(loveAction)
                if (state == getString(R.string.state_scrobbling)) {
                    nb.addAction(getAction(R.drawable.vd_cancel, "❌", getString(R.string.unscrobble), cancelToastIntent))
//                    nb.setDeleteIntent(cancelToastIntent)
                    style.setShowActionsInCompactView(0, 1)
                } else
                    style.setShowActionsInCompactView(0)
                nb.setStyle(style)
                        .setCustomBigContentView(null)
            }
            val n = buildMediaStyleMod(nb)

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

        private fun buildMediaStyleMod(nb:NotificationCompat.Builder): Notification {
            val modNeeded = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && nb.mActions != null && nb.mActions.isNotEmpty()
            if (modNeeded) {
                val icon = getDrawable(R.drawable.vd_noti)
                icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
                nb.setLargeIcon(Stuff.drawableToBitmap(icon,true))
            }
            val n = nb.build()
            if (modNeeded)
                n.bigContentView = null

                /*
                val res = Resources.getSystem()
                val attrs = arrayOf(android.R.attr.textColor).toIntArray()

                var sysStyle = res.getIdentifier("TextAppearance.Material.Notification.Title", "style", "android")
                val titleColorHack = obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.BLACK)

                sysStyle = res.getIdentifier("TextAppearance.Material.Notification", "style", "android")
                val descColorHack = obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.BLACK)


                val rv = n.contentView
                var resId = res.getIdentifier("title", "id", "android")
                rv.setTextColor(resId, titleColorHack)
                resId = res.getIdentifier("text", "id", "android")
                rv.setTextColor(resId, descColorHack)
                resId = res.getIdentifier("text2", "id", "android")
                rv.setTextColor(resId, descColorHack)

                resId = res.getIdentifier("action0", "id", "android")
                val context = Class.forName("android.widget.RemoteViews")
                val m = context.getMethod("setDrawableParameters", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, PorterDuff.Mode::class.java, Int::class.javaPrimitiveType)
                m.invoke(rv, resId, false, -1, ContextCompat.getColor(applicationContext, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_ATOP, -1)
                */

            return n
        }

        fun remove(hash: Int) {
            Stuff.log(hash.toString() + " canceled")
            if (hash != 0)
                removeMessages(hash)
//            if (lastNotiIcon != NOTI_ERR_ICON)
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

        fun ensureServiceRunning(context:Context) {
            val serviceComponent = ComponentName(context, NLService::class.java)
            Stuff.log("ensureServiceRunning serviceComponent: " + serviceComponent)
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var serviceRunning = false
            val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
            if (runningServices == null) {
                Stuff.log("ensureServiceRunning() runningServices is NULL")
                return
            }
            for (service in runningServices) {
                if (service.service == serviceComponent) {
                    Stuff.log("ensureServiceRunning service - pid: " + service.pid + ", currentPID: " +
                            Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " +
                            service.clientCount + ", clientLabel: " +
                            if (service.clientLabel == 0) "0" else "(" + context.resources.getString(service.clientLabel) + ")")
                    if (service.pid == Process.myPid() /*&& service.clientCount > 0 && !TextUtils.isEmpty(service.clientPackage)*/) {
                        serviceRunning = true
                    }
                }
            }
            if (serviceRunning) {
                Stuff.log("ensureServiceRunning: service is running")
                return
            }
            Stuff.log("ensureServiceRunning: service not running, reviving...")
            toggleNotificationListenerService(context)
        }

        private fun toggleNotificationListenerService(context:Context) {
            Stuff.log("toggleNotificationListenerService() called")
            val thisComponent = ComponentName(context, NLService::class.java)
            val pm = context.packageManager
            pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }

        lateinit var handler: ScrobbleHandler
        val pNLS = "com.arn.scrobble.NLS"
        val pCANCEL = "com.arn.scrobble.CANCEL"
        val pCANCEL_TOAST = "com.arn.scrobble.CANCEL_TOAST"
        val pLOVE = "com.arn.scrobble.LOVE"
        val pUNLOVE = "com.arn.scrobble.UNLOVE"
        val pBLACKLIST = "com.arn.scrobble.BLACKLIST"
        val pWHITELIST = "com.arn.scrobble.WHITELIST"
        val iPREFS_CHANGED = "com.arn.scrobble.PREFS_CHANGED"
        val B_TITLE = "title"
        val B_TIME = "time"
        val B_ARTIST = "artist"
        val B_ALBUM = "album"
        val B_DURATION = "duration"
        val NOTI_ID_SCR = "scrobble_success"
        val NOTI_ID_ERR = "err"
        val NOTI_ID_APP = "new_app"
        val NOTI_ERR_ICON = R.drawable.ic_transparent
    }
}