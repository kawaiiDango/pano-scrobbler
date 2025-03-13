package com.arn.scrobble.main

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Process
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
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.R
import com.arn.scrobble.Tokens
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingClientData
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MigratePrefs
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.AppIconFetcher
import com.arn.scrobble.ui.AppIconKeyer
import com.arn.scrobble.ui.DemoInterceptor
import com.arn.scrobble.ui.MusicEntryImageInterceptor
import com.arn.scrobble.ui.MusicEntryMapper
import com.arn.scrobble.ui.StarMapper
import com.arn.scrobble.utils.Stuff
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber


class App : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    private val musicEntryImageInterceptor = MusicEntryImageInterceptor()

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (BuildConfig.DEBUG)
                setMinimumLoggingLevel(android.util.Log.INFO)
        }.build()


    override fun onCreate() {
        super.onCreate()
        application = this

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        Timber.plant(LogcatTree())

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

        // the built in content provider initializer only runs in the main process
        val crashlyticsEnabled = isMainProcess() && prefs.crashlyticsEnabled

        if (crashlyticsEnabled) {
            val crashlyticsKeys = mapOf(
                "isDebug" to BuildConfig.DEBUG.toString(),
            )

            CrashReporter.config(crashlyticsKeys)
        }

        createChannels()

        initConnectivityCheck()
    }

    fun isMainProcess(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // For API 28+ we can use Application.getProcessName()
            return getProcessName() == packageName
        } else {
            val manager = getSystemService(ActivityManager::class.java)
            val pid = Process.myPid()
            manager?.runningAppProcesses?.forEach { processInfo ->
                if (processInfo.pid == pid) {
                    return processInfo.processName == packageName
                }
            }
        }
        return false
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

            if (prefs.demoMode)
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

    companion object {
        // not a leak
        lateinit var application: Application
            private set
        val prefs by lazy { MainPrefs() }
        val billingRepository: BaseBillingRepository by lazy {
            val billingClientData = BillingClientData(
                proProductId = Stuff.PRO_PRODUCT_ID,
                appName = application.getString(R.string.app_name),
                publicKeyBase64 = if (ExtrasConsts.isFossBuild)
                    Tokens.LICENSE_PUBLIC_KEY_BASE64
                else
                    Tokens.PLAY_BILLING_PUBLIC_KEY_BASE64,
                apkSignature = Tokens.APK_SIGNATURE,
                httpClient = Requesters.genericKtorClient,
                serverUrl = Tokens.LICENSE_CHECKING_SERVER,
                getLastcheckTime = { prefs.lastLicenseCheckTime },
                setLastcheckTime = { prefs.lastLicenseCheckTime = it },
                getReceipt = { prefs.receipt to prefs.receiptSignature },
                setReceipt = { r, s ->
                    prefs.receipt = r
                    prefs.receiptSignature = s
                }
            )
            BillingRepository(application, billingClientData)
        }
        val globalExceptionFlow by lazy { MutableSharedFlow<Throwable>() }
    }
}