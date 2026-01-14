package com.arn.scrobble.main

import android.app.Application
import android.app.Application.getProcessName
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // For API 28+ we can use Application.getProcessName()
                getProcessName() == application.packageName
            } else {
                val currentProcessName = Class
                    .forName("android.app.ActivityThread")
                    .getDeclaredMethod("currentProcessName")
                    .apply { isAccessible = true }
                    .invoke(null) as String

                currentProcessName == application.packageName
            }
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

        val nm = getSystemService(NotificationManager::class.java)!!

        // Create channel groups first
        val groups = mutableListOf<NotificationChannelGroup>()

        groups += NotificationChannelGroup(
            Stuff.GROUP_NOTI_SCROBBLES,
            getString(R.string.scrobbles)
        )
        groups += NotificationChannelGroup(
            Stuff.GROUP_NOTI_DIGESTS,
            getString(R.string.charts)
        )
        groups += NotificationChannelGroup(
            Stuff.GROUP_NOTI_FG_SERVICE,
            getString(R.string.show_persistent_noti)
        )

        nm.createNotificationChannelGroups(groups)

        // Now create channels
        val channels = mutableListOf<NotificationChannel>()

        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_SCROBBLING,
            getString(R.string.state_scrobbling),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_SCROBBLES
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_SCR_ERR,
            getString(R.string.channel_err),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            group = Stuff.GROUP_NOTI_SCROBBLES
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_NEW_APP,
            getString(
                R.string.new_player,
                getString(R.string.new_app)
            ),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_SCROBBLES
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_DIGEST_WEEKLY,
            getString(
                R.string.s_top_scrobbles,
                getString(R.string.weekly)
            ),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_DIGESTS
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_DIGEST_MONTHLY,
            getString(
                R.string.s_top_scrobbles,
                getString(R.string.monthly)
            ),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_DIGESTS
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_FG_SERVICE,
            getString(R.string.show_persistent_noti),
            // foreground service noti cannot be IMPORTANCE_MIN
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_FG_SERVICE
        }

        nm.createNotificationChannels(channels)
        nm.deleteNotificationChannel("noti_persistent")
        nm.deleteNotificationChannel("noti_pending_scrobbles")
    }
}