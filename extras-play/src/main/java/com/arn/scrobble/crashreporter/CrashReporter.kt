package com.arn.scrobble.crashreporter

import android.app.Application
import android.app.Application.getProcessName
import android.os.Build
import co.touchlab.kermit.Logger
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize

object CrashReporter {
    fun init(
        application: Any?,
        collectionEnabled: Boolean,
        keysMap: Map<String, String> = emptyMap(),
    ) {
        application as Application
        Firebase.initialize(application)

        keysMap.forEach { (key, value) ->
            Firebase.crashlytics.setCustomKey(key, value)
        }

        if (collectionEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getProcessName() == application.packageName ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P
            ) {
                Firebase.crashlytics.isCrashlyticsCollectionEnabled = true
            }
            Logger.addLogWriter(CrashlyticsLogWriter)
        }
    }

    fun setEnabled(enabled: Boolean) {
        kotlin.runCatching {
            Firebase.crashlytics.apply {
                isCrashlyticsCollectionEnabled = enabled
                if (!enabled)
                    deleteUnsentReports()
            }
        }
    }
}