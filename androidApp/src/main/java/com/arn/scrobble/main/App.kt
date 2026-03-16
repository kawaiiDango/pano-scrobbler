package com.arn.scrobble.main

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.work.Configuration
import com.arn.scrobble.androidApp.BuildConfig
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.ExtrasVariantStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File


class App : Application(), Configuration.Provider {

    override val workManagerConfiguration =
        Configuration.Builder().apply {
            if (BuildConfig.DEBUG)
                setMinimumLoggingLevel(Log.INFO)
        }.build()


    override fun onCreate() {
        super.onCreate()

//        if (BuildConfig.DEBUG) {
//            enableStrictMode()
//        }

        Initializer.init(this)

        val lastLicenseCheckTimeFile = applicationContext.noBackupFilesDir
            .resolve("last_license_check_time.txt")

        val billingRepository = BillingRepository(
            scope = Stuff.appScope,
            lastCheckTime = flow {
                val t = withContext(Dispatchers.IO) {
                    lastLicenseCheckTimeFile
                        .takeIf { it.exists() }
                        ?.readText()
                        ?.toLongOrNull()
                } ?: -1L

                emit(t)
            },
            setLastcheckTime = { time ->
                withContext(Dispatchers.IO) {
                    lastLicenseCheckTimeFile
                        .writeText(time.toString())
                }
            },
            receipt = flow { emitAll(Stuff.receiptFlow) },
            setReceipt = Stuff::setReceipt,
            httpPost = Stuff::httpPost,
            deviceIdentifier = PlatformStuff::getDeviceIdentifier,
            openInBrowser = PlatformStuff::openInBrowser,
            context = applicationContext
        )

        VariantStuff = ExtrasVariantStuff(billingRepository)

        // the built-in content provider initializer only runs in the main process
        if (AndroidStuff.isMainProcess) {
            val crashlyticsKeys =
                if (BuildConfig.DEBUG)
                    mapOf("isDebug" to true.toString())
                else
                    emptyMap()

            val crashReportDisabledFile = File(filesDir, "crash_reporter_disabled.txt")
            VariantStuff.crashReporter.init(crashReportDisabledFile, crashlyticsKeys)
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