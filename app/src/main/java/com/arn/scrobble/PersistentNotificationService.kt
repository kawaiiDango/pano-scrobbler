package com.arn.scrobble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arn.scrobble.pref.MainPrefs

class PersistentNotificationService : Service() {

    // I hope that the popular notification pushing app was right and this thing actually works.

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        return START_STICKY
    }

    private fun showNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    MainPrefs.CHANNEL_NOTI_PERSISTENT,
                    getString(R.string.show_persistent_noti), NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        val nb = NotificationCompat.Builder(applicationContext, MainPrefs.CHANNEL_NOTI_PERSISTENT)
            .setSmallIcon(R.drawable.vd_noti_persistent)
            .setPriority(Notification.PRIORITY_MIN)
            .setContentTitle(getString(R.string.persistent_noti_desc))
            .setContentText(getString(R.string.scrobbler_on))

        startForeground(ID, nb.build())
    }
}

private const val ID = 30