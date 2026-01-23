package com.arn.scrobble.main

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.arn.scrobble.navigation.LocalActivityRestoredFlag
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.utils.AndroidStuff.prolongSplashScreen
import com.arn.scrobble.utils.applyAndroidLocaleLegacy

class MainDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var initDone = false
        prolongSplashScreen { initDone }

        setContent {
            AppTheme(
                onInitDone = { initDone = true }
            ) {
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