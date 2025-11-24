package com.arn.scrobble.main

import android.app.Application
import android.app.Application.getProcessName
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext


object Initializer {

    @OptIn(ExperimentalResourceApi::class)
    fun init(application: Application) {
        fun isMainProcess(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // For API 28+ we can use Application.getProcessName()
                return getProcessName() == application.packageName
            } else {
                val currentProcessName = Class
                    .forName("android.app.ActivityThread")
                    .getDeclaredMethod("currentProcessName")
                    .apply { isAccessible = true }
                    .invoke(null) as String

                return currentProcessName == application.packageName
            }
            return false
        }

        AndroidStuff.applicationContext = application.applicationContext
        AndroidStuff.isMainProcess = isMainProcess()

        runBlocking {
            Stuff.mainPrefsInitialValue = PlatformStuff.mainPrefs.data.first()
        }

        Logger.setTag("scrobbler")
        Logger.setMinSeverity(
            if (BuildKonfig.DEBUG) Severity.Debug else Severity.Info
        )

        setResourceReaderAndroidContext(application.applicationContext)
        application.createChannels()
        OkHttp.initialize(application)
    }

    private fun Context.createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

        // check for existing channels to avoid recreating them on every app start
        // because multiplatform getString() can cause ANRs
        val existingChannels = nm.notificationChannels.map { it.id }.toSet()
        val newChannels = mutableListOf<NotificationChannel>()

        if (Stuff.CHANNEL_NOTI_SCROBBLING !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_SCROBBLING,
                    getString(R.string.state_scrobbling),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_SCR_ERR !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_SCR_ERR,
                    getString(R.string.channel_err),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_NEW_APP !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_NEW_APP,
                    getString(
                        R.string.new_player,
                        Stuff.CHANNEL_NOTI_NEW_APP,
                        getString(R.string.new_app)
                    ),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_PENDING !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_PENDING,
                    getString(R.string.pending_scrobbles),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_DIGEST_WEEKLY !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_DIGEST_WEEKLY,
                    getString(
                        R.string.s_top_scrobbles,
                        getString(R.string.weekly)
                    ),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (Stuff.CHANNEL_NOTI_DIGEST_MONTHLY !in existingChannels) {
            newChannels.add(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_DIGEST_MONTHLY,
                    getString(
                        R.string.s_top_scrobbles,
                        getString(R.string.monthly)
                    ),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        if (newChannels.isNotEmpty()) {
            nm.createNotificationChannels(newChannels)
        }
    }
}