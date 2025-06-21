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
import com.arn.scrobble.utils.applyAndroidLocaleLegacy
import com.arn.scrobble.utils.getStringInDeviceLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

    private val deviceInteractiveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                coroutineScope.launch {
                    mainPrefs.updateData { it.copy(lastInteractiveTime = System.currentTimeMillis()) }
                }
            }
        }
    }

    private var inited = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyAndroidLocaleLegacy() ?: return)
    }

    override fun onListenerConnected() {
        //    This sometimes gets called twice without calling onListenerDisconnected or onDestroy
        //    onCreate seems to get called only once in those cases.
        //    also unreliable on lp and mm, which i am no longer supporting anyway
        // just gate them with an inited flag


        if (!inited) {
            inited = true

            job = SupervisorJob()
            coroutineScope = CoroutineScope(Dispatchers.Main + job!!)

            if (BuildConfig.DEBUG)
                toast(R.string.scrobbler_on)
            init()
        }
    }

    private fun init() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        ContextCompat.registerReceiver(
            applicationContext,
            deviceInteractiveReceiver,
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
        val persistentNoti = Stuff.mainPrefsInitialValue.notiPersistent
        if (persistentNoti && AndroidStuff.canShowPersistentNotiIfEnabled) {
            try {
                PersistentNotificationService.start()
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
            applicationContext.unregisterReceiver(deviceInteractiveReceiver)
        } catch (e: IllegalArgumentException) {
            Logger.w { "deviceInteractiveReceiver wasn't registered" }
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

        destroy()
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
                (sbn.packageName == Stuff.PACKAGE_SHAZAM &&
                        (sbn.notification.channelId == Stuff.CHANNEL_SHAZAM || sbn.notification.channelId == Stuff.CHANNEL_SHAZAM2) &&
                        sbn.notification.actions != null ||
                        sbn.packageName in Stuff.PACKAGES_PIXEL_NP &&
                        sbn.notification.channelId == Stuff.CHANNEL_PIXEL_NP)

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
            // different song, scrobble it
            trackInfo?.let { scrobbleQueue.remove(it.hash) }
            val newTrackInfo = PlayingTrackInfo(
                appId = pkgName,
                sessionId = TAG_NOTI
            )
            newTrackInfo.putOriginals(artist, title)

            sessListener?.putTrackInfo("$pkgName|$TAG_NOTI", newTrackInfo)
            coroutineScope.launch {
                scrobbleQueue.scrobble(
                    trackInfo = newTrackInfo,
                    appIsAllowListed =
                        mainPrefs.data.map { it.allowedPackages }.first().contains(pkgName),
                    delay = 30 * 1000L
                )
            }
        } else {
            Logger.w("${this::scrobbleFromNoti.name} parse failed")
        }
    }

    companion object {
        const val TAG_NOTI = "noti"
    }
}