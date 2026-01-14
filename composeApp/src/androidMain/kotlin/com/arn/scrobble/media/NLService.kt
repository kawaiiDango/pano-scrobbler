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
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.ScrobbleData
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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class NLService : NotificationListenerService() {
    private val mainPrefs = PlatformStuff.mainPrefs
    private var sessListener: SessListener? = null
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var scrobbleQueue: ScrobbleQueue
    private var job: Job? = null
    private val audioManager by lazy {
        getSystemService(AudioManager::class.java)!!
    }

    private val deviceInteractiveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                coroutineScope.launch(Dispatchers.IO) {
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

            if (BuildKonfig.DEBUG)
                toast(R.string.scrobbler_on)
            init()
        }
    }

    private fun init() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        ContextCompat.registerReceiver(
            this@NLService.applicationContext,
            deviceInteractiveReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        val sessManager = getSystemService(MediaSessionManager::class.java)!!
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
                PersistentNotificationService.start(this)
//                ForegroundServiceStartNotAllowedException extends IllegalStateException
            } catch (e: IllegalStateException) {
                Logger.e(e) { "Foreground service start not allowed" }
            }
        }

        if (BuildKonfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AndroidStuff.getScrobblerExitReasons().let {
                it.take(5).forEachIndexed { index, applicationExitInfo ->
                    Logger.w("${index + 1}. $applicationExitInfo", tag = "exitReasons")
                }
            }
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
            this@NLService.applicationContext.unregisterReceiver(deviceInteractiveReceiver)
        } catch (e: IllegalArgumentException) {
            Logger.w { "deviceInteractiveReceiver wasn't registered" }
        }

        if (sessListener != null) {
            sessListener?.removeSessions(setOf<MediaSession.Token>())
            getSystemService(MediaSessionManager::class.java)!!
                .removeOnActiveSessionsChangedListener(sessListener!!)
            sessListener = null
            scrobbleQueue.shutdown()
        }
        job?.cancel()
    }

    override fun onListenerDisconnected() { //api 24+ only
        if (BuildKonfig.DEBUG)
            toast(R.string.scrobbler_off)

        destroy()
    }

    private suspend fun shouldScrobbleFromNoti(pkgName: String): Boolean {
        val prefs = mainPrefs.data.first()

        return prefs.scrobblerEnabled && prefs.scrobbleAccounts.isNotEmpty() &&
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
                        sbn.packageName in Stuff.PACKAGES_PIXEL_NP && sbn.notification.channelId == Stuff.CHANNEL_PIXEL_NP ||
                        sbn.packageName == Stuff.PACKAGE_AUDILE && sbn.notification.channelId in Stuff.CHANNELS_AUDILE &&
                        sbn.notification.extras.containsKey(Stuff.AUDILE_METADATA_KEY_TRACK_TITLE) &&
                        sbn.notification.extras.containsKey(Stuff.AUDILE_METADATA_KEY_TRACK_ARTIST)
                        )

    }

    @OptIn(ExperimentalTime::class)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!shouldCheckNoti(sbn))
            return

        coroutineScope.launch {
            if (!shouldScrobbleFromNoti(sbn!!.packageName))
                return@launch

            val n = sbn.notification
            if (sbn.packageName == Stuff.PACKAGE_SHAZAM)
                scrobbleFromNoti {
                    val title = n.extras.getString(Notification.EXTRA_TITLE)
                    val artist = n.extras.getString(Notification.EXTRA_TEXT)
                    if (title != null && artist != null) {
                        ScrobbleData(
                            artist = artist,
                            track = title,
                            album = null,
                            timestamp = System.currentTimeMillis(),
                            albumArtist = null,
                            duration = null,
                            appId = sbn.packageName
                        )
                    } else {
                        null
                    }
                }
            else if (sbn.packageName in Stuff.PACKAGES_PIXEL_NP)
                scrobbleFromNoti {
                    val artistTitlePair = MetadataUtils.scrobbleFromNotiExtractMeta(
                        n.extras.getString(Notification.EXTRA_TITLE) ?: "",
                        getStringInDeviceLocale(R.string.song_format_string)
                    )

                    if (artistTitlePair != null) {
                        val (artist, title) = artistTitlePair
                        ScrobbleData(
                            artist = artist,
                            track = title,
                            album = null,
                            timestamp = System.currentTimeMillis(),
                            albumArtist = null,
                            duration = null,
                            appId = sbn.packageName
                        )
                    } else {
                        null
                    }
                }
            else if (sbn.packageName == Stuff.PACKAGE_AUDILE)
                scrobbleFromNoti {
                    val title = n.extras.getString(Stuff.AUDILE_METADATA_KEY_TRACK_TITLE)
                    val artist = n.extras.getString(Stuff.AUDILE_METADATA_KEY_TRACK_ARTIST)
                    val album = n.extras.getString(Stuff.AUDILE_METADATA_KEY_TRACK_ALBUM)
                    val duration = n.extras.getLong(Stuff.AUDILE_METADATA_KEY_TRACK_DURATION, -1)
                        .takeIf { it > 0 }
                    val timestamp =
                        n.extras.getString(Stuff.AUDILE_METADATA_KEY_TRACK_SAMPLE_TIMESTAMP)
                            ?.let {
                                Instant.parseOrNull(it)?.toEpochMilliseconds()
                            } ?: System.currentTimeMillis()

                    if (!title.isNullOrEmpty() && !artist.isNullOrEmpty()) {
                        ScrobbleData(
                            artist = artist,
                            track = title,
                            album = album,
                            timestamp = timestamp,
                            albumArtist = null,
                            duration = duration,
                            appId = sbn.packageName
                        )
                    } else {
                        null
                    }
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

        if (scrobbleQueue.has(trackInfo.hash))
            trackInfo.addTimePlayed()
        else
            trackInfo.resetTimePlayed()
        trackInfo.paused()

        scrobbleQueue.remove(trackInfo.hash)

        PanoNotifications.removeNotificationByKey("$pkgName|$TAG_NOTI")

    }

    private fun scrobbleFromNoti(transformIntoScrobbleData: () -> ScrobbleData?) {
        val delay = 30 * 1000L
        val cooldown = 5 * 60 * 1000L
        val scrobbleData = transformIntoScrobbleData() ?: return
        val pkgName = scrobbleData.appId ?: return
        val trackInfo = sessListener?.findTrackInfoByKey("$pkgName|$TAG_NOTI")
        val needsDelayAndCooldown = pkgName in Stuff.PACKAGES_PIXEL_NP
        val metadataChanged = !needsDelayAndCooldown || trackInfo == null ||
                (System.currentTimeMillis() - trackInfo.playStartTime > cooldown) ||
                trackInfo.origTitle != scrobbleData.track ||
                trackInfo.origArtist != scrobbleData.artist


        val updatedTrackInfo = trackInfo ?: PlayingTrackInfo(
            appId = pkgName,
            uniqueId = "$pkgName|$TAG_NOTI"
        )

        if (metadataChanged) {
            scrobbleQueue.remove(updatedTrackInfo.hash)

            updatedTrackInfo.putOriginals(
                artist = scrobbleData.artist,
                title = scrobbleData.track,
                album = scrobbleData.album.orEmpty(),
                albumArtist = scrobbleData.albumArtist.orEmpty(),
                durationMillis = scrobbleData.duration ?: 0L,
                trackId = null,
                artUrl = null,
                extraData = emptyMap()
            )

            sessListener?.putTrackInfo("$pkgName|$TAG_NOTI", updatedTrackInfo)
            coroutineScope.launch {
                scrobbleQueue.scrobble(
                    trackInfo = updatedTrackInfo,
                    appIsAllowListed =
                        mainPrefs.data.map { it.allowedPackages.contains(pkgName) }.first(),
                    delay = if (needsDelayAndCooldown) delay else 0L,
                    timestampOverride = if (!needsDelayAndCooldown) scrobbleData.timestamp else null
                )
            }
        }
    }

    companion object {
        const val TAG_NOTI = "noti"
    }
}