package com.arn.scrobble.main

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) {
            Firebase.crashlytics.log(t.message.toString())
            Firebase.crashlytics.recordException(t)
        } else
            Firebase.crashlytics.log(message)

    }
}