package com.arn.scrobble.main

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.StrictMode
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.size.Precision
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.ui.AppIconFetcher
import com.arn.scrobble.ui.AppIconKeyer
import com.arn.scrobble.ui.DemoInterceptor
import com.arn.scrobble.ui.MusicEntryImageInterceptor
import com.arn.scrobble.ui.MusicEntryMapper
import com.arn.scrobble.ui.StarMapper
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber


class App : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    private val musicEntryImageInterceptor = MusicEntryImageInterceptor()

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (BuildConfig.DEBUG)
                setMinimumLoggingLevel(android.util.Log.INFO)
        }.build()


    override fun onCreate() {
        PlatformStuff.application = this

        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
            Stuff.isInDemoMode =
                runBlocking { PlatformStuff.mainPrefs.data.map { it.demoModeP }.first() }
        }

        Timber.plant(LogcatTree())

//        ColorPatchUtils.setDarkMode()

//        val colorsOptions = DynamicColorsOptions.Builder()
//            .setThemeOverlay(R.style.AppTheme_Dynamic_Overlay)
//            .setPrecondition { _, _ ->
//                runBlocking { PlatformStuff.mainPrefs.data.map { it.themeDynamic }.first() } &&
//                        Stuff.billingRepository.isLicenseValid
//            }
//            .build()
//        DynamicColors.applyToActivitiesIfAvailable(this, colorsOptions)

        val crashlyticsKeys = mapOf(
            "isDebug" to BuildConfig.DEBUG.toString(),
        )

        CrashReporter.init(
            this,
            runBlocking {
                PlatformStuff.mainPrefs.data.map { it.crashReporterEnabled }.first()
            },
            crashlyticsKeys
        )

        createChannels()

        initConnectivityCheck()
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

    private fun initConnectivityCheck() {
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
    }

    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(this)
        .components {
            add(AppIconKeyer())
            add(AppIconFetcher.Factory())
            add(MusicEntryMapper())
            add(musicEntryImageInterceptor)
            add(StarMapper())

            if (Stuff.isInDemoMode)
                add(DemoInterceptor())
        }
        .crossfade(true)
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
                Stuff.CHANNEL_NOTI_SCROBBLING,
                getString(R.string.state_scrobbling), NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_SCR_ERR,
                getString(R.string.channel_err), NotificationManager.IMPORTANCE_MIN
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_NEW_APP,
                getString(R.string.new_player, getString(R.string.new_app)),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_PENDING,
                getString(R.string.pending_scrobbles), NotificationManager.IMPORTANCE_MIN
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_DIGEST_WEEKLY,
                getString(R.string.s_top_scrobbles, getString(R.string.weekly)),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_DIGEST_MONTHLY,
                getString(R.string.s_top_scrobbles, getString(R.string.monthly)),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}