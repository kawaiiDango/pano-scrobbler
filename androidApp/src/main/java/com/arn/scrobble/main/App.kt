package com.arn.scrobble.main

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.work.Configuration
import com.arn.scrobble.ExtrasProps
import com.arn.scrobble.androidApp.BuildConfig
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.review.ReviewPrompter
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import java.io.File


class App : Application(), Configuration.Provider {

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (BuildConfig.DEBUG)
                setMinimumLoggingLevel(Log.INFO)
        }.build()


    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        Initializer.init(this)

        VariantStuff.billingRepository = BillingRepository(
            applicationContext,
            Stuff.billingClientData,
            PlatformStuff::openInBrowser
        )

        val crashReportEnabledFile = File(filesDir, "crash_reporter_disabled.txt")
        val crashReporter = CrashReporter(crashReportEnabledFile)
        VariantStuff.crashReporter = crashReporter
        VariantStuff.reviewPrompter = ReviewPrompter
        VariantStuff.extrasProps = ExtrasProps

        // the built-in content provider initializer only runs in the main process
        val crashlyticsEnabled = AndroidStuff.isMainProcess && crashReporter.isEnabled

        if (crashlyticsEnabled) {
            val crashlyticsKeys = mapOf(
                "isDebug" to BuildConfig.DEBUG.toString(),
            )

            crashReporter.config(crashlyticsKeys)
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
//                     .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .detectCustomSlowCalls()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        detectUnbufferedIo()
                }
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        detectIncorrectContextUse()
                }
                .penaltyLog()
                .build()
        )
    }
}