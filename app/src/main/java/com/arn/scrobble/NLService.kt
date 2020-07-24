@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.session.MediaSessionManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.os.*
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.Html
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.media.app.MediaStyleMod
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.receivers.LegacyMetaReceiver
import org.codechimp.apprater.AppRater


class NLService : NotificationListenerService() {
    private lateinit var pref: SharedPreferences
    private lateinit var nm: NotificationManager
    private var sessListener: SessListener? = null
    private var legacyMetaReceiver: LegacyMetaReceiver? = null
    private var lastNpTask: LFMRequester.MyAsyncTask? = null
    private var currentBundle = Bundle()
//    private var connectivityCb: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        if (BuildConfig.DEBUG)
            Stuff.toast(applicationContext,getString(R.string.pref_master_on))
        super.onCreate()
        Stuff.log("onCreate")
        // lollipop and mm bug
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
            init()
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
                Handler(mainLooper!!).post { init() }
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
        filter.addAction(iMETA_UPDATE)
        filter.addAction(iDISMISS_MAIN_NOTI)
        filter.addAction(CONNECTIVITY_ACTION)
        applicationContext.registerReceiver(nlservicereciver, filter)

        val f = IntentFilter()
        f.addDataScheme("package")
        f.addAction(ACTION_PACKAGE_ADDED)
        applicationContext.registerReceiver(pkgInstallReceiver, f)

        pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        migratePrefs()
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        handler = ScrobbleHandler(mainLooper)
        val sessManager = applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        sessListener = SessListener(pref, handler)
        sessListener?.browserPackages = Stuff.getBrowsersAsStrings(packageManager)
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
            legacyMetaReceiver = LegacyMetaReceiver.regIntents(applicationContext)
        initChannels()
//        KeepNLSAliveJob.checkAndSchedule(applicationContext)

//        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        isOnline = cm.activeNetworkInfo?.isConnected == true

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
        try {
            applicationContext.unregisterReceiver(pkgInstallReceiver)
        } catch(e:IllegalArgumentException) {
            Stuff.log("pkgInstallReceiver wasn't registered")
        }
        if (sessListener != null) {
            sessListener?.removeSessions(mutableSetOf())
            (applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                    .removeOnActiveSessionsChangedListener(sessListener!!)
            pref.unregisterOnSharedPreferenceChangeListener(sessListener)
            sessListener = null
            handler.removeCallbacksAndMessages(null)
        }
        if (legacyMetaReceiver != null) {
            try {
                applicationContext.unregisterReceiver(legacyMetaReceiver)
                legacyMetaReceiver = null
            } catch(e:IllegalArgumentException) {
                Stuff.log("LegacyMetaReceiver wasn't registered")
            }
        }
        PendingScrobblesDb.destroyInstance()
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

    private fun initChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(NOTI_ID_SCR,
                getString(R.string.channel_scrobbling), NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(NOTI_ID_ERR,
                getString(R.string.channel_err), NotificationManager.IMPORTANCE_MIN))
        nm.createNotificationChannel(NotificationChannel(NOTI_ID_APP,
                getString(R.string.channel_new_app), NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(NOTI_ID_FG,
                getString(R.string.channel_fg), NotificationManager.IMPORTANCE_MIN))
    }

    private fun migratePrefs() {
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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        detectPixelNP(sbn, false)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) { //only for >26
        if (reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL ||
                reason == REASON_TIMEOUT || reason == REASON_ERROR)
            detectPixelNP(sbn, true)
    }

    private fun detectPixelNP(sbn: StatusBarNotification?, removed:Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && sbn != null &&
                pref.getBoolean(Stuff.PREF_PIXEL_NP, true) &&
                (sbn.packageName == Stuff.PACKAGE_PIXEL_NP || sbn.packageName == Stuff.PACKAGE_PIXEL_NP_R)) {
            val n = sbn.notification
            if (n.channelId == Stuff.CHANNEL_PIXEL_NP) {
                Stuff.log("detectPixelNP " + n.extras.getString(Notification.EXTRA_TITLE) + "removed=$removed")
                if (removed) {
                    handler.remove(currentBundle.getInt(B_HASH))
                    return
                }
                val title = n.extras.getString(Notification.EXTRA_TITLE) ?: return
                val meta = Stuff.pixelNPExtractMeta(title, getString(R.string.song_format_string))
                if (meta != null){
                    val hash = Stuff.genHashCode(meta[0], "", meta[1])
                    val packageNameArg =
                            if (pref.getStringSet(Stuff.PREF_WHITELIST, null)?.contains(sbn.packageName) == false)
                                sbn.packageName
                            else
                                null
                    if (handler.hasMessages(currentBundle.getInt(B_HASH)))
                        handler.remove(currentBundle.getInt(B_HASH))

                    val delay = System.currentTimeMillis() - currentBundle.getLong(B_TIME) - currentBundle.getLong(B_DELAY)
                    if (currentBundle.getInt(B_HASH) == hash && delay < 0){ //"resume" scrobbling
                        val m = handler.obtainMessage()
                        m.data = currentBundle
                        m.what = hash
                        handler.sendMessageDelayed(m, -delay)
                        handler.notifyScrobble(meta[0], meta[1], hash, true, currentBundle.getBoolean(B_USER_LOVED))
                    } else if (currentBundle.getInt(B_HASH) == hash &&
                            System.currentTimeMillis() - currentBundle.getLong(B_TIME) < Stuff.PIXEL_NP_INTERVAL)
                        Stuff.log("ignoring possible duplicate")
                    else
                        handler.nowPlaying(meta[0], "", meta[1], "", 0, hash, false, packageNameArg, true)
                } else
                    Stuff.log("detectPixelNP parse failed")
            }
        }
    }

    private val nlservicereciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action){
                 pCANCEL -> {
                     var hash = intent.getIntExtra(B_HASH, 0)
                     if (hash == 0) {
                         hash = currentBundle.getInt(B_HASH, 0)
                         if (hash == 0 || !handler.hasMessages(hash))
                             return
                     } else
                         handler.notifyTempMsg(getString(R.string.state_unscrobbled))
                     handler.removeMessages(hash)
                     handler.postDelayed({
                         if(!handler.hasMessages(SessListener.lastHash)) //dont dismiss if it started showing another np
                            handler.remove(0)
                     }, 1000)
                 }
                pLOVE, pUNLOVE -> {
                    val loved = intent.action == pLOVE
                    var artist = intent.getStringExtra(B_ARTIST)
                    var title = intent.getStringExtra(B_TITLE)
                    if (artist == null && title == null) {
                        if (currentBundle.getBoolean(B_IS_SCROBBLING) && currentBundle.getString(B_ARTIST) != null &&
                                currentBundle.getString(B_TITLE) != null) {
                            artist = currentBundle.getString(B_ARTIST)!!
                            title = currentBundle.getString(B_TITLE)!!
                            Stuff.toast(context, (
                                    if (loved)
                                        "♥"
                                    else
                                        "\uD83D\uDC94"
                                    ) + artist + " — " + title
                            )
                            if (loved == intent.getBooleanExtra(B_USER_LOVED, false))
                                return
                        } else
                            return
                    }

                    LFMRequester(if (loved) Stuff.LOVE else Stuff.UNLOVE, artist!!, title!!)
                            .skipContentProvider()
                            .asSerialAsyncTask(applicationContext)
                    val np = handler.hasMessages(SessListener.lastHash)
                    currentBundle.putBoolean(B_USER_LOVED, loved)
                    handler.notifyScrobble(artist,
                            title, SessListener.lastHash, np, loved, currentBundle.getInt(B_USER_PLAY_COUNT))
                }
                pWHITELIST, pBLACKLIST -> {
                    //handle pixel_np blacklist in its own settings
                    val pkgName = intent.getStringExtra("packageName")
                    if (pkgName == Stuff.PACKAGE_PIXEL_NP || pkgName == Stuff.PACKAGE_PIXEL_NP_R){
                        if (intent.action == pBLACKLIST) {
                            pref.edit().putBoolean(Stuff.PREF_PIXEL_NP, false).apply()
                            handler.remove(currentBundle.getInt(B_HASH))
                            nm.cancel(NOTI_ID_APP, 0)
                            return
                        }
                    }
                    //create copies
                    val wSet = pref.getStringSet(Stuff.PREF_WHITELIST, mutableSetOf())!!.toMutableSet()
                    val bSet = pref.getStringSet(Stuff.PREF_BLACKLIST, mutableSetOf())!!.toMutableSet()

                    if (intent.action == pWHITELIST)
                        wSet.add(pkgName)
                    else {
                        bSet.add(pkgName)
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
                    if (intent.getBooleanExtra(B_PENDING, false))
                        handler.notifyOtherError(getString(R.string.saved_as_pending), intent.getStringExtra(B_ERR_MSG)!!)
                    else
                        handler.notifyOtherError(" ", intent.getStringExtra(B_ERR_MSG)!!)
                }
                iBAD_META -> {
                    handler.remove(intent.getIntExtra(B_HASH, 0), false)
                    handler.notifyBadMeta(intent.getStringExtra(B_ARTIST)!!,
                            intent.getStringExtra(B_ALBUM)!!,
                            intent.getStringExtra(B_TITLE)!!,
                            intent.getLongExtra(B_TIME, System.currentTimeMillis()),
                            intent.getStringExtra(B_ERR_MSG)
                            )
                }
                iMETA_UPDATE -> {
                    if (handler.hasMessages(intent.getIntExtra(B_HASH, 0))) {
                        currentBundle.putAll(intent.extras!!)
                        handler.notifyScrobble(currentBundle.getString(B_ARTIST)!!, currentBundle.getString(B_TITLE)!!,
                                intent.getIntExtra(B_HASH, 0), true,
                                currentBundle.getBoolean(B_USER_LOVED), currentBundle.getInt(B_USER_PLAY_COUNT))
                    }
                }
                iDISMISS_MAIN_NOTI -> {
                    nm.cancel(NOTI_ID_SCR, 0)
                }
                CONNECTIVITY_ACTION -> {
                    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    Main.isOnline =  cm.activeNetworkInfo?.isConnected == true
                }

            }
        }
    }

    private val pkgInstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_PACKAGE_ADDED) {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                    sessListener?.browserPackages = Stuff.getBrowsersAsStrings(packageManager)
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
            val albumArtist = m.data.getString(B_ALBUM_ARTIST)!!
            val time = m.data.getLong(B_TIME)
            val duration = m.data.getLong(B_DURATION)

            submitScrobble(artist, album, title, albumArtist, time, duration, m.what)
        }

        fun nowPlaying(artist:String, album:String, title: String, albumArtist:String, duration:Long,
                       hash:Int, forcable:Boolean, packageName: String?, lessDelay: Boolean = false) {
            removeMessages(SessListener.lastHash)
            if (artist != "" && !hasMessages(hash)){
                val now = System.currentTimeMillis()
                var album = Stuff.sanitizeAlbum(album)
                var artist = Stuff.sanitizeArtist(artist)
                var title = title
                var albumArtist = Stuff.sanitizeAlbum(albumArtist)
                if (artist != "" && title == "") {
                    val dao = PendingScrobblesDb.getDb(applicationContext).getEditsDao()
                    try {
                        dao.find(artist.hashCode().toString() +
                                album.hashCode().toString() + title.hashCode().toString())
                        ?.let {
                            artist = it.artist
                            album = it.album
                            title = it.track
                            albumArtist = it.albumArtist
                        }
                    } catch (e: Exception) {
                        Stuff.log("editsDao exception")
                    }
                }
                if (artist != "" && title != "") {
                    lastNpTask?.cancel(true)
                    lastNpTask = LFMRequester(Stuff.NOW_PLAYING, artist, album, title, albumArtist, now.toString(), duration.toString(), hash.toString())
                        .skipContentProvider()
                        .asSerialAsyncTask(applicationContext)

                    val b = Bundle()
                    b.putString(B_ARTIST, artist)
                    b.putString(B_ALBUM, album)
                    b.putString(B_TITLE, title)
                    b.putString(B_ALBUM_ARTIST, albumArtist)
                    b.putLong(B_TIME, now)
                    b.putLong(B_DURATION, duration)
                    b.putBoolean(B_FORCEABLE, forcable)
                    b.putInt(B_HASH, hash)
                    b.putBoolean(B_IS_SCROBBLING, true)

                    val delaySecs = pref.getInt(Stuff.PREF_DELAY_SECS, 90).toLong() * 1000
                    val delayPer = pref.getInt(Stuff.PREF_DELAY_PER, 50).toLong()
                    val delay = if (duration > 10000 && duration*delayPer/100 < delaySecs) //dont scrobble <10 sec songs?
                        duration*delayPer/100
                    else {
                        if (lessDelay)
                            delaySecs*2/3 //esp for pixel now playing
                        else
                            delaySecs
                    }
                    b.putLong(B_DELAY, delay)
                    currentBundle = b

                    val m = obtainMessage()
                    m.data = b
                    m.what = hash
                    sendMessageDelayed(m, delay)

                    notifyScrobble(artist, title, hash, true, false)
                    //display ignore thing only on successful parse
                    if (packageName != null) {
                        notifyApp(packageName)
                    }
                    //for rating
                    AppRater.incrementScrobbleCount(applicationContext)
                } else {
                    currentBundle = Bundle()
                    notifyBadMeta(artist, album, title, now, getString(R.string.parse_error))
                }
            }
        }

        private fun submitScrobble(artist:String, album:String, title: String, albumArtist: String, time:Long, duration:Long, hash:Int) {
            lastNpTask = null
            LFMRequester(Stuff.SCROBBLE, artist, album, title, albumArtist, time.toString(), duration.toString(), hash.toString())
                    .skipContentProvider()
                    .asSerialAsyncTask(applicationContext)
            notifyScrobble(artist, title, hash, false, currentBundle.getBoolean(B_USER_LOVED), currentBundle.getInt(B_USER_PLAY_COUNT))
        }

        fun buildNotification(): NotificationCompat.Builder {
            return NotificationCompat.Builder(applicationContext)
                    .setShowWhen(false)
                    .setColor(ContextCompat.getColor(applicationContext, R.color.colorNoti))
                    .setAutoCancel(true)
                    .setCustomBigContentView(null)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        }

        fun notifyScrobble(artist: String, title: String, hash:Int, nowPlaying: Boolean, loved: Boolean = false, userPlayCount: Int = 0) {
            if (!pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                return
            var i = Intent()
                    .putExtra(B_ARTIST, artist)
                    .putExtra(B_TITLE, title)
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
                    .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_NOW_PLAYING)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, i,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            i = Intent(pCANCEL)
                    .putExtra(B_HASH, hash)
            val cancelToastIntent = PendingIntent.getBroadcast(applicationContext, 5, i,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val style= MediaStyleMod()
            val nb = buildNotification()
                    .setAutoCancel(false)
                    .setChannelId(NOTI_ID_SCR)
                    .setSmallIcon(R.drawable.vd_noti)
                    .setContentIntent(launchIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setStyle(style)
                    .addAction(loveAction)
                    .setOngoing(Stuff.persistentNoti)
            if (userPlayCount > 0)
                nb.setContentTitle("$artist — $title")
                    .setContentText(resources.getQuantityString(R.plurals.num_scrobbles_noti, userPlayCount, userPlayCount))
            else
                nb.setContentTitle(title)
                    .setContentText(artist)

            if (nowPlaying) {
                nb.setSubText(getString(R.string.state_scrobbling))
                nb.addAction(getAction(R.drawable.vd_undo, "❌", getString(R.string.unscrobble), cancelToastIntent))
                if (resources.getBoolean(R.bool.is_rtl))
                    style.setShowActionsInCompactView(1, 0)
                else
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
            i.putExtra(B_FORCEABLE, currentBundle.getBoolean(B_FORCEABLE))

            val editIntent = PendingIntent.getActivity(applicationContext, 9, i,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val nb = buildNotification()
                    .setAutoCancel(false)
                    .setChannelId(NOTI_ID_ERR)
                    .setSmallIcon(R.drawable.vd_noti_err)
                    .setContentIntent(editIntent)
                    .setContentText(artist)
                    .setContentTitle(
                            (stateText ?: getString(R.string.state_invalid_artist)) + " " +
                                    (if (currentBundle.getBoolean(B_FORCEABLE))
                                        getString(R.string.tap_to_edit_force)
                                    else
                                        getString(R.string.tap_to_edit))
                        )
                    .setPriority(
                        if (stateText == null )
                            NotificationCompat.PRIORITY_LOW
                        else
                            NotificationCompat.PRIORITY_MIN
                    )
            nm.notify(NOTI_ID_SCR, 0, nb.build())
        }

        fun notifyOtherError(title:String, errMsg: String) {
            val intent = Intent(applicationContext, Main::class.java)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            val spanned = Html.fromHtml(errMsg)

            val nb = buildNotification()
                    .setChannelId(NOTI_ID_SCR)
                    .setSmallIcon(R.drawable.vd_noti_err)
                    .setContentIntent(launchIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentText(spanned) //required on recent oneplus devices

            val isMinimised = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    nm.getNotificationChannel(NOTI_ID_SCR).importance < NotificationManager.IMPORTANCE_LOW
            if (isMinimised)
                nb.setContentTitle(errMsg.replace("</?br?>".toRegex(), ""))
            else
                nb.setContentTitle(title)
            nb.setSubtextCompat(getString(R.string.state_didnt_scrobble))

            nb.setStyle(NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(spanned))

            nm.notify(NOTI_ID_SCR, 0, nb.build())
        }

        fun notifyTempMsg(msg: String) {
            val nb = buildNotification()
                    .setChannelId(NOTI_ID_SCR)
                    .setSmallIcon(R.drawable.vd_noti_err)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentTitle(msg)
            nm.notify(NOTI_ID_SCR, 0, nb.build())
        }

        fun notifyApp(packageName:String) {
            val appName =
            try {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: Exception) {
                //eat up all NPEs and stuff
                packageName
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
                    .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_APP_LIST)
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
                    .setStyle(
                            if (resources.getBoolean(R.bool.is_rtl))
                        MediaStyleMod().setShowActionsInCompactView(1, 0)
                    else
                        MediaStyleMod().setShowActionsInCompactView(0, 1)
                    )
                    .buildMediaStyleMod()
            nm.notify(NOTI_ID_APP, 0, n)
        }

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

        @SuppressLint("RestrictedApi")
        private fun NotificationCompat.Builder.buildMediaStyleMod(): Notification {
            val modNeeded = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && mActions != null && mActions.isNotEmpty()
            if (modNeeded) {
                if (notiIconBitmap == null || notiIconBitmap?.isRecycled == true){
                    notiIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
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

        fun remove(hash: Int, removeNoti: Boolean = true) {
            Stuff.log("$hash cancelled")
            currentBundle.putBoolean(B_IS_SCROBBLING, false)
            if (hash != 0)
                removeMessages(hash)
            if (removeNoti)
                nm.cancel(NOTI_ID_SCR, 0)
        }
    }

    companion object {

        lateinit var handler: ScrobbleHandler
        const val pNLS = "com.arn.scrobble.NLS"
        const val pCANCEL = "com.arn.scrobble.CANCEL"
        const val pLOVE = "com.arn.scrobble.LOVE"
        const val pUNLOVE = "com.arn.scrobble.UNLOVE"
        const val pBLACKLIST = "com.arn.scrobble.BLACKLIST"
        const val pWHITELIST = "com.arn.scrobble.WHITELIST"
        const val iDISMISS_MAIN_NOTI = "com.arn.scrobble.DISMISS_MAIN_NOTI"
        const val iNLS_STARTED = "com.arn.scrobble.NLS_STARTED"
        const val iSESS_CHANGED = "com.arn.scrobble.SESS_CHANGED"
        const val iEDITED = "com.arn.scrobble.EDITED"
        const val iDRAWER_UPDATE = "com.arn.scrobble.DRAWER_UPDATE"
        const val iMETA_UPDATE = "com.arn.scrobble.iMETA_UPDATE"
        const val iOTHER_ERR = "com.arn.scrobble.OTHER_ERR"
        const val iBAD_META = "com.arn.scrobble.BAD_META"
        const val B_TITLE = "title"
        const val B_ALBUM_ARTIST = "albumartist"
        const val B_TIME = "time"
        const val B_ARTIST = "artist"
        const val B_ALBUM = "album"
        const val B_DURATION = "duration"
        const val B_USER_PLAY_COUNT = "playcount"
        const val B_USER_LOVED = "loved"
        const val B_HASH = "hash"
        const val B_ERR_MSG = "err"
        const val B_PENDING = "pending_saved"
        const val B_STANDALONE = "alone"
        const val B_FORCEABLE = "forceable"
        const val B_DELAY = "delay"
        const val B_IS_SCROBBLING = "scrobbling"
        const val NOTI_ID_SCR = "scrobble_success"
        const val NOTI_ID_ERR = "err"
        const val NOTI_ID_APP = "new_app"
        const val NOTI_ID_FG = "fg"
    }
}