package com.arn.scrobble.crashreporter

import co.touchlab.kermit.Logger
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

object CrashReporter : BaseCrashReporter() {
    override val isAvailable = true

    override fun config(
        keysMap: Map<String, String>,
    ) {
        keysMap.forEach { (key, value) ->
            Firebase.crashlytics.setCustomKey(key, value)
        }

        Firebase.crashlytics.isCrashlyticsCollectionEnabled = true
        Logger.addLogWriter(CrashlyticsLogWriter)
    }
}