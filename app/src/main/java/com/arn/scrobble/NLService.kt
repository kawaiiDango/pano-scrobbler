@file:Suppress("DEPRECATION")

package com.arn.scrobble

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_TIME_CHANGED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.session.MediaSessionManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.os.*
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.Html
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.media.app.MediaStyleMod
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.db.PendingScrobblesDb
import com.arn.scrobble.themes.ColorPatchUtils
import de.umass.lastfm.Period
import de.umass.lastfm.scrobble.ScrobbleData
import org.codechimp.apprater.AppRater
import java.text.NumberFormat
import android.media.AudioManager
import com.arn.scrobble.Stuff.toBundle
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.*
import timber.log.Timber


class NLService : NotificationListenerService() {
    private lateinit var pref: SharedPreferences
    private lateinit var nm: NotificationManager
    private var sessListener: SessListener? = null
    lateinit var handler: ScrobbleHandler
    private var lastNpTask: LFMRequester.MyAsyncTask? = null
    private var currentBundle = Bundle()
    private var notiColor = Color.MAGENTA
    private val defaultScope = CoroutineScope(Dispatchers.Default + Job())
    private val isPro
        get() = pref.getBoolean(Stuff.PREF_PRO_STATUS, false)
    private var crashlyticsReporterJob: Job? = null
//    private var connectivityCb: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        if (BuildConfig.DEBUG)
            Stuff.toast(applicationContext,getString(R.string.pref_master_on))
        super.onCreate()
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
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
/*
    override fun onListenerConnected() {
//    This sometimes gets called twice without calling onListenerDisconnected or onDestroy
//    onCreate seems to get called only once in those cases.
//    also unreliable on lp and mm
        super.onListenerConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Looper.myLooper() == null) {
                Handler(mainLooper!!).post { init() }
            } else
                init()
        }
    }
*/

    private fun init(){
        val filter = IntentFilter()
        filter.addAction(iCANCEL)
        filter.addAction(iLOVE)
        filter.addAction(iUNLOVE)
        filter.addAction(iWHITELIST)
        filter.addAction(iBLACKLIST)
        filter.addAction(iBAD_META)
        filter.addAction(iOTHER_ERR)
        filter.addAction(iMETA_UPDATE)
        filter.addAction(iDIGEST_WEEKLY)
        filter.addAction(iDIGEST_MONTHLY)
        filter.addAction(iSCROBBLER_ON)
        filter.addAction(iSCROBBLER_OFF)
        filter.addAction(iTHEME_CHANGED)
        filter.addAction(ACTION_TIME_CHANGED)
        filter.addAction(CONNECTIVITY_ACTION)
        applicationContext.registerReceiver(nlservicereciver, filter)

        val f = IntentFilter()
        f.addDataScheme("package")
        f.addAction(ACTION_PACKAGE_ADDED)
        applicationContext.registerReceiver(pkgInstallReceiver, f)

        pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        notiColor = ColorPatchUtils.getNotiColor(applicationContext, pref)
//        migratePrefs()
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        handler = ScrobbleHandler(mainLooper)
        val sessManager = applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        sessListener = SessListener(pref, handler, audioManager)
        sessListener?.browserPackages = Stuff.getBrowsersAsStrings(packageManager)
        try {
            sessManager.addOnActiveSessionsChangedListener(sessListener!!, ComponentName(this, this::class.java))
            //scrobble after the app is updated
            sessListener?.onActiveSessionsChanged(sessManager.getActiveSessions(ComponentName(this, this::class.java)))
        } catch (exception: SecurityException) {
            Stuff.log("Failed to start media controller: " + exception.message)
            // Try to unregister it, just in case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener!!)
            } catch (e: Exception) {
                Timber.tag(Stuff.TAG).w(e)
            }
            // Media controller needs notification listener service
            // permissions to be granted.
        }
        initChannels()
//        KeepNLSAliveJob.checkAndSchedule(applicationContext)

//        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        isOnline = cm.activeNetworkInfo?.isConnected == true
        Stuff.scheduleDigests(applicationContext, pref)
        sendBroadcast(Intent(iNLS_STARTED))
        BillingRepository.getInstance(application).apply {
            startDataSourceConnections()
            queryPurchasesAsync()
            Handler(Looper.getMainLooper()).postDelayed({ this.endDataSourceConnections() }, 20000)
        }

        crashlyticsReporterJob = CoroutineScope(Dispatchers.Main + Job()).launch {
            while (isActive) {
                delay(Stuff.CRASH_REPORT_INTERVAL)
                FirebaseCrashlytics.getInstance().sendUnsentReports()
            }
        }

        Stuff.log("init")
    }

    private fun destroy() {
        Stuff.log("destroy")
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
            sessListener?.removeSessions(setOf())
            (applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                    .removeOnActiveSessionsChangedListener(sessListener!!)
            pref.unregisterOnSharedPreferenceChangeListener(sessListener)
            sessListener = null
            handler.removeCallbacksAndMessages(null)
        }
        defaultScope.cancel()
        crashlyticsReporterJob?.cancel()
        PendingScrobblesDb.destroyInstance()
    }

    /*
    override fun onListenerDisconnected() { //api 24+ only
        destroy()
        super.onListenerDisconnected()
    }
    */

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
        nm.createNotificationChannel(NotificationChannel(NOTI_ID_DIGESTS,
                getString(R.string.channel_digests), NotificationManager.IMPORTANCE_LOW))
    }

    private fun migratePrefs() {
        if ("username" in pref || "sesskey" in pref)
            pref.edit()
                    .putString(Stuff.PREF_LASTFM_USERNAME, pref.getString("username", ""))
                    .putString(Stuff.PREF_LASTFM_SESS_KEY, pref.getString("sesskey", ""))
                    .remove("username")
                    .remove("sesskey")
                    .remove("offline_scrobble")
                    .remove("search_url")
                    .remove(Stuff.PREF_ACTIVITY_TODAY_SCROBBLES)
                    .remove(Stuff.PREF_ACTIVITY_PROFILE_PIC)
                    .apply()
    }

    private fun isAppEnabled(packageName: String) =
         ((packageName in pref.getStringSet(Stuff.PREF_WHITELIST, setOf())!! ||
                (pref.getBoolean(Stuff.PREF_AUTO_DETECT, true) &&
                        packageName !in pref.getStringSet(Stuff.PREF_BLACKLIST, setOf())!!)))

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (pref.getBoolean(Stuff.PREF_PIXEL_NP, true))
            scrobbleFromNoti(
                sbn,
                removed = false,
                packageNames = listOf(Stuff.PACKAGE_PIXEL_NP, Stuff.PACKAGE_PIXEL_NP_R),
                channelName = Stuff.CHANNEL_PIXEL_NP,
                notiField = Notification.EXTRA_TITLE,
                format = R.string.song_format_string
            )
        if (isAppEnabled(Stuff.PACKAGE_SHAZAM))
            scrobbleFromNoti(
                sbn,
                removed = false,
                packageNames = listOf(Stuff.PACKAGE_SHAZAM),
                channelName = Stuff.CHANNEL_SHAZAM,
                notiField = Notification.EXTRA_TEXT,
                format = R.string.auto_shazam_now_playing
            )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) { //only for >26
        if (reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL ||
                reason == REASON_TIMEOUT || reason == REASON_ERROR) {
            if (pref.getBoolean(Stuff.PREF_PIXEL_NP, true))
                scrobbleFromNoti(
                    sbn,
                    removed = true,
                    packageNames = listOf(Stuff.PACKAGE_PIXEL_NP, Stuff.PACKAGE_PIXEL_NP_R),
                    channelName = Stuff.CHANNEL_PIXEL_NP,
                )
            if (isAppEnabled(Stuff.PACKAGE_SHAZAM))
                scrobbleFromNoti(
                    sbn,
                    removed = true,
                    packageNames = listOf(Stuff.PACKAGE_SHAZAM),
                    channelName = Stuff.CHANNEL_SHAZAM,
                )
        }
    }

    private fun scrobbleFromNoti(sbn: StatusBarNotification?, removed:Boolean,
                                 packageNames: List<String>, channelName: String,
                                 notiField: String = Notification.EXTRA_TITLE,
                                 @StringRes format: Int = 0
    ) {
        if (pref.getBoolean(Stuff.PREF_MASTER, true) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && sbn != null && sbn.packageName in packageNames) {
            val n = sbn.notification
            if (n.channelId == channelName) {
                val title = n.extras.getString(notiField) ?: return
                Stuff.log("${this::scrobbleFromNoti.name} $title removed=$removed")
                if (removed) {
                    handler.remove(currentBundle.getInt(B_HASH))
                    return
                }
                val meta = MetadataUtils.pixelNPExtractMeta(title, getString(format))
                if (meta != null){
                    val hash = Stuff.genHashCode(meta[0], "", meta[1], sbn.packageName)
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
                            System.currentTimeMillis() - currentBundle.getLong(B_TIME) < Stuff.NOTI_SCROBBLE_INTERVAL)
                        Stuff.log("${this::scrobbleFromNoti.name} ignoring possible duplicate")
                    else
                        handler.nowPlaying(
                            meta[0],
                            "",
                            meta[1],
                            "",
                            0,
                            0,
                            hash,
                            null,
                            packageNameArg,
                            true
                        )
                } else
                    Stuff.log("\"${this::scrobbleFromNoti.name} parse failed")
            }
        }
    }

    private fun markAsScrobbled(hash: Int) {
        sessListener?.packageMap?.values?.any {
            if (it.lastScrobbleHash == hash) {
                it.lastScrobbledHash = hash
                true
            } else
                false
        }
    }

    private val nlservicereciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action){
                 iCANCEL -> {
                     val hash: Int
                     if (!intent.hasExtra(B_HASH)) {
                         hash = currentBundle.getInt(B_HASH)
                         if (currentBundle[B_HASH] == null || !handler.hasMessages(hash))
                             return
                         nm.cancel(NOTI_ID_SCR, 0)
                     } else {
                         hash = intent.getIntExtra(B_HASH, 0)
                         handler.notifyTempMsg(getString(R.string.state_unscrobbled))
                     }
                     handler.removeMessages(hash)
                     markAsScrobbled(hash)
                 }
                iLOVE, iUNLOVE -> {
                    val loved = intent.action == iLOVE
                    var artist = intent.getStringExtra(B_ARTIST)
                    var title = intent.getStringExtra(B_TITLE)
                    val hash = currentBundle.getInt(B_HASH, 0)
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
                            if (loved == currentBundle.getBoolean(B_USER_LOVED, false))
                                return
                        } else
                            return
                    }
                    LFMRequester(applicationContext)
                            .skipContentProvider()
                            .loveOrUnlove(loved,  artist!!, title!!)
                            .asSerialAsyncTask()
                    val np = handler.hasMessages(hash)
                    currentBundle.putBoolean(B_USER_LOVED, loved)
                    handler.notifyScrobble(artist,
                            title, hash, np, loved, currentBundle.getInt(B_USER_PLAY_COUNT))
                }
                iWHITELIST, iBLACKLIST -> {
                    //handle pixel_np blacklist in its own settings
                    val pkgName = intent.getStringExtra("packageName")
                    if (pkgName == Stuff.PACKAGE_PIXEL_NP || pkgName == Stuff.PACKAGE_PIXEL_NP_R){
                        if (intent.action == iBLACKLIST) {
                            pref.edit().putBoolean(Stuff.PREF_PIXEL_NP, false).apply()
                            handler.remove(currentBundle.getInt(B_HASH))
                            nm.cancel(NOTI_ID_APP, 0)
                            return
                        }
                    }
                    //create copies
                    val wSet = pref.getStringSet(Stuff.PREF_WHITELIST, mutableSetOf())!!.toMutableSet()
                    val bSet = pref.getStringSet(Stuff.PREF_BLACKLIST, mutableSetOf())!!.toMutableSet()

                    if (intent.action == iWHITELIST)
                        wSet.add(pkgName)
                    else {
                        bSet.add(pkgName)
                    }
                    bSet.removeAll(wSet) //whitelist takes over blacklist for conflicts
                    pref.edit()
                            .putStringSet(Stuff.PREF_WHITELIST, wSet)
                            .putStringSet(Stuff.PREF_BLACKLIST,  bSet)
                            .apply()
                    val key = if (intent.action == iBLACKLIST)
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
                    val scrobbleData = ScrobbleData().apply {
                        artist = intent.getStringExtra(B_ARTIST)!!
                        track = intent.getStringExtra(B_TITLE)!!
                        albumArtist = intent.getStringExtra(B_ALBUM_ARTIST)!!
                        timestamp = (intent.getLongExtra(B_TIME, System.currentTimeMillis())/1000).toInt()
                    }
                    handler.notifyBadMeta(
                        scrobbleData,
                        intent.getIntExtra(B_HASH, 0),
                        intent.getBooleanExtra(B_FORCEABLE, true),
                        intent.getStringExtra(B_ERR_MSG),
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
                iDIGEST_WEEKLY -> {
                    Stuff.scheduleDigests(applicationContext, pref)
                    if (pref.getBoolean(Stuff.PREF_DIGEST_WEEKLY, true))
                        handler.notifyDigest(Period.WEEK)
                }
                iDIGEST_MONTHLY -> {
                    Stuff.scheduleDigests(applicationContext, pref)
                    if (pref.getBoolean(Stuff.PREF_DIGEST_MONTHLY, true))
                        handler.notifyDigest(Period.ONE_MONTH)
                }
                iSCROBBLER_ON -> {
                    pref.edit().putBoolean(Stuff.PREF_MASTER, true).apply()
                    Stuff.toast(context, getString(R.string.pref_master_on))
                }
                iSCROBBLER_OFF -> {
                    pref.edit().putBoolean(Stuff.PREF_MASTER, false).apply()
                    Stuff.toast(context, getString(R.string.pref_master_off))
                }
                iTHEME_CHANGED -> {
                    notiColor = ColorPatchUtils.getNotiColor(applicationContext, pref)
                }
                CONNECTIVITY_ACTION -> {
                    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    Main.isOnline =  cm.activeNetworkInfo?.isConnected == true
                }
                ACTION_TIME_CHANGED -> {
                    Stuff.scheduleDigests(applicationContext, pref)
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
            val hash = m.data.getInt(B_HASH)

            submitScrobble(artist, album, title, albumArtist, time, duration, m.what)
            markAsScrobbled(hash)
        }

        fun nowPlaying(artist:String, album:String, title: String, albumArtist:String, position: Long, duration:Long,
                       hash:Int, ignoredArtist: String?, packageName: String?, lessDelay: Boolean = false) {
            if (title != "" && !hasMessages(hash)){
                val now = System.currentTimeMillis()
                val album = MetadataUtils.sanitizeAlbum(album)
                val artist = MetadataUtils.sanitizeArtist(artist)
                val albumArtist = MetadataUtils.sanitizeAlbum(albumArtist)

                val scrobbleData = ScrobbleData()
                scrobbleData.artist = artist
                scrobbleData.album = album
                scrobbleData.track = title
                scrobbleData.albumArtist = albumArtist
                scrobbleData.timestamp = (now/1000).toInt() // in secs
                scrobbleData.duration = (duration/1000).toInt() // in secs
                lastNpTask?.cancel(true)
                lastNpTask = LFMRequester(applicationContext)
                        .skipContentProvider()
                        .scrobble(true, scrobbleData, hash, ignoredArtist)
                        .asAsyncTask()

                val b = scrobbleData.toBundle().apply {
                    putBoolean(B_FORCEABLE, ignoredArtist == null)
                    putInt(B_HASH, hash)
                    putBoolean(B_IS_SCROBBLING, true)
                }

                val delayMillis = pref.getInt(Stuff.PREF_DELAY_SECS, 90).toLong() * 1000
                val delayPer = pref.getInt(Stuff.PREF_DELAY_PER, 50).toLong()
                var delay = if (duration > 10000 && duration*delayPer/100 < delayMillis) //dont scrobble <10 sec songs?
                    duration*delayPer/100
                else {
                    if (lessDelay)
                        delayMillis*2/3 //esp for pixel now playing
                    else
                        delayMillis
                }
                if (delay - position > 1000)
                    delay -= position
                b.putLong(B_DELAY, delay)
                currentBundle = b

                val m = obtainMessage()
                m.data = b
                m.what = hash
                sendMessageDelayed(m, delay)

                notifyScrobble(artist, title, hash, nowPlaying = true, loved = false)
                if (packageName != null) {
                    notifyApp(packageName)
                }
                //for rating
                AppRater.incrementScrobbleCount(pref)
            }
        }

        private fun submitScrobble(artist:String, album:String, title: String, albumArtist: String, time:Long, duration:Long, hash:Int) {
            val scrobbleData = ScrobbleData()
            scrobbleData.artist = artist
            scrobbleData.album = album
            scrobbleData.track = title
            scrobbleData.albumArtist = albumArtist
            scrobbleData.timestamp = (time/1000).toInt() // in secs
            scrobbleData.duration = (duration/1000).toInt() // in secs
            LFMRequester(applicationContext)
                    .skipContentProvider()
                    .scrobble(false, scrobbleData, hash)
                    .asAsyncTask()

            var userPlayCount = currentBundle.getInt(B_USER_PLAY_COUNT)
            if (userPlayCount > 0)
                currentBundle.putInt(B_USER_PLAY_COUNT, ++userPlayCount)
            notifyScrobble(artist, title, hash, false, currentBundle.getBoolean(B_USER_LOVED), userPlayCount)
        }

        fun buildNotification(): NotificationCompat.Builder {
            val visibility = if (pref.getBoolean(Stuff.PREF_LOCKSCREEN_NOTI, false))
                    NotificationCompat.VISIBILITY_PUBLIC
                else
                    NotificationCompat.VISIBILITY_SECRET
            return NotificationCompat.Builder(applicationContext)
                    .setShowWhen(false)
                    .setColor(notiColor)
                    .setAutoCancel(true)
                    .setCustomBigContentView(null)
                    .setVisibility(visibility)
        }

        fun notifyScrobble(artist: String, title: String, hash:Int, nowPlaying: Boolean, loved: Boolean = false, userPlayCount: Int = 0) {
            if (!pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                return
            var i = Intent()
                    .putExtra(B_ARTIST, artist)
                    .putExtra(B_TITLE, title)
            val loveAction = if (loved) {
                i.action = iUNLOVE
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 4, i,
                        Stuff.updateCurrentOrImmutable)
                getAction(R.drawable.vd_heart_break, "\uD83D\uDC94", getString(R.string.unlove), loveIntent)
            } else {
                i.action = iLOVE
                val loveIntent = PendingIntent.getBroadcast(applicationContext, 3, i,
                        Stuff.updateCurrentOrImmutable)
                getAction(R.drawable.vd_heart, "❤", getString(R.string.love), loveIntent)
            }

            i = Intent(applicationContext, Main::class.java)
                    .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_RECENTS)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, i,
                    Stuff.updateCurrentOrImmutable)

            i = Intent(iCANCEL)
                    .putExtra(B_HASH, hash)
            val cancelToastIntent = PendingIntent.getBroadcast(applicationContext, 5, i,
                    Stuff.updateCurrentOrImmutable)

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
                    .setContentText(resources.getQuantityString(R.plurals.num_scrobbles_noti,
                            userPlayCount,
                            NumberFormat.getInstance().format(userPlayCount)))
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

        fun notifyBadMeta(scrobbleData: ScrobbleData, hash: Int, forcable: Boolean, stateText: String?) {
            if (!pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                return
            val b = scrobbleData.toBundle().apply {
                putBoolean(B_STANDALONE, true)
                putBoolean(B_FORCEABLE, forcable)
            }
            val i = Intent(applicationContext, EditActivity::class.java).apply {
                putExtras(b)
            }

            val editIntent = PendingIntent.getActivity(applicationContext, 9, i,
                    Stuff.updateCurrentOrImmutable)

            val nb = buildNotification()
                    .setAutoCancel(false)
                    .setChannelId(NOTI_ID_ERR)
                    .setSmallIcon(R.drawable.vd_noti_err)
                    .setContentIntent(editIntent)
                    .setContentText(
                        if (stateText == null)
                            scrobbleData.artist
                        else
                            scrobbleData.track
                    )
                    .setContentTitle(
                            (stateText ?: getString(R.string.state_invalid_artist)) + " " +
                                    (if (forcable)
                                        getString(R.string.tap_to_edit_force)
                                    else
                                        getString(R.string.tap_to_edit))
                        )
                    .setPriority(
                        if (stateText == null)
                            NotificationCompat.PRIORITY_LOW
                        else
                            NotificationCompat.PRIORITY_MIN
                    )
            remove(hash, false)
            currentBundle = Bundle()

            nm.notify(NOTI_ID_SCR, 0, nb.build())
            markAsScrobbled(hash)
        }

        fun notifyOtherError(title:String, errMsg: String) {
            val intent = Intent(applicationContext, Main::class.java)
            val launchIntent = PendingIntent.getActivity(applicationContext, 8, intent,
                    Stuff.updateCurrentOrImmutable)
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
                    .setTimeoutAfter(1000)
            nm.notify(NOTI_ID_SCR, 0, nb.build())
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                handler.postDelayed({ nm.cancel(NOTI_ID_SCR, 0) }, 1000)
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

            var intent = Intent(iBLACKLIST)
                    .putExtra("packageName", packageName)
            val ignoreIntent = PendingIntent.getBroadcast(applicationContext, 1, intent,
                    Stuff.updateCurrentOrImmutable)
            intent = Intent(iWHITELIST)
                    .putExtra("packageName", packageName)
            val okayIntent = PendingIntent.getBroadcast(applicationContext, 2, intent,
                    Stuff.updateCurrentOrImmutable)
            intent = Intent(applicationContext, Main::class.java)
                    .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_APP_LIST)
            val launchIntent = PendingIntent.getActivity(applicationContext, 7, intent,
                    Stuff.updateCurrentOrImmutable)

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

        fun notifyDigest(period: Period) {
            if (pref.getString(Stuff.PREF_LASTFM_USERNAME, null) == null)
                return

            val digestMld = MutableLiveData<List<String>>()
            val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.arn.scrobble::digest").apply {
                            acquire(30000)
                        }
                    }

            digestMld.observeForever(object : Observer<List<String>>{

                override fun onChanged(digestArr: List<String>?) {
                    digestArr ?: return
                    digestMld.removeObserver(this)
                    if (digestArr.isEmpty())
                        return

                    val title = getString(R.string.my) + " " + if (period == Period.WEEK)
                        getString(R.string.digest_weekly).lowercase()
                    else
                        getString(R.string.digest_monthly).lowercase()
                    var intent = Intent(Intent.ACTION_SEND)
                            .putExtra("packageName", packageName)
                    var shareText = title + "\n\n" +
                            digestArr.mapIndexed { i, s ->
                                if (i % 2 == 1 && i != digestArr.size - 1)
                                    s + "\n"
                                else
                                    s
                            }
                                .joinToString(separator = "\n")
                    if (!isPro)
                        shareText += "\n\n" + getString(R.string.share_sig)
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/plain"
                    i.putExtra(Intent.EXTRA_SUBJECT, shareText)
                    i.putExtra(Intent.EXTRA_TEXT, shareText)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, shareText)
                    val shareIntent = PendingIntent.getActivity(applicationContext, 10,
                            Intent.createChooser(intent, title),
                            Stuff.updateCurrentOrImmutable)
                    intent = Intent(applicationContext, Main::class.java)
                            .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_CHARTS)
                    val launchIntent = PendingIntent.getActivity(applicationContext, 11, intent,
                            Stuff.updateCurrentOrImmutable)

                    var digestHtml = ""
                    digestArr.forEachIndexed { index, s ->
                        digestHtml += if (index % 2 == 0)
                            "<b>$s</b>"
                        else if (index == digestArr.size - 1)
                            " $s"
                        else
                            " $s<br>"
                    }
                    val spanned = Html.fromHtml(digestHtml)
                    val nb = buildNotification()
                            .setChannelId(NOTI_ID_DIGESTS)
                            .setSmallIcon(R.drawable.vd_charts)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentTitle(title)
                            .setContentIntent(launchIntent)
                            .addAction(getAction(R.drawable.vd_share, "↗", getString(R.string.share), shareIntent))
                            .setContentText(spanned)
                            .setShowWhen(true)
                            .setStyle(NotificationCompat.BigTextStyle()
                                    .setBigContentTitle(title)
                                    .bigText(spanned))
                    nm.notify(NOTI_ID_DIGESTS, period.ordinal, nb.build())
                    wakeLock.release()
                }
            })
            LFMRequester(applicationContext)
                    .skipContentProvider()
                    .getDigest(period)
                    .asAsyncTask(digestMld)
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
            removeMessages(hash)
            if (hash == currentBundle.getInt(B_HASH)) {
                currentBundle.putBoolean(B_IS_SCROBBLING, false)
                if (removeNoti)
                    nm.cancel(NOTI_ID_SCR, 0)
            }
        }
    }

    companion object {
        const val iCANCEL = "com.arn.scrobble.CANCEL"
        const val iLOVE = "com.arn.scrobble.LOVE"
        const val iUNLOVE = "com.arn.scrobble.UNLOVE"
        const val iBLACKLIST = "com.arn.scrobble.BLACKLIST"
        const val iWHITELIST = "com.arn.scrobble.WHITELIST"
        const val iNLS_STARTED = "com.arn.scrobble.NLS_STARTED"
        const val iSESS_CHANGED = "com.arn.scrobble.SESS_CHANGED"
        const val iMETA_UPDATE = "com.arn.scrobble.iMETA_UPDATE"
        const val iOTHER_ERR = "com.arn.scrobble.OTHER_ERR"
        const val iBAD_META = "com.arn.scrobble.BAD_META"
        const val iDIGEST_WEEKLY = "com.arn.scrobble.DIGEST_WEEKLY"
        const val iDIGEST_MONTHLY = "com.arn.scrobble.DIGEST_MONTHLY"
        const val iUPDATE_WIDGET = "com.arn.scrobble.UPDATE_WIDGET"
        const val iSCROBBLER_ON = "com.arn.scrobble.SCROBBLER_ON"
        const val iSCROBBLER_OFF = "com.arn.scrobble.SCROBBLER_OFF"
        const val iTHEME_CHANGED = "com.arn.scrobble.THEME_CHANGED"
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
        const val NOTI_ID_DIGESTS = "digests"
    }
}