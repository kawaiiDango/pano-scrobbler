package com.arn.scrobble.main

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Process
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


class App : Application(), Configuration.Provider {

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (BuildConfig.DEBUG)
                setMinimumLoggingLevel(android.util.Log.INFO)
        }.build()


    override fun onCreate() {
        AndroidStuff.application = this

        super.onCreate()

        runBlocking {
            Stuff.mainPrefsInitialValue = PlatformStuff.mainPrefs.data.first()
        }

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        Logger.setTag("scrobbler")
        Logger.setMinSeverity(
            if (PlatformStuff.isDebug) Severity.Debug else Severity.Info
        )

        // the built in content provider initializer only runs in the main process
        val crashlyticsEnabled = isMainProcess() && Stuff.mainPrefsInitialValue.crashReporterEnabled

        if (crashlyticsEnabled) {
            val crashlyticsKeys = mapOf(
                "isDebug" to BuildConfig.DEBUG.toString(),
            )

            CrashReporter.config(crashlyticsKeys)
        }
        setComposeResourcesAndroidContext(this)
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

    private fun setComposeResourcesAndroidContext(context: Context) {
        val clazz =
            Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
        val field = clazz.getDeclaredField("ANDROID_CONTEXT")
        field.isAccessible = true

        // only set value if it is null
        if (field.get(null) == null) {
            field.set(null, context)
        }
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

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_SCROBBLING,
                getString(R.string.state_scrobbling),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_SCR_ERR,
                getString(R.string.channel_err),
                NotificationManager.IMPORTANCE_MIN
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_NEW_APP,
                getString(
                    R.string.new_player,
                    getString(
                        R.string.new_app
                    )
                ),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_PENDING,
                getString(R.string.pending_scrobbles),
                NotificationManager.IMPORTANCE_MIN
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_DIGEST_WEEKLY,
                getString(
                    R.string.s_top_scrobbles,
                    getString(R.string.weekly)
                ),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Stuff.CHANNEL_NOTI_DIGEST_MONTHLY,
                getString(
                    R.string.s_top_scrobbles,
                    getString(R.string.monthly)
                ),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}