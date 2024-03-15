package com.arn.scrobble.main

import android.annotation.SuppressLint
import android.util.Log
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.utils.Stuff
import timber.log.Timber

class LogcatTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        val minLevel = if (BuildConfig.DEBUG)
            Log.DEBUG
        else
            Log.INFO
        return priority >= minLevel
    }

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val tag = tag ?: Stuff.TAG
        when (priority) {
            Log.ASSERT -> Log.wtf(tag, message, t)
            Log.ERROR -> Log.e(tag, message, t)
            Log.WARN -> Log.w(tag, message, t)
            Log.INFO -> Log.i(tag, message, t)
            Log.DEBUG -> Log.d(tag, message, t)
            Log.VERBOSE -> Log.v(tag, message, t)
        }
    }
}