package com.arn.scrobble.crashreporter

import co.touchlab.kermit.Logger
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import java.io.File

internal object CrashReporter : BaseCrashReporter() {
    override fun init(
        disabledFile: File?,
        keysMap: Map<String, String>,
    ) {
        this.disabledFile = disabledFile

        if (disabledFile?.exists() == true) {
            return
        }

        keysMap.forEach { (key, value) ->
            Firebase.crashlytics.setCustomKey(key, value)
        }

        Firebase.crashlytics.isCrashlyticsCollectionEnabled = true
        Logger.addLogWriter(CrashlyticsLogWriter)
    }
}