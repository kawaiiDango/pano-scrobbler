package com.arn.scrobble.main

import android.app.Application
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
        AndroidStuff.applicationContext = application.applicationContext

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