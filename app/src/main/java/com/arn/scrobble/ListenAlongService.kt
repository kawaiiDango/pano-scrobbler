package com.arn.scrobble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.themes.ColorPatchUtils
import de.umass.lastfm.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListenAlongService : Service() {

    private lateinit var nb: NotificationCompat.Builder
    private lateinit var nm: NotificationManager
    private var coroutineScope: CoroutineScope? = null
    private var currentUsername: String? = null
    private var currentTrack: Track? = null
    private var notificationPosted = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope!!.launch { fetchTrackLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getSingle<UserSerializable>()?.let {
            currentUsername = it.name
        }

        if (!notificationPosted) {
            showNotification()
            notificationPosted = true
        }

        intent?.extras?.getBoolean(STOP_EXTRA)?.let {
            if (it) {
                coroutineScope?.cancel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun fetchTrackLoop() {
        while (true) {
            val track = withContext(Dispatchers.IO) {
                Scrobblables.current!!.getRecents(
                    1,
                    currentUsername ?: "nobody",
                    limit = 1,
                )
            }.pageResults?.firstOrNull()
            if (track != null && (track.artist != currentTrack?.artist || track.name != currentTrack?.name)) {
                currentTrack = track

                val spotifyId = withContext(Dispatchers.IO) {
                    SpotifyRequester.getSpotifyTrack(track, 0.7f)?.id
                } ?: continue

                val spotifyLink = "https://open.spotify.com/track/$spotifyId"

                val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyLink))
                    .setPackage(Stuff.PACKAGE_SPOTIFY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(spotifyIntent)
            }

            delay(10000)
        }
    }

    override fun onDestroy() {
        coroutineScope?.cancel()
        coroutineScope = null
        super.onDestroy()
    }

    private fun showNotification() {
        nm = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.listen_along), NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            applicationContext, 8, intent,
            Stuff.updateCurrentOrImmutable
        )
        nb = NotificationCompat.Builder(applicationContext, MainPrefs.CHANNEL_NOTI_PENDING)
            .setSmallIcon(R.drawable.vd_noti_persistent)
            .setPriority(Notification.PRIORITY_LOW)
            .setContentIntent(launchIntent)
            .apply { color = (ColorPatchUtils.getNotiColor(applicationContext) ?: return@apply) }
            .addAction(
                NotificationCompat.Action(
                    R.drawable.vd_cancel, getString(R.string.close),
                    PendingIntent.getService(
                        this, NOTIFICATION_ID,
                        Intent(this, ListenAlongService::class.java)
                            .putExtra(STOP_EXTRA, true),
                        Stuff.updateCurrentOrImmutable
                    )
                )
            )
            .setContentTitle(getString(R.string.listen_along))
            .setContentText(currentUsername ?: "not set")

        startForeground(NOTIFICATION_ID, nb.build())
    }

    companion object {
        const val NOTIFICATION_ID = 21
        const val CHANNEL_ID = "listen_along"
        const val STOP_EXTRA = "stop"
    }
}