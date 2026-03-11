package com.arn.scrobble.main

import android.content.Context
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

class MainDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

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
                }

                CompositionLocalProvider(LocalActivityRestoredFlag provides (savedInstanceState != null)) {
                    PanoMainDialogContent(
                        onClose = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyAndroidLocaleLegacy() ?: return)
    }
}