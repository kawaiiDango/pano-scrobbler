package com.arn.scrobble.crashreporter

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber

object CrashReporter {
    fun config(
        keysMap: Map<String, String> = emptyMap(),
    ) {
        keysMap.forEach { (key, value) ->
            Firebase.crashlytics.setCustomKey(key, value)
        }

        Firebase.crashlytics.isCrashlyticsCollectionEnabled = true
        Timber.plant(CrashlyticsTree())
    }
}