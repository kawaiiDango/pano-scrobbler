package com.arn.scrobble

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.themes.ColorPatchUtils
import com.frybits.harmony.getHarmonySharedPreferences
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.umass.lastfm.Caller
import timber.log.Timber
import java.io.File
import java.util.logging.Level


class App : Application() {

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        super.onCreate()

        initCaller()

        Timber.plant(Timber.DebugTree())

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

        if (BuildConfig.DEBUG && !mainPrefs.lastfmLinksEnabled) {
            enableOpeningLastfmLinks()
            mainPrefs.lastfmLinksEnabled = true
        }

        ColorPatchUtils.setDarkMode(this, mainPrefs.proStatus)

        val colorsOptions = DynamicColorsOptions.Builder()
            .setThemeOverlay(R.style.AppTheme_Dynamic_Overlay)
            .setPrecondition { _, _ ->
                mainPrefs.themeDynamic && mainPrefs.proStatus
            }
            .build()
        DynamicColors.applyToActivitiesIfAvailable(this, colorsOptions)

        if (mainPrefs.crashlyticsEnabled) {
            FirebaseApp.initializeApp(applicationContext)
            FirebaseCrashlytics.getInstance().setCustomKey("isDebug", BuildConfig.DEBUG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getProcessName() == BuildConfig.APPLICATION_ID ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P
            ) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            } // do manual collection in other (background) processes
            Timber.plant(CrashlyticsTree())
        }
    }

    private fun initCaller() {
        Caller.getInstance().apply {
            logger.level = Level.WARNING
            client = LFMRequester.okHttpClient
            setCache(File(cacheDir, "lastfm-java"), Stuff.LASTFM_JAVA_CACHE_SIZE)
            setErrorNotifier(29) { e ->
                Timber.tag(Stuff.TAG).w(e)
            }
        }
    }

    private fun migratePrefs(prefFrom: SharedPreferences, prefTo: SharedPreferences) {
        prefTo.edit {
            prefFrom.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                    is Set<*> -> putStringSet(key, value as Set<String>)
                }
            }
        }
        // two processes may be doing this at the same time
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
//                     .detectDiskReads()
//                    .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .detectCustomSlowCalls()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }

    // This is broken af. Don't enable in production
    private fun enableOpeningLastfmLinks() {
        val pm = applicationContext.packageManager
        val componentName = ComponentName(packageName, "$packageName.LastfmLinksActivity")
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}