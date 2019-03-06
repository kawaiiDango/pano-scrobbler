package com.arn.scrobble
import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
//        DebugOnly.installLeakCanary(this)
        DebugOnly.strictMode()
    }
}