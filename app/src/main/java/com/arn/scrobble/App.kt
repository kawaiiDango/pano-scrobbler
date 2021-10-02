package com.arn.scrobble
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.preference.PreferenceManager
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.WidgetPrefs
import com.frybits.harmony.getHarmonySharedPreferences
import com.github.anrwatchdog.ANRWatchDog
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.logging.Level


class App : Application() {

    override fun onCreate() {
        DebugOnly.strictMode()
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            LocaleUtils.systemDefaultLocaleList = LocaleList.getDefault()
        LocaleUtils.systemDefaultLocale = Locale.getDefault()

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
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 5)
                exitReasons.forEachIndexed { index, applicationExitInfo ->
                    Timber.tag("exitReasons").w("${index + 1}. $applicationExitInfo")
                }
            } catch (e: Exception) {}
            // Caused by java.lang.IllegalArgumentException at getHistoricalProcessExitReasons
            // Comparison method violates its general contract!
            // probably a samsung bug
        }

        // migrate prefs
        val mainPrefs = MainPrefs(this)
        if (mainPrefs.prefVersion < 1) {
            val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            migratePrefs(defaultPrefs, mainPrefs.sharedPreferences)

            val activityPrefs = getSharedPreferences("activity_preferences", Context.MODE_PRIVATE)
            migratePrefs(activityPrefs, mainPrefs.sharedPreferences)

            val raterPrefs = getSharedPreferences("apprater", Context.MODE_PRIVATE)
            migratePrefs(raterPrefs, mainPrefs.sharedPreferences)

            val widgetPrefs = getSharedPreferences("widget_preferences", Context.MODE_PRIVATE)
            val newWidgetPrefs = WidgetPrefs(this).sharedPreferences
            migratePrefs(widgetPrefs, newWidgetPrefs)

            val cookiePrefs = getSharedPreferences("CookiePersistence", Context.MODE_PRIVATE)
            val newCookiePrefs = getHarmonySharedPreferences("CookiePersistence")
            migratePrefs(cookiePrefs, newCookiePrefs)

            mainPrefs.prefVersion = 1
        }
    }

    private fun initCaller() {
        Caller.getInstance().apply {
            userAgent = Stuff.USER_AGENT
            logger.level = Level.WARNING
            cache = FileSystemCache(cacheDir)
            cache.expirationPolicy = LFMCachePolicy(true)
            setErrorNotifier(29) { e ->
                Timber.tag(Stuff.TAG).w(e)
            }
        }
    }

    private fun migratePrefs(prefFrom: SharedPreferences, prefTo: SharedPreferences) {
        prefTo.edit().apply {
            prefFrom.all.forEach{ (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                    is Set<*> -> putStringSet(key, value as Set<String>)
                }
            }
            commit()
        }
        // two processes may be doing this at the same time
    }
}