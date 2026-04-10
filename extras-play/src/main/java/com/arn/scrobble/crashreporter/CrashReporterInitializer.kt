package com.arn.scrobble.crashreporter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.startup.Initializer
import co.touchlab.kermit.Logger
import com.google.firebase.Firebase
import com.google.firebase.initialize
import java.io.File

class CrashReporterInitializer : Initializer<CrashReporterConfig> {
    override fun create(context: Context): CrashReporterConfig {
        val f = File(context.filesDir, "crash_reporter_disabled.txt")
        CrashReporterConfig.init(f)

        if (CrashReporterConfig.isEnabled) {
            if (context is Application) {
                // do not initialize heavyweight Firebase for Services, BroadcastReceivers, ContentProviders etc.
                context.registerActivityLifecycleCallbacks(FirebaseInitLifecycleCallbacks())
            }
        }

        return CrashReporterConfig
    }

    override fun dependencies(): List<Class<Initializer<*>>> = emptyList()

    private class FirebaseInitLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        private var initialized = false

        override fun onActivityCreated(activity: Activity, p1: Bundle?) {
            activity.application.unregisterActivityLifecycleCallbacks(this)

            if (initialized) return
            initialized = true

            Firebase.initialize(activity.applicationContext)
            Logger.addLogWriter(CrashlyticsLogWriter)
        }

        override fun onActivityDestroyed(p0: Activity) {
        }

        override fun onActivityPaused(p0: Activity) {
        }

        override fun onActivityResumed(p0: Activity) {
        }

        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        }

        override fun onActivityStarted(p0: Activity) {
        }

        override fun onActivityStopped(p0: Activity) {
        }
    }
}
