package com.arn.scrobble
import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import timber.log.Timber
import java.util.logging.Level

class App : Application() {

    override fun onCreate() {
        DebugOnly.strictMode()
        super.onCreate()
        initCaller()
        if (!BuildConfig.DEBUG) {
            FirebaseApp.initializeApp(applicationContext)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            FirebaseCrashlytics.getInstance().setCustomKey("isDebug", BuildConfig.DEBUG)
            Timber.plant(CrashlyticsTree())
        }
        Timber.plant(Timber.DebugTree())
    }

    @Synchronized
    fun initCaller() {
        val caller = Caller.getInstance()
        caller.userAgent = Stuff.USER_AGENT
        caller.logger.level = Level.WARNING
        caller.cache = FileSystemCache(cacheDir)
        caller.cache.expirationPolicy = LFMCachePolicy(true)
        caller.setErrorNotifier(29) { e ->
            Timber.tag(Stuff.TAG).w(e)
        }
    }
}