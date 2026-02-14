package com.arn.scrobble.main

import android.app.Application
import android.app.Application.getProcessName
import android.os.Build
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.logger.JavaUtilFileLogger
import com.arn.scrobble.utils.AndroidStuff
import okhttp3.OkHttp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext


object Initializer {

    @OptIn(ExperimentalResourceApi::class)
    fun init(application: Application) {
        fun isMainProcess(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // For API 28+ we can use Application.getProcessName()
                getProcessName() == application.packageName
            } else {
                val currentProcessName = Class
                    .forName("android.app.ActivityThread")
                    .getDeclaredMethod("currentProcessName")
                    .apply { isAccessible = true }
                    .invoke(null) as String

                currentProcessName == application.packageName
            }
        }

        AndroidStuff.applicationContext = application.applicationContext
        AndroidStuff.isMainProcess = isMainProcess()

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

        setResourceReaderAndroidContext(application.applicationContext)
        OkHttp.initialize(application)
    }
}