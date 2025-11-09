package com.arn.scrobble.main

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import android.os.StrictMode
import android.util.Log
import androidx.work.Configuration
import com.arn.scrobble.ExtrasProps
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.review.ReviewPrompter
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff


class App : Application(), Configuration.Provider {

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (PlatformStuff.isDebug)
                setMinimumLoggingLevel(Log.INFO)
        }.build()


    override fun onCreate() {
        AndroidStuff.applicationContext = applicationContext
        AndroidStuff.isMainProcess = isMainProcess()

        super.onCreate()

        if (PlatformStuff.isDebug) {
            enableStrictMode()
        }

        Initializer.init(this)

        VariantStuff.billingRepository = BillingRepository(
            applicationContext,
            Stuff.billingClientData,
            PlatformStuff::openInBrowser
        )

        VariantStuff.crashReporter = CrashReporter
        VariantStuff.reviewPrompter = ReviewPrompter
        VariantStuff.extrasProps = ExtrasProps

        // the built in content provider initializer only runs in the main process
        val crashlyticsEnabled = AndroidStuff.isMainProcess &&
                Stuff.mainPrefsInitialValue.crashReporterEnabled

        if (crashlyticsEnabled) {
            val crashlyticsKeys = mapOf(
                "isDebug" to PlatformStuff.isDebug.toString(),
            )

            CrashReporter.config(crashlyticsKeys)
        }
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
}