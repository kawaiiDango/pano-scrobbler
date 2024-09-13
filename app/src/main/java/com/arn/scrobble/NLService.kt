package com.arn.scrobble

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.Html
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.navigation.NavDeepLinkBuilder
import com.arn.scrobble.PlayerActions.love
import com.arn.scrobble.PlayerActions.skip
import com.arn.scrobble.PlayerActions.unlove
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.edits.EditDialogFragmentArgs
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.utils.LocaleUtils.getStringInDeviceLocale
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.dLazy
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.getScrobblerExitReasons
import com.arn.scrobble.utils.Stuff.getSingle
import com.arn.scrobble.utils.Stuff.isChannelEnabled
import com.arn.scrobble.utils.Stuff.putData
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Objects
import java.util.PriorityQueue
import kotlin.math.min


class NLService : NotificationListenerService() {
    private val mainPrefs = PlatformStuff.mainPrefs
    private val notificationManager = PlatformStuff.notificationManager
    private var notificationsOnLockscreen = false
    private var sessListener: SessListener? = null
    private val scrobbleQueue by lazy { ScrobbleQueue() }
    private var lastNpTask: Job? = null
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var packageTagTrackMap: MutableMap<String, PlayingTrackInfo> // package name to track info
    private var notiColor: Int? = Color.MAGENTA
    private var job: Job? = null
    private var listenAlongJob: Job? = null
    private val audioManager by lazy {
        ContextCompat.getSystemService(
            this,
            AudioManager::class.java
        )!!
    }
    private var inited = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.setLocaleCompat() ?: return)
    }

    //from https://gist.github.com/xinghui/b2ddd8cffe55c4b62f5d8846d5545bf9
//    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) =
//        Service.START_STICKY


// this prevents the service from starting on N+
//    override fun onBind(intent: Intent): IBinder? {
//        return null
//    }

    override fun onListenerConnected() {
        //    This sometimes gets called twice without calling onListenerDisconnected or onDestroy
        //    onCreate seems to get called only once in those cases.
        //    also unreliable on lp and mm
        // just gate them with an inited flag


        if (!inited) {
            job = SupervisorJob()
            coroutineScope = CoroutineScope(Dispatchers.Main + job!!)

            // API 23 bug, force run them on Main thread
            coroutineScope.launch {
                if (BuildConfig.DEBUG)
                    toast(R.string.scrobbler_on)
                init()
            }

            coroutineScope.launch {
                mainPrefs.data.map { it.notificationsOnLockscreen }.collect {
                    notificationsOnLockscreen = it
                }
            }
        }
    }


    private suspend fun init() {
        // set it to true right away in case onListenerConnected gets called again before init has finished
        inited = true

        val filter = IntentFilter().apply {
            addAction(iCANCEL)
            addAction(iLOVE)
            addAction(iUNLOVE)
            addAction(iALLOWLIST)
            addAction(iBLOCKLIST)
            addAction(iSCROBBLER_ON)
            addAction(iSCROBBLER_OFF)
            addAction(iLISTEN_ALONG)

            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            applicationContext,
            nlserviceReciver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        val pkgFilter = IntentFilter().apply {
            addDataScheme("package")
            addAction(Intent.ACTION_PACKAGE_ADDED)
        }
        ContextCompat.registerReceiver(
            applicationContext,
            pkgInstallReceiver,
            pkgFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        val sFilter = IntentFilter().apply {
            addAction(iTHEME_CHANGED_S)
            addAction(iBAD_META_S)
            addAction(iOTHER_ERR_S)
            addAction(iMETA_UPDATE_S)
            addAction(iBLOCK_ACTION_S)
            addAction(iSCROBBLE_SUBMIT_LOCK_S)
        }

        ContextCompat.registerReceiver(
            applicationContext,
            nlserviceReciverWithPermission,
            sFilter,
            BROADCAST_PERMISSION,
            null,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        notiColor = ColorPatchUtils.getNotiColor(applicationContext)

        val sessManager = ContextCompat.getSystemService(this, MediaSessionManager::class.java)!!

        sessListener = SessListener(
            coroutineScope,
            scrobbleQueue,
            audioManager
        )
        packageTagTrackMap = sessListener!!.packageTagTrackMap
        Stuff.updateBrowserPackages()
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
            Timber.w("Failed to start media controller: " + exception.message)
            // Try to unregister it, just in case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener!!)
            } catch (e: Exception) {
                Timber.tag(Stuff.TAG).w(e)
            }
            // Media controller needs notification listener service permissions.
        }

//      Don't instantiate BillingRepository in this service, it causes unexplained ANRs
        val persistentNoti = mainPrefs.data.map { it.notiPersistent }.first()
        if (persistentNoti && Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.TIRAMISU) {
            try {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, PersistentNotificationService::class.java)
                )
//                ForegroundServiceStartNotAllowedException extends IllegalStateException
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getScrobblerExitReasons(printAll = true)
        }

        Timber.i("init")
    }

    private fun destroy() {
        inited = false

        Timber.i("destroy")
        try {
            applicationContext.unregisterReceiver(nlserviceReciver)
        } catch (e: IllegalArgumentException) {
            Timber.w("nlservicereciver wasn't registered")
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
            ContextCompat.getSystemService(this, MediaSessionManager::class.java)!!
                .removeOnActiveSessionsChangedListener(sessListener!!)
            sessListener = null
            scrobbleQueue.shutdown()
        }
        PanoDb.destroyInstance()
        job?.cancel()
    }

    override fun onListenerDisconnected() { //api 24+ only
        if (BuildConfig.DEBUG)
            toast(R.string.scrobbler_off)

        if (inited && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            destroy()
        }
    }


    override fun onDestroy() {
        if (inited && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // API 23 bug, force run them on Main thread
            coroutineScope.launch {
                destroy()
            }
        }
        super.onDestroy()
    }

    private suspend fun shouldScrobbleFromNoti(pkgName: String): Boolean {
        val prefs = mainPrefs.data.first()

        return prefs.scrobblerEnabled && Stuff.isLoggedIn() &&
                (pkgName in prefs.allowedPackages || (prefs.autoDetectAppsP && pkgName !in prefs.blockedPackages)) &&
                !(prefs.preventDuplicateAmbientScrobbles && sessListener?.isMediaPlaying() == true)
    }

    // don't do file reads here
    private fun shouldCheckNoti(sbn: StatusBarNotification?): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                sbn != null &&
                (sbn.packageName == Stuff.PACKAGE_SHAZAM && sbn.notification.channelId == Stuff.CHANNEL_SHAZAM && sbn.notification.actions != null ||
                        sbn.packageName in Stuff.PACKAGES_PIXEL_NP && sbn.notification.channelId == Stuff.CHANNEL_PIXEL_NP)

    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!shouldCheckNoti(sbn))
            return

        coroutineScope.launch {
            if (!shouldScrobbleFromNoti(sbn!!.packageName))
                return@launch

            if (sbn.packageName == Stuff.PACKAGE_SHAZAM)
                scrobbleFromNoti(sbn.packageName) {
                    val n = sbn.notification
                    val title = n.extras.getString(Notification.EXTRA_TITLE)
                    val artist = n.extras.getString(Notification.EXTRA_TEXT)

                    if (title != null && artist != null)
                        Pair(artist, title)
                    else
                        null
                }
            else if (sbn.packageName in Stuff.PACKAGES_PIXEL_NP)
                scrobbleFromNoti(sbn.packageName) {
                    MetadataUtils.scrobbleFromNotiExtractMeta(
                        sbn.notification.extras.getString(Notification.EXTRA_TITLE) ?: "",
                        getStringInDeviceLocale(R.string.song_format_string)
                    )
                }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) { //only for >26
        if (!shouldCheckNoti(sbn) ||
            !(reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL || reason == REASON_TIMEOUT || reason == REASON_ERROR)
        )
            return

        if (sbn?.packageName in Stuff.PACKAGES_PIXEL_NP)
            stopScrobbleFromNoti(sbn!!.packageName)
    }


    private fun stopScrobbleFromNoti(pkgName: String) {
        val trackInfo = packageTagTrackMap["$pkgName|$TAG_NOTI"]
        scrobbleQueue.remove(trackInfo?.hash ?: return, trackInfo.packageName)
    }

    private fun scrobbleFromNoti(
        pkgName: String,
        transformIntoArtistTitle: () -> Pair<String, String>?,
    ) {
        val trackInfo = packageTagTrackMap["$pkgName|$TAG_NOTI"]
        val meta = transformIntoArtistTitle()

        if (meta != null) {
            val (artist, title) = meta
            val hash = Objects.hash(artist, "", title, pkgName)
            if (trackInfo != null && trackInfo.hash == hash) {
                val scrobbleTimeReached =
                    SystemClock.elapsedRealtime() >= trackInfo.scrobbleElapsedRealtime
                if (!scrobbleTimeReached && !scrobbleQueue.has(hash)) { //"resume" scrobbling
                    scrobbleQueue.addScrobble(trackInfo.copy())
                    notifyScrobble(trackInfo)
                    Timber.i("${this::scrobbleFromNoti.name} rescheduling")
                } else if (System.currentTimeMillis() - trackInfo.playStartTime < Stuff.NOTI_SCROBBLE_INTERVAL) {
                    Timber.i("${this::scrobbleFromNoti.name} ignoring possible duplicate")
                }
            } else {
                // different song, scrobble it
                trackInfo?.let { scrobbleQueue.remove(it.hash) }
                val newTrackInfo = PlayingTrackInfo(
                    playStartTime = System.currentTimeMillis(),
                    hash = hash,
                    packageName = pkgName,
                    sessionTag = TAG_NOTI
                )
                newTrackInfo.putOriginals(artist, title)

                packageTagTrackMap["$pkgName|$TAG_NOTI"] = newTrackInfo
                coroutineScope.launch {
                    scrobbleQueue.nowPlaying(newTrackInfo, 30 * 1000L)
                }
            }
        } else {
            Timber.w("${this::scrobbleFromNoti.name} parse failed")
        }
    }


    private fun buildNotification(): NotificationCompat.Builder {
        val visibility = if (notificationsOnLockscreen)
            NotificationCompat.VISIBILITY_PUBLIC
        else
            NotificationCompat.VISIBILITY_SECRET
        return NotificationCompat.Builder(applicationContext)
            .setShowWhen(false)
            .apply {
                // coloring on android 6 looks very ugly
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    color = (notiColor ?: return@apply)
            }
            .setAutoCancel(true)
            .setCustomBigContentView(null)
            .setVisibility(visibility)
    }

    private fun notifyScrobble(trackInfo: PlayingTrackInfo) {
        if (!notificationManager.isChannelEnabled(Stuff.CHANNEL_NOTI_SCROBBLING))
            return

        val nowPlaying = scrobbleQueue.has(trackInfo.hash)

        var i = Intent()
            .setPackage(packageName)
            .putExtra(B_HASH, trackInfo.hash)
        val loveAction = if (trackInfo.userLoved) {
            i.action = iUNLOVE
            val loveIntent = PendingIntent.getBroadcast(
                applicationContext, 4, i,
                Stuff.updateCurrentOrImmutable
            )
            Stuff.getNotificationAction(
                R.drawable.vd_heart_filled,
                "\uD83E\uDD0D",
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

        val infoArgs = trackInfo.toTrack().let { Bundle().putData(it) }
        val launchPi =
            MainDialogActivity.createDestinationPendingIntent(R.id.infoFragment, infoArgs)

        i = Intent(iCANCEL)
            .setPackage(packageName)
            .putExtra(B_HASH, trackInfo.hash)
            .putSingle(
                ScrobbleError(
                    getString(R.string.state_unscrobbled),
                    null,
                    trackInfo.packageName,
                )
            )

        val cancelToastIntent = PendingIntent.getBroadcast(
            applicationContext, 5, i,
            Stuff.updateCurrentOrImmutable
        )

        val editArgs = EditDialogFragmentArgs(
            data = trackInfo.toScrobbleData(),
            hash = trackInfo.hash
        )
            .toBundle()

        val editPi =
            MainDialogActivity.createDestinationPendingIntent(R.id.editDialogFragment, editArgs)

        val editAction = Stuff.getNotificationAction(
            R.drawable.vd_edit,
            "✏️",
            getString(R.string.edit),
            editPi
        )

        val unscrobbleAction = Stuff.getNotificationAction(
            R.drawable.vd_remove,
            "⛔️",
            getString(R.string.unscrobble),
            cancelToastIntent
        )

        val state =
            if (nowPlaying)
                ""
//                    "▷ "
            else
                "✓ "

        val style = MediaStyle()
        val nb = buildNotification()
            .setAutoCancel(false)
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti)
            .setContentIntent(launchPi)
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
                        "~" + trackInfo.userPlayCount.format()
                    )
                )
        else
            nb.setContentTitle(state + trackInfo.title)
                .setContentText(trackInfo.artist)

        if (nowPlaying) {
            nb.addAction(editAction)
            nb.addAction(unscrobbleAction)
            if (resources.getBoolean(R.bool.is_rtl))
                style.setShowActionsInCompactView(2, 1, 0)
            else
                style.setShowActionsInCompactView(0, 1, 2)
        } else {
            style.setShowActionsInCompactView(0)
        }

        try {
            notificationManager.notify(trackInfo.packageName, 0, nb.buildMediaStyleMod())
        } catch (e: RuntimeException) {
            val nExpandable = nb.setLargeIcon(null as Bitmap?)
                .setStyle(null)
                .build()
            notificationManager.notify(trackInfo.packageName, 0, nExpandable)
        }
    }

    private fun notifyBadMeta(trackInfo: PlayingTrackInfo, scrobbleError: ScrobbleError) {
        val editArgs = EditDialogFragmentArgs(
            data = trackInfo.toScrobbleData(),
            hash = trackInfo.hash
        )
            .toBundle()

        val editPi =
            MainDialogActivity.createDestinationPendingIntent(R.id.editDialogFragment, editArgs)

        val subtitleSpanned = if (scrobbleError.description != null)
            Html.fromHtml(scrobbleError.description)
        else
            getString(R.string.artist_title, trackInfo.artist, trackInfo.title)

        val nb = buildNotification()
            .setAutoCancel(false)
            .setChannelId(Stuff.CHANNEL_NOTI_SCR_ERR)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentIntent(editPi)
            .setContentText(subtitleSpanned)
            .setContentTitle("${trackInfo.title} " + getString(R.string.tap_to_edit))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyleCompat(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(scrobbleError.title)
                    .bigText(subtitleSpanned)
            )
        scrobbleQueue.remove(trackInfo.hash)

        notificationManager.notify(trackInfo.packageName, 0, nb.build())
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
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentIntent(launchIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentText(spanned) //required on recent oneplus devices

        val isMinimised = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(Stuff.CHANNEL_NOTI_SCROBBLING).importance < NotificationManager.IMPORTANCE_LOW
        if (isMinimised)
            nb.setContentTitle(scrobbleError.description?.replace("</?br?>".toRegex(), ""))
        else
            nb.setContentTitle(scrobbleError.title)

        nb.setStyleCompat(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(scrobbleError.title)
                .bigText(spanned)
        )

        notificationManager.notify(scrobbleError.packageName, 0, nb.build())
    }

    private fun notifyUnscrobbled(hash: Int) {
        val delayTime = 4000L
        val trackInfo = sessListener?.findTrackInfoByHash(hash) ?: return

        val blockedMetadata = BlockedMetadata(
            track = trackInfo.title,
            album = trackInfo.album,
            artist = trackInfo.artist,
            albumArtist = trackInfo.albumArtist,
            skip = true,
        )

        val args = Bundle().apply {
            putSingle(blockedMetadata)
            putString(B_IGNORED_ARTIST, trackInfo.origArtist)
            putInt(B_HASH, hash)
        }

        val blockPi = MainDialogActivity.createDestinationPendingIntent(
            R.id.blockedMetadataAddDialogFragment,
            args
        )

        val nb = buildNotification()
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(getString(R.string.state_unscrobbled) + " • " + getString(R.string.blocked_metadata_noti))
            .setContentIntent(blockPi)
            .setTimeoutAfter(delayTime)
        notificationManager.notify(trackInfo.packageName, 0, nb.build())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            coroutineScope.launch {
                delay(delayTime)
                notificationManager.cancel(trackInfo.packageName, 0)
            }
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
            .setPackage(packageName)
            .putExtra(B_PACKAGE_NAME, pkgName)
        val ignoreIntent = PendingIntent.getBroadcast(
            applicationContext, 1, intent,
            Stuff.updateCurrentOrImmutable
        )
        intent = Intent(iALLOWLIST)
            .setPackage(packageName)
            .putExtra(B_PACKAGE_NAME, pkgName)
        val okayIntent = PendingIntent.getBroadcast(
            applicationContext, 2, intent,
            Stuff.updateCurrentOrImmutable
        )

        val launchIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setComponentName(MainActivity::class.java)
            .setDestination(R.id.appListFragment)
            .createPendingIntent()

        val n = buildNotification()
            .setContentTitle(getString(R.string.new_player, appName))
            .setContentText(getString(R.string.new_player_prompt))
            .setChannelId(Stuff.CHANNEL_NOTI_NEW_APP)
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
                    MediaStyle().setShowActionsInCompactView(1, 0)
                else
                    MediaStyle().setShowActionsInCompactView(0, 1)
            )
            .buildMediaStyleMod()
        notificationManager.notify(Stuff.CHANNEL_NOTI_NEW_APP, 0, n)
    }

    private var notiIconBitmap: Bitmap? = null

    private fun NotificationCompat.Builder.setStyleCompat(style: NotificationCompat.Style): NotificationCompat.Builder {
        if (!(Stuff.isWindows11 && style is MediaStyle))
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
                    AppCompatResources.getDrawable(applicationContext, R.mipmap.ic_launcher)
                        ?.toBitmap()
            }
//                icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
            setLargeIcon(notiIconBitmap)
        }
        return build()
    }

    suspend fun onBroadcastReceived(intent: Intent) {
        when (intent.action) {
            iCANCEL -> {
                val hash: Int
                if (!intent.hasExtra(B_HASH)) {
                    val trackInfo = packageTagTrackMap.values.find { it.isPlaying } ?: return
                    hash = trackInfo.hash
                    if (!scrobbleQueue.has(hash))
                        return
                    notificationManager.cancel(trackInfo.packageName, 0)
                    trackInfo.markAsScrobbled()
                } else {
                    hash = intent.getIntExtra(B_HASH, 0)
                    val packageName = intent.getStringExtra(B_PACKAGE_NAME)
                    val trackInfo = sessListener?.findTrackInfoByHash(hash)
                    if (scrobbleQueue.has(hash)) {
                        trackInfo?.markAsScrobbled()
                    } else if (trackInfo != null) {
                        if (!Stuff.isOnline) {
                            toast(R.string.unavailable_offline)
                            return
                        }
                        val track = Track(
                            trackInfo.title,
                            null,
                            Artist(trackInfo.artist),
                            date = trackInfo.playStartTime
                        )
                        coroutineScope.launch(Dispatchers.IO) {
                            ScrobbleEverywhere.delete(track)
                        }
                    }
                    notifyUnscrobbled(hash)
                }

                scrobbleQueue.remove(hash)
            }

            iLOVE, iUNLOVE -> {
                val loved = intent.action == iLOVE
                val hash = intent.getIntExtra(B_HASH, 0)
                val trackInfo = if (!intent.hasExtra(B_HASH)) {
                    packageTagTrackMap.values.find { it.isPlaying } ?: return
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

                coroutineScope.launch(Dispatchers.IO) {
                    ScrobbleEverywhere.loveOrUnlove(trackInfo.toTrack(), loved)
                }

                trackInfo.userLoved = loved
                notifyScrobble(trackInfo)

                val linkHeartButtonToRating =
                    mainPrefs.data.map { it.linkHeartButtonToRating }.first()

                if (linkHeartButtonToRating && Stuff.billingRepository.isLicenseValid)
                    sessListener?.findControllersByPackage(trackInfo.packageName)?.apply {
                        if (loved)
                            love()
                        else
                            unlove()
                    }
            }

            iALLOWLIST, iBLOCKLIST -> {
                val pkgName = intent.getStringExtra(B_PACKAGE_NAME) ?: return
                //create copies
                val aSet = mainPrefs.data.map { it.allowedPackages }.first().toMutableSet()
                val bSet = mainPrefs.data.map { it.blockedPackages }.first().toMutableSet()

                if (intent.action == iALLOWLIST)
                    aSet += pkgName
                else
                    bSet += pkgName
                bSet.removeAll(aSet) // allowlist takes over blocklist for conflicts

                mainPrefs.updateData { it.copy(allowedPackages = aSet, blockedPackages = bSet) }

                notificationManager.cancel(Stuff.CHANNEL_NOTI_NEW_APP, 0)
            }

            iSCROBBLER_ON -> {
                mainPrefs.updateData { it.copy(scrobblerEnabled = true) }
                toast(R.string.scrobbler_on)
            }

            iSCROBBLER_OFF -> {
                mainPrefs.updateData { it.copy(scrobblerEnabled = false) }
                toast(R.string.scrobbler_off)
            }

            iLISTEN_ALONG -> {
                if (!BuildConfig.DEBUG) return

                val username = intent.getStringExtra(ListenAlong.USERNAME_EXTRA)
                val stop = intent.getBooleanExtra(ListenAlong.STOP_EXTRA, false)

                if (username != null) {
                    listenAlongJob?.cancel()
                    listenAlongJob = coroutineScope.launch(Dispatchers.IO) {
                        ListenAlong.fetchTrackLoop(username)
                    }
                } else if (stop) {
                    listenAlongJob?.cancel()
                    notificationManager.cancel(ListenAlong.NOTIFICATION_ID)

                }
            }

            Intent.ACTION_SCREEN_ON -> {
                mainPrefs.updateData { it.copy(lastInteractiveTime = System.currentTimeMillis()) }
            }

            iOTHER_ERR_S -> {
                val scrobbleError = intent.getSingle<ScrobbleError>() ?: return
                notifyOtherError(scrobbleError)
            }

            iBAD_META_S -> {
                val trackInfo = intent.getSingle<PlayingTrackInfo>() ?: return
                val scrobbleError = intent.getSingle<ScrobbleError>() ?: return

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

            iSCROBBLE_SUBMIT_LOCK_S -> {
                val locked = intent.getBooleanExtra(B_LOCKED, false)
                val hash = intent.getIntExtra(B_HASH, -1)
                if (hash == -1) return

                if (locked) {
                    scrobbleQueue.lockedHash = hash
                } else {
                    scrobbleQueue.lockedHash = null
                }
            }
        }
    }

    private val nlserviceReciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            coroutineScope.launch {
                onBroadcastReceived(intent)
            }
        }
    }

    private val nlserviceReciverWithPermission = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            coroutineScope.launch {
                onBroadcastReceived(intent)
            }
        }
    }

    private val pkgInstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                    Stuff.updateBrowserPackages()
            }
        }
    }

    inner class ScrobbleQueue {

        // delays scrobbling this hash until it becomes null again
        var lockedHash: Int? = null

        private val tickEveryMs = 500L

        private val tracksCopyPQ = PriorityQueue<PlayingTrackInfo>(20) { a, b ->
            (a.scrobbleElapsedRealtime - b.scrobbleElapsedRealtime).toInt()
        }

        private var tickerJob: Job? = null

        // ticker, only handles empty messages and messagePQ
        // required because uptimeMillis pauses / slows down in deep sleep

        private fun startTickerIfNeeded() {
            if (tickerJob?.isActive == true) return

            tickerJob = coroutineScope.launch {
                while (tracksCopyPQ.isNotEmpty()) {
                    delay(tickEveryMs)
                    val queuedMessage = tracksCopyPQ.peek()
                    if (queuedMessage != null && queuedMessage.hash != lockedHash &&
                        queuedMessage.scrobbleElapsedRealtime <= SystemClock.elapsedRealtime()
                    ) {
                        tracksCopyPQ.remove(queuedMessage)
                        submitScrobble(queuedMessage)
                    }
                }
            }
        }

        fun shutdown() {
            tickerJob?.cancel()
            tracksCopyPQ.clear()
        }

        fun has(hash: Int) = tracksCopyPQ.any { it.hash == hash }

        fun reschedule(hash: Int, newElapsedRealtime: Long) {
            tracksCopyPQ.find { it.hash == hash }
                ?.scrobbleElapsedRealtime = newElapsedRealtime
        }

        fun addScrobble(trackInfo: PlayingTrackInfo) =
            trackInfo.copy().also {
                tracksCopyPQ.add(it)
                startTickerIfNeeded()
            }

        suspend fun nowPlaying(trackInfo: PlayingTrackInfo, fixedDelay: Long? = null) {
            if (trackInfo.title.isEmpty() || has(trackInfo.hash))
                return

            val prefs = mainPrefs.data.first()

            trackInfo.artist = MetadataUtils.sanitizeArtist(trackInfo.artist)
            trackInfo.album = MetadataUtils.sanitizeAlbum(trackInfo.album)
            trackInfo.albumArtist = MetadataUtils.sanitizeAlbumArtist(trackInfo.albumArtist)
            trackInfo.userPlayCount = 0
            trackInfo.userLoved = false

            trackInfo.isPlaying = true

            var finalDelay: Long
            if (fixedDelay == null) {
                val delayMillis = prefs.delaySecsP.toLong() * 1000
                val delayFraction = prefs.delayPercentP / 100.0
                val delayMillisFraction = if (trackInfo.durationMillis > 0)
                    (trackInfo.durationMillis * delayFraction).toLong()
                else
                    Long.MAX_VALUE

                finalDelay = min(delayMillisFraction, delayMillis)
                    .coerceAtLeast(10 * 1000) // don't scrobble < 10 seconds

                finalDelay = (finalDelay - trackInfo.timePlayed)
                    .coerceAtLeast(1000)// deal with negative or 0 delay
            } else {
                finalDelay = fixedDelay
            }

            val submitTime = SystemClock.elapsedRealtime() + finalDelay
            trackInfo.scrobbleElapsedRealtime = submitTime
            val trackInfoCopy = addScrobble(trackInfo)

            lastNpTask?.cancel()
            lastNpTask = coroutineScope.launch(Dispatchers.IO) {
                ScrobbleEverywhere.scrobble(true, trackInfoCopy)
            }

            notifyScrobble(trackInfo)
            if (trackInfo.packageName !in prefs.allowedPackages) {
                notifyAppDetected(trackInfo.packageName)
            }
        }

        private fun submitScrobble(trackInfoCopy: PlayingTrackInfo) {
            // if it somehow reached here, don't scrobble
            if (sessListener?.hasOtherPlayingControllers(trackInfoCopy) == true && trackInfoCopy.hasBlockedTag)
                return

            coroutineScope.launch(Dispatchers.IO) {
                ScrobbleEverywhere.scrobble(false, trackInfoCopy)
            }
            val trackInfo = sessListener?.findTrackInfoByHash(trackInfoCopy.hash) ?: return

            if (trackInfo.userPlayCount > 0)
                trackInfo.userPlayCount++

            notifyScrobble(trackInfo)

            trackInfo.markAsScrobbled()
        }

        fun remove(hash: Int, notificationPackageNameToRemove: String? = null) {
            if (hash == lockedHash) return

            Timber.dLazy { "$hash from $notificationPackageNameToRemove cancelled" }
            tracksCopyPQ.removeAll { it.hash == hash }
            sessListener?.findTrackInfoByHash(hash)?.isPlaying = false
            if (notificationPackageNameToRemove != null)
                notificationManager.cancel(notificationPackageNameToRemove, 0)
        }
    }


    companion object {
        const val iCANCEL = "com.arn.scrobble.CANCEL"
        const val iLOVE = "com.arn.scrobble.LOVE"
        const val iUNLOVE = "com.arn.scrobble.UNLOVE"
        const val iBLOCKLIST = "com.arn.scrobble.BLOCKLIST"
        const val iALLOWLIST = "com.arn.scrobble.ALLOWLIST"
        const val iSCROBBLER_ON = "com.arn.scrobble.SCROBBLER_ON"
        const val iSCROBBLER_OFF = "com.arn.scrobble.SCROBBLER_OFF"

        const val iMETA_UPDATE_S = "com.arn.scrobble.iMETA_UPDATE"
        const val iOTHER_ERR_S = "com.arn.scrobble.OTHER_ERR"
        const val iBAD_META_S = "com.arn.scrobble.BAD_META"
        const val iTHEME_CHANGED_S = "com.arn.scrobble.THEME_CHANGED"
        const val iBLOCK_ACTION_S = "com.arn.scrobble.BLOCK_ACTION"
        const val iSCROBBLE_SUBMIT_LOCK_S = "com.arn.scrobble.SCROBBLE_SUBMIT_LOCK"
        const val iLISTEN_ALONG = "com.arn.scrobble.LISTEN_ALONG"
        const val BROADCAST_PERMISSION = "com.arn.scrobble.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

        const val B_TRACK = "track"
        const val B_ALBUM_ARTIST = "albumartist"
        const val B_ARTIST = "artist"
        const val B_DURATION = "duration"
        const val B_ALBUM = "album"
        const val B_HASH = "hash"
        const val B_PACKAGE_NAME = "package_name"
        const val B_IGNORED_ARTIST = "ignored_artist"
        const val B_LOCKED = "locked"

        const val TAG_NOTI = "noti"
    }
}