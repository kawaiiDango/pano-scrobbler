package com.arn.scrobble

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_TIME_CHANGED
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.Html
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.media.app.MediaStyleMod
import com.arn.scrobble.LocaleUtils.getStringInDeviceLocale
import com.arn.scrobble.LocaleUtils.setLocaleCompat
import com.arn.scrobble.PlayerActions.love
import com.arn.scrobble.PlayerActions.skip
import com.arn.scrobble.PlayerActions.unlove
import com.arn.scrobble.Stuff.getScrobblerExitReasons
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.Stuff.isChannelEnabled
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.UiUtils.toast
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import java.text.NumberFormat
import java.util.Objects
import java.util.PriorityQueue
import kotlin.math.min


class NLService : NotificationListenerService() {
    private val prefs by lazy { MainPrefs(applicationContext) }
    private val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private var sessListener: SessListener? = null
    private lateinit var scrobbleHandler: ScrobbleHandler
    private var lastNpTask: LFMRequester? = null
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var packageTrackMap: MutableMap<String, PlayingTrackInfo> // package name to track info
    private var notiColor: Int? = Color.MAGENTA
    private var job: Job? = null
    private val browserPackages = mutableSetOf<String>()

    override fun onCreate() {
        if (BuildConfig.DEBUG)
            toast(R.string.scrobbler_on)
        super.onCreate()
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
        init()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.setLocaleCompat() ?: return)
    }

    //from https://gist.github.com/xinghui/b2ddd8cffe55c4b62f5d8846d5545bf9
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) =
        Service.START_STICKY


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

    private fun init() {
        initChannels()

        job = SupervisorJob()
        coroutineScope = CoroutineScope(Dispatchers.IO + job!!)

        val filter = IntentFilter().apply {
            addAction(iCANCEL)
            addAction(iLOVE)
            addAction(iUNLOVE)
            addAction(iALLOWLIST)
            addAction(iBLOCKLIST)
            addAction(iSCROBBLER_ON)
            addAction(iSCROBBLER_OFF)

            addAction(ACTION_TIME_CHANGED)
        }
        applicationContext.registerReceiver(nlserviceReciver, filter)

        val pkgFilter = IntentFilter().apply {
            addDataScheme("package")
            addAction(ACTION_PACKAGE_ADDED)
        }
        applicationContext.registerReceiver(pkgInstallReceiver, pkgFilter)

        val sFilter = IntentFilter().apply {
            addAction(iTHEME_CHANGED_S)
            addAction(iNOW_PLAYING_INFO_REQUEST_S)
            addAction(iBAD_META_S)
            addAction(iOTHER_ERR_S)
            addAction(iMETA_UPDATE_S)
            addAction(iBLOCK_ACTION_S)
        }
        applicationContext.registerReceiver(
            nlserviceReciverWithPermission,
            sFilter,
            BROADCAST_PERMISSION,
            null
        )

        notiColor = ColorPatchUtils.getNotiColor(applicationContext)

        scrobbleHandler = ScrobbleHandler(mainLooper)
        val sessManager =
            applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        sessListener = SessListener(scrobbleHandler, audioManager, browserPackages)
        packageTrackMap = sessListener!!.packageTrackMap
        browserPackages += Stuff.getBrowsersAsStrings(packageManager)
        try {
            sessManager.addOnActiveSessionsChangedListener(
                sessListener!!,
                ComponentName(this, this::class.java)
            )
            //scrobble after the app is updated
            sessListener?.onActiveSessionsChanged(
                sessManager.getActiveSessions(
                    ComponentName(
                        this,
                        this::class.java
                    )
                )
            )
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

        DigestJob.scheduleAlarms(applicationContext)
//      Don't instantiate BillingRepository in this service, it causes unexplained ANRs

        if (prefs.notiPersistent)
            ContextCompat.startForegroundService(
                this,
                Intent(this, PersistentNotificationService::class.java)
            )

        (application as App).initConnectivityCheck()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getScrobblerExitReasons(printAll = true)
        }

        Stuff.log("init")
    }

    private fun destroy() {
        Stuff.log("destroy")
        try {
            applicationContext.unregisterReceiver(nlserviceReciver)
        } catch (e: IllegalArgumentException) {
            Stuff.log("nlservicereciver wasn't registered")
        }
        try {
            applicationContext.unregisterReceiver(pkgInstallReceiver)
        } catch (e: IllegalArgumentException) {
        }
        try {
            applicationContext.unregisterReceiver(nlserviceReciverWithPermission)
        } catch (e: IllegalArgumentException) {
        }
        if (sessListener != null) {
            sessListener?.removeSessions(setOf())
            (applicationContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                .removeOnActiveSessionsChangedListener(sessListener!!)
            sessListener!!.unregisterPrefsChangeListener()
            sessListener = null
            scrobbleHandler.removeCallbacksAndMessages(null)
        }
        job?.cancel()
        PanoDb.destroyInstance()
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

        val channels = nm.notificationChannels

        // delete old channels, if they exist
        if (channels?.any { it.id == "fg" } == true) {
            channels.forEach { nm.deleteNotificationChannel(it.id) }
        }

        nm.createNotificationChannel(
            NotificationChannel(
                MainPrefs.CHANNEL_NOTI_SCROBBLING,
                getString(R.string.state_scrobbling), NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                MainPrefs.CHANNEL_NOTI_SCR_ERR,
                getString(R.string.channel_err), NotificationManager.IMPORTANCE_MIN
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                MainPrefs.CHANNEL_NOTI_NEW_APP,
                getString(R.string.new_player, getString(R.string.new_app)),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                MainPrefs.CHANNEL_NOTI_PENDING,
                getString(R.string.pending_scrobbles), NotificationManager.IMPORTANCE_MIN
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY,
                getString(R.string.s_top_scrobbles, getString(R.string.weekly)),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                MainPrefs.CHANNEL_NOTI_DIGEST_MONTHLY,
                getString(R.string.s_top_scrobbles, getString(R.string.monthly)),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun isAppEnabled(pkgName: String) =
        pkgName in prefs.allowedPackages || (prefs.autoDetectApps && pkgName !in prefs.blockedPackages)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        scrobbleFromNoti(
            sbn,
            removed = false,
            packageNames = Stuff.PACKAGES_PIXEL_NP,
            channelName = Stuff.CHANNEL_PIXEL_NP,
            notiField = Notification.EXTRA_TITLE,
            format = R.string.song_format_string
        )
        scrobbleFromNoti(
            sbn,
            removed = false,
            packageNames = listOf(Stuff.PACKAGE_SHAZAM),
            channelName = Stuff.CHANNEL_SHAZAM,
            notiField = Notification.EXTRA_TEXT,
            format = R.string.auto_shazam_now_playing
        )
        if (BuildConfig.DEBUG) {
            scrobbleFromNoti(
                sbn,
                removed = false,
                packageNames = listOf(packageName),
                channelName = MainPrefs.CHANNEL_TEST_SCROBBLE_FROM_NOTI,
                notiField = Notification.EXTRA_TITLE,
                format = R.string.song_format_string
            )
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) { //only for >26
        if (reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL ||
            reason == REASON_TIMEOUT || reason == REASON_ERROR
        ) {
            scrobbleFromNoti(
                sbn,
                removed = true,
                packageNames = Stuff.PACKAGES_PIXEL_NP,
                channelName = Stuff.CHANNEL_PIXEL_NP,
            )
            scrobbleFromNoti(
                sbn,
                removed = true,
                packageNames = listOf(Stuff.PACKAGE_SHAZAM),
                channelName = Stuff.CHANNEL_SHAZAM,
            )
            if (BuildConfig.DEBUG) {
                scrobbleFromNoti(
                    sbn,
                    removed = true,
                    packageNames = listOf(packageName),
                    channelName = MainPrefs.CHANNEL_TEST_SCROBBLE_FROM_NOTI,
                )
            }
        }
    }

    private fun scrobbleFromNoti(
        sbn: StatusBarNotification?,
        removed: Boolean,
        packageNames: Collection<String>,
        channelName: String,
        notiField: String = Notification.EXTRA_TITLE,
        @StringRes format: Int = 0
    ) {
        if (prefs.scrobblerEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            sbn != null &&
            sbn.packageName in packageNames &&
            isAppEnabled(sbn.packageName)
        ) {
            val n = sbn.notification
            if (n.channelId == channelName) {
                val notiText = n.extras.getString(notiField) ?: return
                val trackInfo = packageTrackMap[sbn.packageName]

                Stuff.log("${this::scrobbleFromNoti.name} $notiText removed=$removed")

                if (removed) {
                    scrobbleHandler.remove(trackInfo?.hash ?: return)
                    return
                }
                val meta = MetadataUtils.scrobbleFromNotiExtractMeta(
                    notiText,
                    getStringInDeviceLocale(format)
                )
                if (meta != null) {
                    val (artist, title) = meta
                    val hash = Objects.hash(artist, "", title, sbn.packageName)
                    if (trackInfo != null && trackInfo.hash == hash) {
                        val scrobbleTimeReached =
                            SystemClock.elapsedRealtime() >= trackInfo.scrobbleElapsedRealtime
                        if (!scrobbleTimeReached && !scrobbleHandler.has(hash)) { //"resume" scrobbling
                            scrobbleHandler.addScrobble(trackInfo.copy())
                            notifyScrobble(trackInfo)
                            Stuff.log("${this::scrobbleFromNoti.name} rescheduling")
                        } else if (System.currentTimeMillis() - trackInfo.playStartTime < Stuff.NOTI_SCROBBLE_INTERVAL) {
                            Stuff.log("${this::scrobbleFromNoti.name} ignoring possible duplicate")
                        }
                    } else {
                        // different song, scrobble it
                        trackInfo?.let { scrobbleHandler.remove(it.hash, false) }
                        val newTrackInfo = PlayingTrackInfo(
                            playStartTime = System.currentTimeMillis(),
                            hash = hash,
                            packageName = sbn.packageName,
                        )
                        newTrackInfo.putOriginals(artist, title)

                        packageTrackMap[sbn.packageName] = newTrackInfo
                        scrobbleHandler.nowPlaying(
                            newTrackInfo,
                            prefs.delaySecs.coerceAtMost(3 * 60) * 1000L
                        )
                    }
                } else {
                    Stuff.log("\"${this::scrobbleFromNoti.name} parse failed")
                }
            }
        }
    }


    private fun buildNotification(): NotificationCompat.Builder {
        val visibility = if (prefs.notiLockscreen)
            NotificationCompat.VISIBILITY_PUBLIC
        else
            NotificationCompat.VISIBILITY_SECRET
        return NotificationCompat.Builder(applicationContext)
            .setShowWhen(false)
            .apply { color = (notiColor ?: return@apply) }
            .setAutoCancel(true)
            .setCustomBigContentView(null)
            .setVisibility(visibility)
    }

    private fun notifyScrobble(trackInfo: PlayingTrackInfo) {
        if (!nm.isChannelEnabled(
                prefs.sharedPreferences,
                MainPrefs.CHANNEL_NOTI_SCROBBLING
            )
        )
            return

        val nowPlaying = scrobbleHandler.has(trackInfo.hash)

        var i = Intent()
            .putExtra(B_HASH, trackInfo.hash)
        val loveAction = if (trackInfo.userLoved) {
            i.action = iUNLOVE
            val loveIntent = PendingIntent.getBroadcast(
                applicationContext, 4, i,
                Stuff.updateCurrentOrImmutable
            )
            Stuff.getNotificationAction(
                R.drawable.vd_heart_break,
                "\uD83D\uDC94",
                getString(R.string.unlove),
                loveIntent
            )
        } else {
            i.action = iLOVE
            val loveIntent = PendingIntent.getBroadcast(
                applicationContext, 3, i,
                Stuff.updateCurrentOrImmutable
            )
            Stuff.getNotificationAction(
                R.drawable.vd_heart,
                "\uD83E\uDD0D",
                getString(R.string.love),
                loveIntent
            )
        }

        i = Intent(applicationContext, MainActivity::class.java)
            .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_RECENTS)
        val launchIntent = PendingIntent.getActivity(
            applicationContext, 8, i,
            Stuff.updateCurrentOrImmutable
        )

        i = Intent(iCANCEL)
            .putExtra(B_HASH, trackInfo.hash)
            .putSingle(ScrobbleError(getString(R.string.state_unscrobbled)))

        val cancelToastIntent = PendingIntent.getBroadcast(
            applicationContext, 5, i,
            Stuff.updateCurrentOrImmutable
        )

        val state =
            if (nowPlaying)
                ""
//                    "▷ "
            else
                "✓ "

        val style = MediaStyleMod()
        val nb = buildNotification()
            .setAutoCancel(false)
            .setChannelId(MainPrefs.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti)
            .setContentIntent(launchIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyleCompat(style)
            .addAction(loveAction)
        if (trackInfo.userPlayCount > 0)
            nb.setContentTitle(
                state + getString(
                    R.string.artist_title,
                    trackInfo.artist,
                    trackInfo.title
                )
            )
                .setContentText(
                    resources.getQuantityString(
                        R.plurals.num_scrobbles_noti,
                        trackInfo.userPlayCount,
                        NumberFormat.getInstance().format(trackInfo.userPlayCount)
                    )
                )
        else
            nb.setContentTitle(state + trackInfo.title)
                .setContentText(trackInfo.artist)

        if (nowPlaying) {
//                nb.setSubText(getString(R.string.state_scrobbling))
            nb.addAction(
                Stuff.getNotificationAction(
                    R.drawable.vd_remove,
                    "⛔️",
                    getString(R.string.unscrobble),
                    cancelToastIntent
                )
            )
            if (resources.getBoolean(R.bool.is_rtl))
                style.setShowActionsInCompactView(1, 0)
            else
                style.setShowActionsInCompactView(0, 1)
        } else {
//                nb.setSubText(getString(R.string.state_scrobbled))
            style.setShowActionsInCompactView(0)
        }

        try {
            nm.notify(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0, nb.buildMediaStyleMod())
        } catch (e: RuntimeException) {
            val nExpandable = nb.setLargeIcon(null)
                .setStyle(null)
                .build()
            nm.notify(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0, nExpandable)
        }
    }

    private fun notifyBadMeta(trackInfo: PlayingTrackInfo, scrobbleError: ScrobbleError) {
        val i = Intent(applicationContext, MainDialogActivity::class.java)
            .putExtras(trackInfo.toMultiFieldBundle())

        val editIntent = PendingIntent.getActivity(
            applicationContext, 9, i,
            Stuff.updateCurrentOrImmutable
        )

        val subtitleSpanned = if (scrobbleError.description != null)
            Html.fromHtml(scrobbleError.description)
        else
            getString(R.string.artist_title, trackInfo.artist, trackInfo.title)

        val nb = buildNotification()
            .setAutoCancel(false)
            .setChannelId(MainPrefs.CHANNEL_NOTI_SCR_ERR)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentIntent(editIntent)
            .setContentText(subtitleSpanned)
            .setContentTitle("${trackInfo.title} " + getString(R.string.tap_to_edit))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyleCompat(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(scrobbleError.title)
                    .bigText(subtitleSpanned)
            )
        scrobbleHandler.remove(trackInfo.hash, false)

        nm.notify(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0, nb.build())
        sessListener?.findTrackInfoByHash(trackInfo.hash)?.markAsScrobbled()
    }

    private fun notifyOtherError(scrobbleError: ScrobbleError) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            applicationContext, 8, intent,
            Stuff.updateCurrentOrImmutable
        )
        val spanned = Html.fromHtml(scrobbleError.description)

        val nb = buildNotification()
            .setChannelId(MainPrefs.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentIntent(launchIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentText(spanned) //required on recent oneplus devices

        val isMinimised = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                nm.getNotificationChannel(MainPrefs.CHANNEL_NOTI_SCROBBLING).importance < NotificationManager.IMPORTANCE_LOW
        if (isMinimised)
            nb.setContentTitle(scrobbleError.description?.replace("</?br?>".toRegex(), ""))
        else
            nb.setContentTitle(scrobbleError.title)

        nb.setStyleCompat(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(scrobbleError.title)
                .bigText(spanned)
        )

        nm.notify(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0, nb.build())
    }

    private fun notifyUnscrobbled(hash: Int) {
        val delay = 4000L
        val trackInfo = sessListener?.findTrackInfoByHash(hash) ?: return

        val blockedMetadata = BlockedMetadata(
            track = trackInfo.title,
            album = trackInfo.album,
            artist = trackInfo.artist,
            albumArtist = trackInfo.albumArtist,
            skip = true,
        )
        val intent = Intent(this, MainDialogActivity::class.java).apply {
            putSingle(blockedMetadata)
            putExtra(B_IGNORED_ARTIST, trackInfo.origArtist)
            putExtra(B_HASH, hash)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            20,
            intent,
            Stuff.updateCurrentOrImmutable
        )

        val nb = buildNotification()
            .setChannelId(MainPrefs.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(getString(R.string.state_unscrobbled) + " • " + getString(R.string.blocked_metadata_noti))
            .setContentIntent(pendingIntent)
            .setTimeoutAfter(delay)
        nm.notify(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0, nb.build())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            scrobbleHandler.postDelayed({ nm.cancel(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0) }, delay)
    }

    private fun notifyAppDetected(pkgName: String) {
        val appName = try {
            val applicationInfo = packageManager.getApplicationInfo(pkgName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            //eat up all NPEs and stuff
            pkgName
        }

        var intent = Intent(iBLOCKLIST)
            .putExtra(B_PACKAGE_NAME, pkgName)
        val ignoreIntent = PendingIntent.getBroadcast(
            applicationContext, 1, intent,
            Stuff.updateCurrentOrImmutable
        )
        intent = Intent(iALLOWLIST)
            .putExtra(B_PACKAGE_NAME, pkgName)
        val okayIntent = PendingIntent.getBroadcast(
            applicationContext, 2, intent,
            Stuff.updateCurrentOrImmutable
        )
        intent = Intent(applicationContext, MainActivity::class.java)
            .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_APP_LIST)
        val launchIntent = PendingIntent.getActivity(
            applicationContext, 7, intent,
            Stuff.updateCurrentOrImmutable
        )

        val n = buildNotification()
            .setContentTitle(getString(R.string.new_player, appName))
            .setContentText(getString(R.string.new_player_prompt))
            .setChannelId(MainPrefs.CHANNEL_NOTI_NEW_APP)
            .setSmallIcon(R.drawable.vd_appquestion_noti)
            .setContentIntent(launchIntent)
            .addAction(
                Stuff.getNotificationAction(
                    R.drawable.vd_ban,
                    "\uD83D\uDEAB",
                    getString(R.string.no),
                    ignoreIntent
                )
            )
            .addAction(
                Stuff.getNotificationAction(
                    R.drawable.vd_check,
                    "✔",
                    getString(R.string.yes),
                    okayIntent
                )
            )
            .setStyleCompat(
                if (resources.getBoolean(R.bool.is_rtl))
                    MediaStyleMod().setShowActionsInCompactView(1, 0)
                else
                    MediaStyleMod().setShowActionsInCompactView(0, 1)
            )
            .buildMediaStyleMod()
        nm.notify(MainPrefs.CHANNEL_NOTI_NEW_APP, 0, n)
    }

    private var notiIconBitmap: Bitmap? = null

    private fun NotificationCompat.Builder.setSubtextCompat(state: String): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setSubText(state)
        } else {
            setContentInfo(state)
        }
        return this
    }

    private fun NotificationCompat.Builder.setStyleCompat(style: NotificationCompat.Style): NotificationCompat.Builder {
        if (!(Stuff.isWindows11 && style is MediaStyleMod))
            setStyle(style)

        return this
    }

    @SuppressLint("RestrictedApi")
    private fun NotificationCompat.Builder.buildMediaStyleMod(): Notification {
        val modNeeded =
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && !mActions.isNullOrEmpty()
        if (modNeeded) {
            if (notiIconBitmap == null || notiIconBitmap?.isRecycled == true) {
                notiIconBitmap =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.ic_launcher)
                        ?.toBitmap()
            }
//                icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
            setLargeIcon(notiIconBitmap)
        }
        return build()
    }

//    private fun broadcastNowPlayingInfo() {
//        val i = Intent(iNOW_PLAYING_INFO_S).apply {
//            putExtras(currentBundle)
//        }
//        sendBroadcast(i, BROADCAST_PERMISSION)
//    }

    private val nlserviceReciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                iCANCEL -> {
                    val hash: Int
                    if (!intent.hasExtra(B_HASH)) {
                        val trackInfo = packageTrackMap.values.find { it.isPlaying } ?: return
                        hash = trackInfo.hash
                        if (!scrobbleHandler.has(hash))
                            return
                        nm.cancel(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0)
                        trackInfo.markAsScrobbled()
                    } else {
                        hash = intent.getIntExtra(B_HASH, 0)
                        sessListener?.findTrackInfoByHash(hash)?.markAsScrobbled()
                        notifyUnscrobbled(hash)
                    }
                    scrobbleHandler.remove(hash, false)
                }

                iLOVE, iUNLOVE -> {
                    val loved = intent.action == iLOVE
                    val hash = intent.getIntExtra(B_HASH, 0)
                    val trackInfo = if (!intent.hasExtra(B_HASH)) {
                        packageTrackMap.values.find { it.isPlaying } ?: return
                    } else {
                        sessListener?.findTrackInfoByHash(hash) ?: return
                    }

                    if (trackInfo.artist.isEmpty() || trackInfo.title.isEmpty())
                        return

                    if (hash == 0) {
                        // called from automation app
                        toast(
                            (if (loved)
                                "♥"
                            else
                                "\uD83D\uDC94"
                                    ) + getString(
                                R.string.artist_title,
                                trackInfo.artist,
                                trackInfo.title
                            )
                        )
                    }

                    LFMRequester(applicationContext, coroutineScope)
                        .loveOrUnlove(Track(trackInfo.title, null, trackInfo.artist), loved)

                    trackInfo.userLoved = loved
                    notifyScrobble(trackInfo)
                    if (BuildConfig.DEBUG)
                        sessListener?.findControllersByPackage(trackInfo.packageName)?.apply {
                            if (loved)
                                love()
                            else
                                unlove()
                        }
                }

                iALLOWLIST, iBLOCKLIST -> {
                    val pkgName = intent.getStringExtra(B_PACKAGE_NAME)!!
                    //create copies
                    val aSet = prefs.allowedPackages.toMutableSet()
                    val bSet = prefs.blockedPackages.toMutableSet()

                    if (intent.action == iALLOWLIST)
                        aSet += pkgName
                    else
                        bSet += pkgName
                    bSet.removeAll(aSet) // allowlist takes over blocklist for conflicts
                    prefs.allowedPackages = aSet
                    prefs.blockedPackages = bSet

                    nm.cancel(MainPrefs.CHANNEL_NOTI_NEW_APP, 0)
                }

                iSCROBBLER_ON -> {
                    prefs.scrobblerEnabled = true
                    toast(R.string.scrobbler_on)
                }

                iSCROBBLER_OFF -> {
                    prefs.scrobblerEnabled = false
                    toast(R.string.scrobbler_off)
                }

                ACTION_TIME_CHANGED -> {
                    DigestJob.scheduleAlarms(applicationContext)
                }
            }
        }
    }

    private val nlserviceReciverWithPermission = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                iOTHER_ERR_S -> {
                    val scrobbleError =
                        intent.getSingle<ScrobbleError>()!!
                    notifyOtherError(scrobbleError)
                }

                iBAD_META_S -> {
                    val trackInfo =
                        intent.getSingle<PlayingTrackInfo>()!!
                    val scrobbleError =
                        intent.getSingle<ScrobbleError>()!!

                    notifyBadMeta(trackInfo, scrobbleError)
                }

                iMETA_UPDATE_S -> {
                    val receivedTrackInfo =
                        intent.getSingle<PlayingTrackInfo>()!!
                    val trackInfo =
                        sessListener?.findTrackInfoByHash(receivedTrackInfo.hash) ?: return
                    trackInfo.updateMetaFrom(receivedTrackInfo)
                    notifyScrobble(trackInfo)
                }

                iBLOCK_ACTION_S -> {
                    val hash = intent.getIntExtra(B_HASH, 0)
                    val blockedMetadata = intent.getSingle<BlockedMetadata>()!!
                    val controllers = sessListener?.findControllersByHash(hash)
                    if (!controllers.isNullOrEmpty()) {
                        if (blockedMetadata.skip) {
                            controllers.skip()
                            toast(R.string.skip, 500)
                        } else if (blockedMetadata.mute) {
                            sessListener!!.mute(hash)
                            toast(R.string.mute, 500)
                        }
                    }
                }

                iTHEME_CHANGED_S -> {
                    notiColor = ColorPatchUtils.getNotiColor(applicationContext)
                }
//                iNOW_PLAYING_INFO_REQUEST_S -> {
//                    broadcastNowPlayingInfo()
//                }
            }
        }
    }

    private val pkgInstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_PACKAGE_ADDED) {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                    browserPackages += Stuff.getBrowsersAsStrings(packageManager)
            }
        }
    }

    inner class ScrobbleHandler : Handler {
        private val tracksCopyPQ = PriorityQueue<PlayingTrackInfo>(20) { a, b ->
            (a.scrobbleElapsedRealtime - b.scrobbleElapsedRealtime).toInt()
        }

        constructor() : super()
        constructor(looper: Looper) : super(looper)

        init {
            // start ticker
            sendEmptyMessageDelayed(0, 500)
        }

        // ticker, only handles empty messages and messagePQ
        // required because uptimeMillis pauses / slows down in deep sleep
        override fun handleMessage(m: Message) {
            val queuedMessage = tracksCopyPQ.peek()
            if (queuedMessage != null && queuedMessage.scrobbleElapsedRealtime <= SystemClock.elapsedRealtime()) {
                tracksCopyPQ.remove(queuedMessage)
                submitScrobble(queuedMessage)
            }
            sendEmptyMessageDelayed(0, 500)
        }

        fun has(hash: Int) = tracksCopyPQ.any { it.hash == hash }

        fun reschedule(hash: Int, newElapsedRealtime: Long) {
            tracksCopyPQ.find { it.hash == hash }
                ?.scrobbleElapsedRealtime = newElapsedRealtime
        }

        fun addScrobble(trackInfo: PlayingTrackInfo) =
            trackInfo.copy().also {
                tracksCopyPQ.add(it)
            }

        fun printContents() {
            Stuff.log(tracksCopyPQ.toString())
            val shit = tracksCopyPQ.any { it.hash == 345678 }
            Stuff.log(shit.toString())
        }

        fun nowPlaying(trackInfo: PlayingTrackInfo, fixedDelay: Long? = null) {
            if (trackInfo.title.isEmpty() || has(trackInfo.hash))
                return

            trackInfo.artist = MetadataUtils.sanitizeArtist(trackInfo.artist)
            trackInfo.album = MetadataUtils.sanitizeArtist(trackInfo.album)
            trackInfo.albumArtist = MetadataUtils.sanitizeAlbumArtist(trackInfo.albumArtist)
            trackInfo.userPlayCount = 0
            trackInfo.userLoved = false

            var unparsedScrobbleData: ScrobbleData? = null

            // music only items have an album field,
            // and the correct artist name on official youtube tv app

            if (trackInfo.ignoreOrigArtist) { // not parsed yet
                val (parsedArtist, parsedTitle) = MetadataUtils.parseArtistTitle(trackInfo.origTitle)

                unparsedScrobbleData = trackInfo.toScrobbleData()

                trackInfo.artist = parsedArtist
                trackInfo.title = parsedTitle
                trackInfo.albumArtist = ""
                trackInfo.album = ""
            }

            lastNpTask?.cancel()

            trackInfo.isPlaying = true

            var finalDelay: Long
            if (fixedDelay == null) {
                val delayMillis = prefs.delaySecs.toLong() * 1000
                val delayFraction = prefs.delayPercent / 100.0
                val delayMillisFraction = if (trackInfo.durationMillis > 0)
                    (trackInfo.durationMillis * delayFraction).toLong()
                else
                    Long.MAX_VALUE // deal with negative or 0 delay

                finalDelay = min(delayMillisFraction, delayMillis)

                if (finalDelay - trackInfo.timePlayed > 1000) {
                    finalDelay -= trackInfo.timePlayed
                }
            } else {
                finalDelay = fixedDelay
            }

            finalDelay = finalDelay.coerceAtLeast(10 * 1000) // don't scrobble < 10 seconds

            val submitTime = SystemClock.elapsedRealtime() + finalDelay
            trackInfo.scrobbleElapsedRealtime = submitTime
            val trackInfoCopy = addScrobble(trackInfo)

            lastNpTask = LFMRequester(applicationContext, coroutineScope).apply {
                scrobble(true, trackInfoCopy, unparsedScrobbleData)
            }

            notifyScrobble(trackInfo)
            if (trackInfo.packageName !in prefs.allowedPackages) {
                notifyAppDetected(trackInfo.packageName)
            }

//            broadcastNowPlayingInfo()

            //for rating
            AppRater.incrementScrobbleCount(prefs)
        }

        private fun submitScrobble(trackInfoCopy: PlayingTrackInfo) {
            LFMRequester(applicationContext, coroutineScope)
                .scrobble(false, trackInfoCopy)

            val trackInfo = sessListener?.findTrackInfoByHash(trackInfoCopy.hash) ?: return

            if (trackInfo.userPlayCount > 0)
                trackInfo.userPlayCount++

            notifyScrobble(trackInfo)

            trackInfo.markAsScrobbled()
        }

        fun remove(hash: Int, removeNoti: Boolean = true) {
            Stuff.log("$hash cancelled")
            tracksCopyPQ.removeAll { it.hash == hash }
            sessListener?.findTrackInfoByHash(hash)?.isPlaying = false
            if (removeNoti)
                nm.cancel(MainPrefs.CHANNEL_NOTI_SCROBBLING, 0)
//              broadcastNowPlayingInfo()
        }
    }


    companion object {
        const val iCANCEL = "com.arn.scrobble.CANCEL"
        const val iLOVE = "com.arn.scrobble.LOVE"
        const val iUNLOVE = "com.arn.scrobble.UNLOVE"
        const val iBLOCKLIST = "com.arn.scrobble.BLOCKLIST"
        const val iALLOWLIST = "com.arn.scrobble.ALLOWLIST"
        const val iDIGEST_WEEKLY = "com.arn.scrobble.DIGEST_WEEKLY"
        const val iDIGEST_MONTHLY = "com.arn.scrobble.DIGEST_MONTHLY"
        const val iUPDATE_WIDGET = "com.arn.scrobble.UPDATE_WIDGET"
        const val iSCROBBLER_ON = "com.arn.scrobble.SCROBBLER_ON"
        const val iSCROBBLER_OFF = "com.arn.scrobble.SCROBBLER_OFF"

        const val iMETA_UPDATE_S = "com.arn.scrobble.iMETA_UPDATE"
        const val iOTHER_ERR_S = "com.arn.scrobble.OTHER_ERR"
        const val iBAD_META_S = "com.arn.scrobble.BAD_META"
        const val iTHEME_CHANGED_S = "com.arn.scrobble.THEME_CHANGED"
        const val iNOW_PLAYING_INFO_S = "com.arn.scrobble.NOW_PLAYING_INFO"
        const val iNOW_PLAYING_INFO_REQUEST_S = "com.arn.scrobble.NOW_PLAYING_INFO_REQUEST"
        const val iBLOCK_ACTION_S = "com.arn.scrobble.BLOCK_ACTION"

        const val BROADCAST_PERMISSION = "com.arn.scrobble.MY_AWESOME_PERMISSION"

        const val B_TRACK = "track"
        const val B_ALBUM_ARTIST = "albumartist"
        const val B_TIME = "time"
        const val B_ARTIST = "artist"
        const val B_ALBUM = "album"
        const val B_HASH = "hash"
        const val B_PACKAGE_NAME = "package_name"
        const val B_IGNORED_ARTIST = "ignored_artist"
    }
}