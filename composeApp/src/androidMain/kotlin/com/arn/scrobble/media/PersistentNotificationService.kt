package com.arn.scrobble.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
import com.arn.scrobble.utils.Stuff

class PersistentNotificationService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        return START_STICKY
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
            nb.setContentTitle(getString(R.string.persistent_noti_desc))
        } else {
            nb.setContentTitle(BuildKonfig.APP_NAME)
        }
        runCatching {
            startForeground(ID, nb.build())
        }
    }

    companion object {
        private const val ID = 30
    }
}