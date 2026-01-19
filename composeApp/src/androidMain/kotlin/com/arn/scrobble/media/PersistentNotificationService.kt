package com.arn.scrobble.media

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.Stuff

class PersistentNotificationService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            startForeground(
                Stuff.CHANNEL_NOTI_FG_SERVICE.hashCode(),
                PanoNotifications.persistentNotification()
            )
        }.onFailure { e ->
            Logger.e(e) { "Failed to start persistent notification service" }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        stopSelf()
        super.onDestroy()
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