package com.arn.scrobble

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.size.Precision
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MigratePrefs
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.AppIconFetcher
import com.arn.scrobble.ui.AppIconKeyer
import com.arn.scrobble.ui.DemoInterceptor
import com.arn.scrobble.ui.MusicEntryImageInterceptor
import com.arn.scrobble.ui.StarInterceptor
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.umass.lastfm.Caller
import timber.log.Timber
import java.io.File
import java.util.logging.Level


class App : Application(), ImageLoaderFactory {
    private var connectivityCheckInited = false

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
        Scrobblables.updateScrobblables()

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

    // will be called multiple times
    fun initConnectivityCheck() {
        if (connectivityCheckInited) return

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nr = NetworkRequest.Builder().apply {
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.build()

        cm.registerNetworkCallback(nr, object : ConnectivityManager.NetworkCallback() {
            private val availableNetworks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                availableNetworks += network
                updateOnlineStatus()
            }

            override fun onLost(network: Network) {
                availableNetworks -= network
                updateOnlineStatus()
            }

            private fun updateOnlineStatus() {
                Stuff.isOnline = availableNetworks.isNotEmpty()
            }
        })

        connectivityCheckInited = true
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .components {
            add(AppIconKeyer())
            add(AppIconFetcher.Factory())
            add(MusicEntryImageInterceptor())
            add(StarInterceptor())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }

            if (Stuff.DEMO_MODE)
                add(DemoInterceptor())
        }
        .crossfade(Stuff.CROSSFADE_DURATION)
        .precision(Precision.INEXACT)
        .build()

    @SuppressLint("StaticFieldLeak")
    companion object {
        // not a leak
        lateinit var context: Context
    }

}