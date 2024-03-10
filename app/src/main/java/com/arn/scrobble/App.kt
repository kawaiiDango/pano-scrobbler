package com.arn.scrobble

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.StrictMode
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.size.Precision
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MigratePrefs
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.AppIconFetcher
import com.arn.scrobble.ui.AppIconKeyer
import com.arn.scrobble.ui.DemoInterceptor
import com.arn.scrobble.ui.MusicEntryImageInterceptor
import com.arn.scrobble.ui.StarInterceptor
import com.arn.scrobble.utils.Stuff
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber


class App : Application(), ImageLoaderFactory, Configuration.Provider {
    private var connectivityCheckInited = false
    private val musicEntryImageInterceptor = MusicEntryImageInterceptor()

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (BuildConfig.DEBUG)
                setMinimumLoggingLevel(android.util.Log.INFO)
        }.build()


    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        context = applicationContext
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        // migrate prefs
        MigratePrefs.migrate(prefs)
        Scrobblables.updateScrobblables()

        ColorPatchUtils.setDarkMode(prefs.proStatus)

        val colorsOptions = DynamicColorsOptions.Builder()
            .setThemeOverlay(R.style.AppTheme_Dynamic_Overlay)
            .setPrecondition { _, _ ->
                prefs.themeDynamic && prefs.proStatus
            }
            .build()
        DynamicColors.applyToActivitiesIfAvailable(this, colorsOptions)

        if (prefs.crashlyticsEnabled) {
            FirebaseApp.initializeApp(applicationContext)
            FirebaseCrashlytics.getInstance().setCustomKey("isDebug", BuildConfig.DEBUG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getProcessName() == BuildConfig.APPLICATION_ID ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P
            ) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            }
            Timber.plant(CrashlyticsTree())
        }

        createChannels()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
//                     .detectDiskReads()
//                    .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .detectCustomSlowCalls()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }

    // will be called multiple times
    fun initConnectivityCheck() {
        if (connectivityCheckInited) return

        val cm = ContextCompat.getSystemService(this, ConnectivityManager::class.java)!!
        val nr = NetworkRequest.Builder().apply {
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.build()

        cm.registerNetworkCallback(nr, object : ConnectivityManager.NetworkCallback() {
            private val availableNetworks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                availableNetworks += network
                updateOnlineStatus()
            }

            override fun onLost(network: Network) {
                availableNetworks -= network
                updateOnlineStatus()
            }

            private fun updateOnlineStatus() {
                Stuff.isOnline = availableNetworks.isNotEmpty()
            }
        })

        connectivityCheckInited = true
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .components {
            add(AppIconKeyer())
            add(AppIconFetcher.Factory())
            add(musicEntryImageInterceptor)
            add(StarInterceptor())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }

            if (prefs.demoMode)
                add(DemoInterceptor())
        }
        .crossfade(Stuff.CROSSFADE_DURATION)
        .precision(Precision.INEXACT)
        .allowHardware(false)
        .build()

    fun clearMusicEntryImageCache(entry: MusicEntry) {
        musicEntryImageInterceptor.clearCacheForEntry(entry)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

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

    @SuppressLint("StaticFieldLeak")
    companion object {
        // not a leak
        lateinit var context: Context
            private set
        val prefs by lazy { MainPrefs() }
        val globalExceptionFlow by lazy { MutableSharedFlow<Throwable>() }
    }
}