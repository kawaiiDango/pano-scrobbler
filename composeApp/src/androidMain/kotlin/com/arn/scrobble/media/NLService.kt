package com.arn.scrobble.media

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.getStringInDeviceLocale
import com.arn.scrobble.utils.setAndroidLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Objects

class NLService : NotificationListenerService() {
    private val mainPrefs = PlatformStuff.mainPrefs
    private var sessListener: SessListener? = null
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var scrobbleQueue: ScrobbleQueue
    private var job: Job? = null
    private val audioManager by lazy {
        ContextCompat.getSystemService(
            this,
            AudioManager::class.java
        )!!
    }
    private var inited = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.setAndroidLocale() ?: return)
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
//                if (BuildConfig.DEBUG)
                toast(R.string.scrobbler_on)
                init()
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
            addAction(iAPP_ALLOWED_BLOCKED)
            addAction(iSCROBBLER_ON)
            addAction(iSCROBBLER_OFF)
            addAction(iSCROBBLE_SUBMIT_LOCK_S)

            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            applicationContext,
            nlserviceReciver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        val sessManager = ContextCompat.getSystemService(this, MediaSessionManager::class.java)!!
        scrobbleQueue = ScrobbleQueue(coroutineScope)

        sessListener = SessListener(
            coroutineScope,
            scrobbleQueue,
            audioManager
        )

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
            Logger.w(exception) { "Failed to start media controller" }
            // Try to unregister it, just in case.
            try {
                sessManager.removeOnActiveSessionsChangedListener(sessListener!!)
            } catch (e: Exception) {
                Logger.w(e) { "Failed to unregister media controller" }
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
                Logger.e(e) { "Foreground service start not allowed" }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AndroidStuff.getScrobblerExitReasons(printAll = true)
        }

        coroutineScope.launch {
            listenForPlayingTrackEvents(scrobbleQueue, sessListener!!)
        }

        Logger.i { "init" }
    }

    private fun destroy() {
        inited = false

        Logger.i { "destroy" }
        try {
            applicationContext.unregisterReceiver(nlserviceReciver)
        } catch (e: IllegalArgumentException) {
            Logger.w { "nlservicereciver wasn't registered" }
        }

        try {
            applicationContext.unregisterReceiver(nlserviceReciverWithPermission)
        } catch (e: IllegalArgumentException) {
        }
        if (sessListener != null) {
            sessListener?.removeSessions(setOf<MediaSession.Token>())
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
        reason: Int,
    ) { //only for >26
        if (!shouldCheckNoti(sbn) ||
            !(reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL || reason == REASON_TIMEOUT || reason == REASON_ERROR)
        )
            return

        if (sbn?.packageName in Stuff.PACKAGES_PIXEL_NP)
            stopScrobbleFromNoti(sbn!!.packageName)
    }


    private fun stopScrobbleFromNoti(pkgName: String) {
        val trackInfo = sessListener?.findTrackInfoByKey("$pkgName|$TAG_NOTI") ?: return
        scrobbleQueue.remove(trackInfo.hash)
        PanoNotifications.removeNotificationByTag(trackInfo.appId)
    }

    private fun scrobbleFromNoti(
        pkgName: String,
        transformIntoArtistTitle: () -> Pair<String, String>?,
    ) {
        val trackInfo = sessListener?.findTrackInfoByKey("$pkgName|$TAG_NOTI")
        val meta = transformIntoArtistTitle()

        if (meta != null) {
            val (artist, title) = meta
            val hash = Objects.hash(artist, "", title, pkgName)
            if (trackInfo != null && trackInfo.hash == hash) {
                val scrobbleTimeReached =
                    SystemClock.elapsedRealtime() >= trackInfo.scrobbleAtMonotonicTime
                if (!scrobbleTimeReached && !scrobbleQueue.has(hash)) { //"resume" scrobbling
                    scrobbleQueue.addScrobble(trackInfo.copy())

                    PanoNotifications.notifyScrobble(
                        trackInfo,
                        nowPlaying = scrobbleQueue.has(trackInfo.hash)
                    )

                    Logger.i { "${this::scrobbleFromNoti.name} rescheduling" }
                } else if (System.currentTimeMillis() - trackInfo.playStartTime < Stuff.NOTI_SCROBBLE_INTERVAL) {
                    Logger.i("${this::scrobbleFromNoti.name} ignoring possible duplicate")
                }
            } else {
                // different song, scrobble it
                trackInfo?.let { scrobbleQueue.remove(it.hash) }
                val newTrackInfo = PlayingTrackInfo(
                    playStartTime = System.currentTimeMillis(),
                    hash = hash,
                    appId = pkgName,
                    sessionId = TAG_NOTI
                )
                newTrackInfo.putOriginals(artist, title)

                sessListener?.putTrackInfo("$pkgName|$TAG_NOTI", newTrackInfo)
                coroutineScope.launch {
                    scrobbleQueue.nowPlaying(
                        trackInfo = newTrackInfo,
                        appIsAllowListed =
                            mainPrefs.data.map { it.allowedPackages }.first().contains(pkgName),
                        fixedDelay = 30 * 1000L
                    )
                }
            }
        } else {
            Logger.w("${this::scrobbleFromNoti.name} parse failed")
        }
    }

    suspend fun onBroadcastReceived(intent: Intent) {
        when (intent.action) {
            iCANCEL -> {
                val event = intent.getStringExtra(Stuff.EXTRA_EVENT)?.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.TrackCancelled>(it)
                } ?: PlayingTrackNotifyEvent.TrackCancelled(
                    hash = null,
                    showUnscrobbledNotification = false,
                    markAsScrobbled = true
                )

                notifyPlayingTrackEvent(event)
            }

            iLOVE, iUNLOVE -> {
                val event = intent.getStringExtra(Stuff.EXTRA_EVENT)?.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.TrackLovedUnloved>(it)
                } ?: PlayingTrackNotifyEvent.TrackLovedUnloved(
                    hash = null,
                    loved = intent.action == iLOVE
                )

                notifyPlayingTrackEvent(event)
            }

            iAPP_ALLOWED_BLOCKED -> {
                val event = intent.getStringExtra(Stuff.EXTRA_EVENT)?.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.AppAllowedBlocked>(it)
                } ?: return

                notifyPlayingTrackEvent(event)
            }

            iSCROBBLER_ON -> {
                mainPrefs.updateData { it.copy(scrobblerEnabled = true) }
                toast(R.string.scrobbler_on)
            }

            iSCROBBLER_OFF -> {
                mainPrefs.updateData { it.copy(scrobblerEnabled = false) }
                toast(R.string.scrobbler_off)
            }

            Intent.ACTION_SCREEN_ON -> {
                mainPrefs.updateData { it.copy(lastInteractiveTime = System.currentTimeMillis()) }
            }

            iSCROBBLE_SUBMIT_LOCK_S -> {
                val event = intent.getStringExtra(Stuff.EXTRA_EVENT)?.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.TrackScrobbleLocked>(it)
                } ?: return

                notifyPlayingTrackEvent(event)
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

    companion object {
        const val iCANCEL = "com.arn.scrobble.CANCEL"
        const val iLOVE = "com.arn.scrobble.LOVE"
        const val iUNLOVE = "com.arn.scrobble.UNLOVE"
        const val iAPP_ALLOWED_BLOCKED = "com.arn.scrobble.ALLOW_BLOCK_APP"
        const val iSCROBBLER_ON = "com.arn.scrobble.SCROBBLER_ON"
        const val iSCROBBLER_OFF = "com.arn.scrobble.SCROBBLER_OFF"

        const val iSCROBBLE_SUBMIT_LOCK_S = "com.arn.scrobble.SCROBBLE_SUBMIT_LOCK"
        const val BROADCAST_PERMISSION =
            "com.arn.scrobble.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

        const val TAG_NOTI = "noti"
    }
}