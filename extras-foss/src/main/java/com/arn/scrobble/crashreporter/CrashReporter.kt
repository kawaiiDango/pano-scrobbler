package com.arn.scrobble.crashreporter

import android.app.Application

object CrashReporter {
    fun init(
        application: Application,
        collectionEnabled: Boolean,
        keysMap: Map<String, String> = emptyMap()
    ) {

    }

    fun setEnabled(enabled: Boolean) {
    }
}