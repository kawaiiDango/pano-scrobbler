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
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext


class App : Application(), Configuration.Provider {

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (BuildConfig.DEBUG)
                setMinimumLoggingLevel(android.util.Log.INFO)
        }.build()


    @OptIn(ExperimentalResourceApi::class)
    override fun onCreate() {
        AndroidStuff.applicationContext = applicationContext
        AndroidStuff.isMainProcess = isMainProcess()

        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        runBlocking {
            Stuff.mainPrefsInitialValue = PlatformStuff.mainPrefs.data.first()
        }

        Logger.setTag("scrobbler")
        Logger.setMinSeverity(
            if (PlatformStuff.isDebug) Severity.Debug else Severity.Info
        )

        // the built in content provider initializer only runs in the main process
        val crashlyticsEnabled = AndroidStuff.isMainProcess &&
                Stuff.mainPrefsInitialValue.crashReporterEnabled

        if (crashlyticsEnabled) {
            val crashlyticsKeys = mapOf(
                "isDebug" to BuildConfig.DEBUG.toString(),
            )

            CrashReporter.config(crashlyticsKeys)
        }
        setResourceReaderAndroidContext(this)
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

        // check for existing channels to avoid recreating them on every app start
        // because multiplatform getString() can cause ANRs
        val existingChannels = nm.notificationChannels.map { it.id }.toSet()
        val newChannels = mutableListOf<NotificationChannel>()

        if (Stuff.CHANNEL_NOTI_SCROBBLING !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_SCROBBLING,
                    getString(R.string.state_scrobbling),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_SCR_ERR !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_SCR_ERR,
                    getString(R.string.channel_err),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_NEW_APP !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_NEW_APP,
                    getString(
                        R.string.new_player,
                        Stuff.CHANNEL_NOTI_NEW_APP,
                        getString(R.string.new_app)
                    ),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_PENDING !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_PENDING,
                    getString(R.string.pending_scrobbles),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_DIGEST_WEEKLY !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_DIGEST_WEEKLY,
                    getString(
                        R.string.s_top_scrobbles,
                        getString(R.string.weekly)
                    ),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_DIGEST_MONTHLY !in existingChannels) {
            newChannels.add(
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

        if (newChannels.isNotEmpty()) {
            nm.createNotificationChannels(newChannels)
        }
    }
}