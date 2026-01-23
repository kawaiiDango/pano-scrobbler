package com.arn.scrobble.main

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowInsetsControllerCompat
import com.arn.scrobble.navigation.LocalActivityRestoredFlag
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.AndroidStuff.prolongSplashScreen
import com.arn.scrobble.utils.applyAndroidLocaleLegacy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var initDone = false
        prolongSplashScreen { initDone }

        setContent {
            AppTheme(
                onInitDone = { initDone = true }
            ) {
                val isDarkTheme = LocalThemeAttributes.current.isDark

                LaunchedEffect(isDarkTheme) {
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }

                    if (Build.VERSION.SDK_INT in 26..27) {
                        // fix always light navigation bar on Oreo
                        val defaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
                        // The dark scrim color used in the platform.
                        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
                        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
                        val defaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                        window.navigationBarColor =
                            if (isDarkTheme)
                                defaultDarkScrim
                            else
                                defaultLightScrim
                    }
                }

                CompositionLocalProvider(LocalActivityRestoredFlag provides (savedInstanceState != null)) {
                    PanoAppContent()
                }
            }
        }

//        val artists = "Siouxsie & The Banshees & The Creatures & John Cale & Lou Reed"
//        val first = runBlocking { FirstArtistExtractor.extract(artists, true) }
//        toast(first)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyAndroidLocaleLegacy() ?: return)
    }
}