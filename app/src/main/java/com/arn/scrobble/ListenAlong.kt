package com.arn.scrobble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.main.App
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.delay

object ListenAlong {

    private lateinit var nm: NotificationManager
    private var currentTrack: Track? = null

    const val NOTIFICATION_ID = 21
    private const val CHANNEL_ID = "listen_along"
    const val STOP_EXTRA = "stop"
    const val USERNAME_EXTRA = "username"

    suspend fun fetchTrackLoop(username: String) {
        while (true) {
            val track = Scrobblables.current!!.getRecents(
                1,
                username,
                limit = 1,
            ).map { it.entries.firstOrNull() }
                .getOrNull()
            if (track != null && (track.artist.name != currentTrack?.artist?.name || track.name != currentTrack?.name)) {
                currentTrack = track

                Stuff.launchSearchIntent(track, Stuff.PACKAGE_SPOTIFY)
                delay(1000)
                Stuff.launchSearchIntent(track, Stuff.PACKAGE_SPOTIFY)
            }
            showNotification(username)

            delay(10000)
        }
    }

    private fun showNotification(username: String) {
        nm = ContextCompat.getSystemService(App.context, NotificationManager::class.java)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    App.context.getString(R.string.listen_along), NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val intent = Intent(App.context, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            App.context, 8, intent,
            Stuff.updateCurrentOrImmutable
        )
        val notification = NotificationCompat.Builder(App.context, MainPrefs.CHANNEL_NOTI_PENDING)
            .setSmallIcon(R.drawable.vd_noti_persistent)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(launchIntent)
            .apply { color = (ColorPatchUtils.getNotiColor(App.context) ?: return@apply) }
            .addAction(
                NotificationCompat.Action(
                    R.drawable.vd_cancel, App.context.getString(R.string.close),
                    PendingIntent.getBroadcast(
                        App.context, NOTIFICATION_ID,
                        Intent(NLService.iLISTEN_ALONG)
                            .setPackage(App.context.packageName)
                            .putExtra(STOP_EXTRA, true),
                        Stuff.updateCurrentOrImmutable
                    )
                )
            )
            .setContentTitle(App.context.getString(R.string.listen_along))
            .setContentText(username)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

}