package com.arn.scrobble.crashreporter

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics


object CrashlyticsLogWriter : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean =
        severity >= Severity.Warn

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?
    ) {
        if (message.isNotBlank()) {
            Firebase.crashlytics.log(message)
        }

        throwable?.let {
            Firebase.crashlytics.recordException(it)
        }
    }
}