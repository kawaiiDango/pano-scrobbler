package com.arn.scrobble.crashreporter

import android.content.Context
import androidx.startup.Initializer
import co.touchlab.kermit.Logger
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import java.io.File

class CrashReporterInitializer : Initializer<CrashReporterConfig> {
    override fun create(context: Context): CrashReporterConfig {
        val f = File(context.filesDir, "crash_reporter_disabled.txt")
        CrashReporterConfig.init(f)

        if (CrashReporterConfig.isEnabled) {
            Firebase.crashlytics.isCrashlyticsCollectionEnabled = true
            Logger.addLogWriter(CrashlyticsLogWriter)
        }

        return CrashReporterConfig
    }

    override fun dependencies(): List<Class<Initializer<*>>> = emptyList()
}
