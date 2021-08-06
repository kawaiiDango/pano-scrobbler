package com.arn.scrobble
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import com.github.anrwatchdog.ANRWatchDog
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
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

        ANRWatchDog(4500)
            .setANRListener {
                val sw = StringWriter()
                it.printStackTrace(PrintWriter(sw))
                Timber.tag("anrWatchDog").e(RuntimeException(sw.toString().take(2500)))
            }
            .start()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 5)
            exitReasons.forEachIndexed { index, applicationExitInfo ->
                Timber.tag("exitReasons").w("${index + 1}. $applicationExitInfo")
            }
        }

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