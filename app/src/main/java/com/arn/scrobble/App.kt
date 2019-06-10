package com.arn.scrobble
import android.app.Application

class App : Application() {

    override fun onCreate() {
        DebugOnly.strictMode()
        super.onCreate()
    }
}