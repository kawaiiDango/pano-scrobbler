@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.os.*
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.media.app.MediaStyleMod
import com.arn.scrobble.receivers.LegacyMetaReceiver
import org.codechimp.apprater.AppRater


class NLService : NotificationListenerService() {
    private lateinit var pref: SharedPreferences
    private lateinit var nm: NotificationManager
    private var sessListener: SessListener? = null
    private var bReceiver: LegacyMetaReceiver? = null
//    private var connectivityCb: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        if (BuildConfig.DEBUG)
            Stuff.toast(applicationContext,getString(R.string.pref_master_on))
//        if (!LeakCanary.isInAnalyzerProcess(this))
//            LeakCanary.install(application)
        super.onCreate()
        Stuff.log("onCreate")
        // lollipop and mm bug
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
            init()
//        KeepNLSAliveJob.ensureServiceRunning(applicationContext)
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
        filter.addAction(pLOVE)
        filter.addAction(pUNLOVE)
        filter.addAction(pWHITELIST)
        filter.addAction(pBLACKLIST)
        filter.addAction(iBAD_META)
        filter.addAction(iOTHER_ERR)
        filter.addAction(CONNECTIVITY_ACTION)
        applicationContext.registerReceiver(nlservicereciver, filter)

        corrrectedDataCache = LruCache(10)

        pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        migratePrefs(pref)
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        handler = ScrobbleHandler()
        val sessManager = applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        sessListener = SessListener(pref, handler)
        try {
            sessManager.addOnActiveSessionsChangedListener(sessListener!!, ComponentName(this, this::class.java))
            //scrobble after the app is updated
            sessListener?.onActiveSessionsChanged(sessManager.getActiveSessions(ComponentName(this, this::class.java)))
            Stuff.log("onListenerConnected")
        } catch (exception: SecurityException) {
            Stuff.log("Failed to start media controller: " + exception.message)
            // Try to unregister it, just in case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Media controller needs notification listener service
            // permissions to be granted.
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) //ok this works
            bReceiver = LegacyMetaReceiver.regIntents(applicationContext)
        initChannels(applicationContext)
        KeepNLSAliveJob.checkAndSchedule(applicationContext)

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isOnline = cm.activeNetworkInfo?.isConnected == true

        sendBroadcast(Intent(iNLS_STARTED))
    }

    private fun destroy() {
        Stuff.log("onListenerDisconnected")
        try {
            applicationContext.unregisterReceiver(nlservicereciver)
//            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            cm.unregisterNetworkCallback(connectivityCb)
        } catch(e:IllegalArgumentException) {
            Stuff.log("nlservicereciver wasn't registered")
        }
        if (sessListener != null) {
            sessListener?.removeSessions(mutableSetOf<MediaSession.Token>())
            (applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                    .removeOnActiveSessionsChangedListener(sessListener!!)
            pref.unregisterOnSharedPreferenceChangeListener(sessListener)
            sessListener = null
            handler.removeCallbacksAndMessages(null)
        }
        if (bReceiver != null) {
            try {
                applicationContext.unregisterReceiver(bReceiver)
                bReceiver = null
            } catch(e:IllegalArgumentException) {
                Stuff.log("LegacyMetaReceiver wasn't registered")
            }
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

    private val nlservicereciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action){
                 pCANCEL -> {
                     handler.removeMessages(intent.getIntExtra(B_HASH, 0))
                     handler.notifyOtherError(getString(R.string.state_unscrobbled), false)
                     handler.postDelayed({
                         if(!handler.hasMessages(SessListener.lastHash)) //dont dismiss if it started showing another np
                            handler.remove(0)
                     }, 1000)
                 }
                pLOVE, pUNLOVE -> {
                    val loved = intent.action == pLOVE
                    LFMRequester(if (loved) Stuff.LOVE else Stuff.UNLOVE,
                            intent.getStringExtra("artist"),
                            intent.getStringExtra("title"))
                            .skipContentProvider()
                            .asAsyncTask(applicationContext)
                    val artist = intent.getStringExtra("artist")
                    val title = intent.getStringExtra("title")
                    val np = handler.hasMessages(artist.hashCode() + title.hashCode())
                    handler.notifyScrobble(intent.getStringExtra("artist"),
                            intent.getStringExtra("title"), np, loved)
                }
                pWHITELIST, pBLACKLIST -> {
                    val wSet = pref.getStringSet(Stuff.PREF_WHITELIST, mutableSetOf())!!
                    val bSet = pref.getStringSet(Stuff.PREF_BLACKLIST, mutableSetOf())!!

                    if (intent.action == pWHITELIST)
                        wSet.add(intent.getStringExtra("packageName"))
                    else {
                        bSet.add(intent.getStringExtra("packageName"))
                    }
                    bSet.removeAll(wSet) //whitelist takes over blacklist for conflicts
                    pref.edit()
                            .putStringSet(Stuff.PREF_WHITELIST, wSet)
                            .putStringSet(Stuff.PREF_BLACKLIST,  bSet)
                            .apply()
                    val key = if (intent.action == pBLACKLIST)
                        Stuff.PREF_BLACKLIST
                    else
                        Stuff.PREF_WHITELIST
                    sessListener?.onSharedPreferenceChanged(pref, key) //it doesnt fire
                    nm.cancel(NOTI_ID_APP, 0)
                }
                iOTHER_ERR -> {
                    handler.remove(intent.getIntExtra(B_HASH, 0))
                    handler.notifyOtherError(intent.getStringExtra(B_ERR_MSG))
                }
                iBAD_META -> {
                    handler.remove(intent.getIntExtra(B_HASH, 0))
                    handler.notifyBadMeta(intent.getStringExtra(B_ARTIST),
                            intent.getStringExtra(B_ALBUM),
                            intent.getStringExtra(B_TITLE),
                            intent.getLongExtra(B_TIME, System.currentTimeMillis()),
                            intent.getStringExtra(B_ERR_MSG)
                            )
                }
                CONNECTIVITY_ACTION -> {
                    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    isOnline =  cm.activeNetworkInfo?.isConnected == true
                }
            }
        }
    }

    inner class ScrobbleHandler : Handler {
        constructor() : super()
        constructor(looper: Looper) : super(looper)

        override fun handleMessage(m: Message) {

            val title = m.data.getString(B_TITLE)!!
            val artist = m.data.getString(B_ARTIST)!!
            val album = m.data.getString(B_ALBUM)!!
            val time = m.data.getLong(B_TIME)
            val duration = m.data.getLong(B_DURATION)
            val packageName = m.data.getString(B_PACKAGE)

            val method = m.data.getInt(B_METHOD)

            if (method == B_NOW_PLAYING) {
                nowPlaying(artist, album, title, duration, m.what, packageName)
            } else if (method == B_SCROBBLE) {
                submitScrobble(artist, album, title, time, duration)
            }

        }

        private fun nowPlaying(artist:String, album:String, title: String, duration:Long, hash:Int, packageName: String?) {
            if (!hasMessages(hash)) {
                val now = System.currentTimeMillis()
                if (artist != "" && title != "") {
                    val album = Stuff.sanitizeAlbum(album)
                    val artist = Stuff.sanitizeArtist(artist)
                    LFMRequester(Stuff.NOW_PLAYING, artist, album, title, now.toString(), duration.toString())
                            .skipContentProvider()
                            .asAsyncTask(applicationContext)

                    val m = obtainMessage()
                    val b = Bundle()
                    b.putString(B_ARTIST, artist)
                    b.putString(B_ALBUM, album)
                    b.putString(B_TITLE, title)
                    b.putLong(B_TIME, now)
                    b.putLong(B_DURATION, duration)
                    b.putInt(B_METHOD, B_SCROBBLE)
                    m.data = b
                    m.what = hash
                    val delaySecs = pref.getInt(Stuff.PREF_DELAY_SECS, 90).toLong() * 1000
                    val delayPer = pref.getInt(Stuff.PREF_DELAY_PER, 50).toLong()
                    val delay = if (duration > 10000 && duration*delayPer/100 < delaySecs) //dont scrobble <10 sec songs?
                        duration*delayPer/100
                    else
                        delaySecs

                    sendMessageDelayed(m, delay)
                    notifyScrobble(artist, title, true, false)
                    //display ignore thing only on successful parse
                    if (packageName != null) {
                        notifyApp(packageName)
                    }
                    //for rating
                    AppRater.incrementScrobbleCount(applicationContext)
                } else {
                    notifyBadMeta(artist, album, title, now, getString(R.string.parse_error))
                }
            }
        }

        private fun submitScrobble(artist:String, album:String, title: String, time:Long, duration:Long) {
            LFMRequester(Stuff.SCROBBLE, artist, album, title, time.toString(), duration.toString())
                    .skipContentProvider()
                    .asAsyncTask(applicationContext)
            notifyScrobble(artist, title, false, false)
        }

        fun scrobble(artist:String, album:String, title: String, duration:Long, packageName: String? = null): Int {
            val m = obtainMessage()
            val b = Bundle()
            val hash = artist.hashCode() + title.hashCode()

            b.putString(B_ARTIST, artist)
            b.putString(B_ALBUM, album)
            b.putString(B_TITLE, title)
            b.putLong(B_DURATION, duration)
            b.putString(B_PACKAGE, packageName)
            b.putInt(B_METHOD, B_NOW_PLAYING)
            m.data = b
            m.what = hash
            removeMessages(SessListener.lastHash)
            sendMessage(m)

            return hash
        }

        fun scrobble(songTitle: String, duration:Long, packageName:String? = null): Int {
            val splits = Stuff.sanitizeTitle(songTitle)
            return scrobble(splits[0], "", splits[1], duration, packageName)
        }

        fun buildNotification(): NotificationCompat.Builder {
            return NotificationCompat.Builder(applicationContext)
                    .setShowWhen(false)
                    .setColor(ContextCompat.getColor(applicationContext, R.color.colorNoti))
                    .setAutoCancel(true)
                    .setCustomBigContentView(null)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
//                    .setOngoing(true) //todo: remove
        }

        fun notifyScrobble(artist: String, title: String, nowPlaying: Boolean, loved: Boolean = false) {
            if (!pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                return
            var i = Intent()
                    .putExtra("artist", artist)
                    .putExtra("title", title)
            val loveAction = if (loved) {
                i.action = pUNLOVE
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 4, i,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                getAction(R.drawable.vd_heart_break, "\uD83D\uDC94", getString(R.string.unlove), loveIntent)
            } else {
                i.action = pLOVE
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 3, i,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                getAction(R.drawable.vd_heart, "❤", getString(R.string.love), loveIntent)
            }

            i = Intent(applicationContext, Main::class.java)
                    .putExtra(Stuff.DEEP_LINK_KEY, Stuff.DL_NOW_PLAYING)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, i,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            i = Intent(pCANCEL)
                    .putExtra(B_HASH, artist.hashCode() + title.hashCode())
            val cancelToastIntent = PendingIntent.getBroadcast(applicationContext, 5, i,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val style= MediaStyleMod()
            val nb = buildNotification()
                    .setChannelId(NOTI_ID_SCR)
                    .setSmallIcon(R.drawable.ic_noti)
                    .setContentIntent(launchIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setStyle(style)
                    .addAction(loveAction)
                    .setContentTitle(title)
                    .setContentText(artist)

            if (nowPlaying) {
                nb.setSubText(getString(R.string.state_scrobbling))
                nb.addAction(getAction(R.drawable.vd_undo, "❌", getString(R.string.unscrobble), cancelToastIntent))
                style.setShowActionsInCompactView(0, 1)
            } else {
                nb.setSubText(getString(R.string.state_scrobbled))
                style.setShowActionsInCompactView(0)
            }

            try {
                nm.notify(NOTI_ID_SCR, 0, nb.buildMediaStyleMod())
            } catch (e: RuntimeException){
                val nExpandable =  nb.setLargeIcon(null)
                        .setStyle(null)
                        .build()
                nm.notify(NOTI_ID_SCR, 0, nExpandable)
            }
        }

        fun notifyBadMeta(artist: String, album: String, title: String, timeMillis: Long, stateText: String?) {
            if (!pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                return
            val i = Intent(applicationContext, EditActivity::class.java)
            i.putExtra(B_ARTIST, artist)
            i.putExtra(B_ALBUM, album)
            i.putExtra(B_TITLE, title)
            i.putExtra(B_TIME, timeMillis)
            i.putExtra(B_STANDALONE, true)

            val editIntent = PendingIntent.getActivity(applicationContext, 9, i,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val nb = buildNotification()
                    .setChannelId(NOTI_ID_ERR)
                    .setSmallIcon(R.drawable.ic_noti_err)
                    .setContentIntent(editIntent)
                    .setContentText(artist)
                    .setContentTitle(
                            (stateText ?: getString(R.string.state_invalid_artist)) + " " + getString(R.string.edit_tags_noti)
                        )
                    .setPriority(
                        if (stateText == null )
                            NotificationCompat.PRIORITY_LOW
                        else
                            NotificationCompat.PRIORITY_MIN
                    )
            nm.notify(NOTI_ID_SCR, 0, nb.build())
        }

        fun notifyOtherError(errMsg: String, showState: Boolean = true) {
            val intent = Intent(applicationContext, Main::class.java)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val nb = buildNotification()
                    .setChannelId(NOTI_ID_SCR)
                    .setSmallIcon(R.drawable.ic_noti_err)
                    .setContentIntent(launchIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentTitle(errMsg)
            if (showState) {
                nb.setSubtextCompat(getString(R.string.state_didnt_scrobble))
                if (errMsg.contains('\n'))
                    nb.setStyle(NotificationCompat.BigTextStyle()
                            .setBigContentTitle(getString(R.string.state_didnt_scrobble))
                            .bigText(errMsg)
                            .setSummaryText(null))
            }
            nm.notify(NOTI_ID_SCR, 0, nb.build())
        }

        fun notifyApp(packageName:String) {
            val appName: String
            try {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                appName = packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: Exception) {
                //eat up all NPEs and stuff
                return
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
            val launchIntent = PendingIntent.getActivity(applicationContext, 7, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val n = buildNotification()
                    .setContentTitle(getString(R.string.new_player, appName))
                    .setContentText(getString(R.string.new_player_prompt))
                    .setChannelId(NOTI_ID_APP)
                    .setSmallIcon(R.drawable.vd_appquestion_noti)
                    .setContentIntent(launchIntent)
                    .addAction(getAction(R.drawable.vd_ban, "\uD83D\uDEAB", getString(R.string.ignore_app), ignoreIntent))
                    .addAction(getAction(R.drawable.vd_check, "✔", getString(R.string.ok_cool), okayIntent))
                    .setStyle(MediaStyleMod().setShowActionsInCompactView(0,1))
                    .buildMediaStyleMod()
            nm.notify(NOTI_ID_APP, 0, n)
        }
/*
        fun notification(title1: String, title2: String?, state: String, iconId: Int, loved: Boolean = true) {
            var title2 = title2
            var iconId = iconId
            if (!pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                return
            if (iconId == 0)
                iconId = R.drawable.ic_noti
            lastNotiIcon = iconId

            var title = title1
            var hash = title1.hashCode()
            if (title2 != null) {
                hash += title2.hashCode()
                title += " - $title2"
            } else {
                title2 = ""
            }

            val loveAction = if (loved){
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
                    .putExtra(Stuff.DEEP_LINK_KEY, Stuff.DL_NOW_PLAYING)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT)

            intent = Intent(pCANCEL_TOAST)
                    .putExtra(B_HASH, hash)
            val cancelToastIntent = PendingIntent.getBroadcast(applicationContext, 5, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val nb = NotificationCompat.Builder(applicationContext,
                    if (iconId == NOTI_ERR_ICON) NOTI_ID_ERR else NOTI_ID_SCR)
                    .setSmallIcon(iconId)
                    .setColor(ContextCompat.getColor(applicationContext, R.color.colorNoti))
                    .setContentIntent(launchIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setAutoCancel(true)
                    .setShowWhen(false)
                    .setPriority(if (iconId == NOTI_ERR_ICON) Notification.PRIORITY_MIN else Notification.PRIORITY_LOW)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || state == getString(R.string.state_didnt_scrobble)){
                nb.setContentTitle(title2)
                        .setContentText(title1)
                if (state != getString(R.string.state_didnt_scrobble))
                    nb.setSubText(state)
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
                    nb.addAction(getAction(R.drawable.vd_undo, "❌", getString(R.string.unscrobble), cancelToastIntent))
//                    nb.setDeleteIntent(cancelToastIntent)
                    style.setShowActionsInCompactView(0, 1)
                } else
                    style.setShowActionsInCompactView(0)
                nb.setStyle(style)
                        .setCustomBigContentView(null)
            }
            val n = nb.buildMediaStyleMod()
            try {
                nm.notify(NOTI_ID_SCR, 0, n)
            } catch (e: RuntimeException){
                val nExpandable =  nb.setLargeIcon(null)
                        .setStyle(null)
                        .build()
                nm.notify(NOTI_ID_SCR, 0, nExpandable)
            }
        }
        */

        private fun getAction(icon:Int, emoji:String, text:String, pIntent:PendingIntent): NotificationCompat.Action {
//            return if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.M)
                return NotificationCompat.Action(icon, text, pIntent)
//            else
//                NotificationCompat.Action(R.drawable.ic_transparent, emoji + " "+ text, pIntent)
        }

        private var notiIconBitmap: Bitmap? = null

        private fun NotificationCompat.Builder.setSubtextCompat(state:String): NotificationCompat.Builder {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                setSubText(state)
            } else {
                setContentInfo(state)
            }
            return this
        }

        private fun NotificationCompat.Builder.buildMediaStyleMod(): Notification {
            val modNeeded = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && mActions != null && mActions.isNotEmpty()
            if (modNeeded) {
                if (notiIconBitmap == null || notiIconBitmap?.isRecycled == true){
                    notiIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                }
//                icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
                this.setLargeIcon(notiIconBitmap)
            }
            val n = build()
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
            Stuff.log(hash.toString() + " cancelled")
            if (hash != 0)
                removeMessages(hash)
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
            nm.createNotificationChannel(NotificationChannel(NOTI_ID_FG,
                    context.getString(R.string.channel_fg), NotificationManager.IMPORTANCE_MIN))
        }

        fun migratePrefs(pref: SharedPreferences) {
            if (pref.contains("username") || pref.contains("sesskey") )
                pref.edit()
                        .putString(Stuff.PREF_LASTFM_USERNAME, pref.getString("username", ""))
                        .putString(Stuff.PREF_LASTFM_SESS_KEY, pref.getString("sesskey", ""))
                        .remove("username")
                        .remove("sesskey")
                        .remove("offline_scrobble")
                        .remove("search_url")
                        .remove(Stuff.PREF_ACTIVITY_NUM_SCROBBLES)
                        .remove(Stuff.PREF_ACTIVITY_PROFILE_PIC)
                        .apply()
        }

        lateinit var corrrectedDataCache: LruCache<Int, Bundle>
        var isOnline = true

        lateinit var handler: ScrobbleHandler
        const val pNLS = "com.arn.scrobble.NLS"
        const val pCANCEL = "com.arn.scrobble.CANCEL"
        const val pLOVE = "com.arn.scrobble.LOVE"
        const val pUNLOVE = "com.arn.scrobble.UNLOVE"
        const val pBLACKLIST = "com.arn.scrobble.BLACKLIST"
        const val pWHITELIST = "com.arn.scrobble.WHITELIST"
        const val iNLS_STARTED = "com.arn.scrobble.NLS_STARTED"
        const val iSESS_CHANGED = "com.arn.scrobble.SESS_CHANGED"
        const val iEDITED = "com.arn.scrobble.EDITED"
        const val iDRAWER_UPDATE = "com.arn.scrobble.DRAWER_UPDATE"
        const val iOTHER_ERR = "com.arn.scrobble.OTHER_ERR"
        const val iBAD_META = "com.arn.scrobble.BAD_META"
        const val B_TITLE = "title"
        const val B_TIME = "time"
        const val B_ARTIST = "artist"
        const val B_ALBUM = "album"
        const val B_DURATION = "duration"
        const val B_METHOD = "method"
        const val B_PACKAGE = "package"
        const val B_HASH = "hash"
        const val B_ERR_MSG = "err"
        const val B_STANDALONE = "alone"
        const val B_NOW_PLAYING = 1
        const val B_SCROBBLE = 2
        const val NOTI_ID_SCR = "scrobble_success"
        const val NOTI_ID_ERR = "err"
        const val NOTI_ID_APP = "new_app"
        const val NOTI_ID_FG = "fg"
    }
}