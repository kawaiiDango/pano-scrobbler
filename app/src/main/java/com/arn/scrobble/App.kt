package com.arn.scrobble
import android.app.Application
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import java.util.logging.Level

class App : Application() {

    override fun onCreate() {
        DebugOnly.strictMode()
        super.onCreate()
        initCaller()
    }

    @Synchronized
    fun initCaller() {
        val caller = Caller.getInstance()
        caller.userAgent = Stuff.USER_AGENT
        caller.logger.level = Level.WARNING
        caller.cache = FileSystemCache(cacheDir)
        caller.cache.expirationPolicy = LFMCachePolicy(true)

    }
}