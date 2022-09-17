package com.arn.scrobble

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MigratePrefs
import com.arn.scrobble.themes.ColorPatchUtils
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

        context = applicationContext

        initCaller()

        Timber.plant(Timber.DebugTree())

        // migrate prefs
        val prefs = MainPrefs(this)
        MigratePrefs.migrate(prefs)

        if (BuildConfig.DEBUG && !prefs.lastfmLinksEnabled) {
            enableOpeningLastfmLinks()
            prefs.lastfmLinksEnabled = true
        }

        ColorPatchUtils.setDarkMode(this, prefs.proStatus)

        val colorsOptions = DynamicColorsOptions.Builder()
            .setThemeOverlay(R.style.AppTheme_Dynamic_Overlay)
            .setPrecondition { _, _ ->
                prefs.themeDynamic && prefs.proStatus
            }
            .build()
        DynamicColors.applyToActivitiesIfAvailable(this, colorsOptions)

        if (prefs.crashlyticsEnabled) {
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

    @SuppressLint("StaticFieldLeak")
    companion object {
        // not a leak
        lateinit var context: Context
    }

}