package com.arn.scrobble.crashreporter

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.recordException


object CrashlyticsLogWriter : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean =
        severity >= Severity.Warn

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?
    ) {
        if (message.isNotBlank() && throwable == null) {
            Firebase.crashlytics.log(message)
        } else if (throwable != null) {
            if (message.isBlank())
                Firebase.crashlytics.recordException(throwable)
            else
                Firebase.crashlytics.recordException(throwable) {
                    key("message", message.replace("https?://\\S+".toRegex(), "<url>"))
                }
        }
    }
}