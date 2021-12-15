package com.arn.scrobble

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.INFO
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) {
            FirebaseCrashlytics.getInstance().log(t.message.toString())
            FirebaseCrashlytics.getInstance().recordException(t)
        } else
            FirebaseCrashlytics.getInstance().log(message)

    }
}