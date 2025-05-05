package com.arn.scrobble.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.utils.applyAndroidLocaleLegacy

class MainDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            var currentDialogArgs by remember { mutableStateOf<PanoDialog?>(null) }
            DisposableEffect(Unit) {
                val consumer = Consumer<Intent> {
                    val uri = it.data

                    if (uri == null) {
                        finish()
                        return@Consumer
                    }
                    currentDialogArgs = DeepLinkUtils.parseDeepLink(uri)

                    if (currentDialogArgs == null) {
                        finish()
                        return@Consumer
                    }
                }
                // Handle the intent that started this activity
                intent?.let {
                    if (savedInstanceState == null)
                        consumer.accept(it)
                }
                // Handle new intents
                addOnNewIntentListener(consumer)

                onDispose {
                    removeOnNewIntentListener(consumer)
                }
            }

            AppTheme {
                currentDialogArgs?.let {
                    PanoMainDialogContent(
                        currentDialogArgs = it,
                        onFinish = ::finish
                    )
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        applyAndroidLocaleLegacy()
    }
}