package com.arn.scrobble.main

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.Stuff

class MainDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                DisposableEffect(navController) {
                    val consumer = Consumer<Intent> {
                        navController.handleDeepLink(it)
                    }
                    addOnNewIntentListener(consumer)
                    onDispose {
                        removeOnNewIntentListener(consumer)
                    }
                }

                PanoMainDialogContent(navController, onFinish = ::finish)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        setLocaleCompat()
    }

    companion object {

        fun createDestinationPendingIntent(
            uri: String,
            mutable: Boolean = false,
        ) =
            PendingIntent.getActivity(
                PlatformStuff.application,
                uri.hashCode(),
                Intent(PlatformStuff.application, MainDialogActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    action = Intent.ACTION_VIEW
                    data = uri.toUri()
                },
                if (mutable) Stuff.updateCurrentOrMutable else Stuff.updateCurrentOrImmutable
            )!!

    }

}