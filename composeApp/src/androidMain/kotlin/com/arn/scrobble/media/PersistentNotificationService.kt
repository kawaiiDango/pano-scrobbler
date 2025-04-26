package com.arn.scrobble.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_PERSISTENT,
                    getString(R.string.show_persistent_noti),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        val nb =
            NotificationCompat.Builder(applicationContext, Stuff.CHANNEL_NOTI_PERSISTENT)
                .setSmallIcon(R.drawable.vd_noti_persistent)
                .setPriority(Notification.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, Stuff.CHANNEL_NOTI_PERSISTENT)
            }
            val pendingIntent =
                PendingIntent.getActivity(this, 100, intent, PendingIntent.FLAG_IMMUTABLE)
            nb.setContentIntent(pendingIntent)
            nb.setContentTitle(getString(R.string.persistent_noti_text))
        } else {
            nb.setContentTitle(BuildKonfig.APP_NAME)
        }
        runCatching {
            startForeground(ID, nb.build())
        }.onFailure {
            it.printStackTrace()
        }
    }

    companion object {
        private const val ID = 30

        fun start() {
            ContextCompat.startForegroundService(
                AndroidStuff.application,
                Intent(AndroidStuff.application, PersistentNotificationService::class.java)
            )
        }

        fun stop() {
            // ignore the warning, it still works
            AndroidStuff.application.stopService(
                Intent(AndroidStuff.application, PersistentNotificationService::class.java)
            )
        }
    }
}