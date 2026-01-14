package com.arn.scrobble.media

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff

class PersistentNotificationService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        stopSelf()
        super.onDestroy()
    }

    private fun showNotification() {
        val nb =
            Notification.Builder(this)
                .setSmallIcon(R.drawable.vd_noti_persistent)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(getColor(R.color.pinkNoti))
                .setGroup(Stuff.GROUP_NOTI_FG_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, Stuff.CHANNEL_NOTI_FG_SERVICE)
            }
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    100,
                    intent,
                    AndroidStuff.updateCurrentOrImmutable
                )
            nb.setChannelId(Stuff.CHANNEL_NOTI_FG_SERVICE)
            nb.setContentIntent(pendingIntent)
            nb.setContentTitle(getString(R.string.persistent_noti_text))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                nb.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
        } else {
            nb.setContentTitle(BuildKonfig.APP_NAME)
            nb.setPriority(Notification.PRIORITY_MIN)
        }
        runCatching {
            startForeground(Stuff.CHANNEL_NOTI_FG_SERVICE.hashCode(), nb.build())
        }.onFailure { e ->
            Logger.e(e) { "Failed to start persistent notification service" }
        }
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PersistentNotificationService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(
                Intent(context, PersistentNotificationService::class.java)
            )
        }
    }
}