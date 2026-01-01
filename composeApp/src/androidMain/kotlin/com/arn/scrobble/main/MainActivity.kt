package com.arn.scrobble.main

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.arn.scrobble.navigation.LocalActivityRestoredFlag
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.applyAndroidLocaleLegacy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                val isDarkTheme = LocalThemeAttributes.current.isDark
                val view = LocalView.current

                LaunchedEffect(isDarkTheme) {
                    val window = (view.context as Activity).window
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
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