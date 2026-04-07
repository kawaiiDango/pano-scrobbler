package com.arn.scrobble.main

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.StrictMode
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.logger.JavaUtilFileLogger
import com.arn.scrobble.utils.AndroidStuff
import okhttp3.OkHttp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext


class App : Application() {

    @OptIn(ExperimentalResourceApi::class)
    override fun onCreate() {
        super.onCreate()

//        if (BuildConfig.DEBUG) {
//            enableStrictMode()
//        }

        AndroidStuff.applicationContext = applicationContext

        Logger.setTag("scrobbler")
        Logger.setMinSeverity(
            if (BuildKonfig.DEBUG) Severity.Debug else Severity.Info
        )
        Logger.addLogWriter(
            JavaUtilFileLogger(
                isEnabled = false,
                redirectStderr = false,
                printToStd = false
            )
        )

        setResourceReaderAndroidContext(applicationContext)
        OkHttp.initialize(applicationContext)

        printStartInfo()
    }

    private fun printStartInfo() {
        if (BuildKonfig.DEBUG && Build.VERSION.SDK_INT >= 36) {
            val activityManager = getSystemService(ActivityManager::class.java)!!
            activityManager.getHistoricalProcessStartReasons(5)
                .forEachIndexed { index, startInfo ->
                    val str = "startInfo" +
                            " processName: " + startInfo.processName +
                            " startComponent: " + startInfo.startComponent +
                            " startType: " + startInfo.startType +
                            " reason: " + startInfo.reason +
                            " wasForceStopped: " + startInfo.wasForceStopped() +
                            " launchMode: " + startInfo.launchMode +
                            " startupState: " + startInfo.startupState +
                            " intent: " + startInfo.intent +
                            " startupTimestamps: " + startInfo.startupTimestamps
                    Logger.d { "$index. $str" }
                }
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