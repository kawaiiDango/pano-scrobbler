package com.arn.scrobble.crashreporter

import android.app.Application
import android.app.Application.getProcessName
import android.os.Build
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize
import timber.log.Timber

object CrashReporter {
    fun init(
        application: Application,
        collectionEnabled: Boolean,
        keysMap: Map<String, String> = emptyMap()
    ) {
        Firebase.initialize(application)
        // otherwise it crashes

        keysMap.forEach { (key, value) ->
            Firebase.crashlytics.setCustomKey(key, value)
        }

        if (collectionEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getProcessName() == application.packageName ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P
            ) {
                Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
            }
            Timber.plant(CrashlyticsTree())
        }
    }

    fun setEnabled(enabled: Boolean) {
        kotlin.runCatching {
            Firebase.crashlytics.apply {
                setCrashlyticsCollectionEnabled(enabled)
                if (!enabled)
                    deleteUnsentReports()
            }
        }
    }
}