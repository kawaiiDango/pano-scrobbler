package com.arn.scrobble

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.themes.ColorPatchUtils
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.coroutines.*

class ListenAlongService : Service() {

    private lateinit var nb: NotificationCompat.Builder
    private lateinit var nm: NotificationManager
    private var coroutineScope: CoroutineScope? = null
    private var currentUsername: String? = null
    private var currentTrack: Track? = null

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

        showNotification()

        intent?.extras?.getBoolean(STOP_EXTRA)?.let {
            if (it) {
                coroutineScope?.cancel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun fetchTrackLoop() {
//        coroutineScope {
        while (true) {
            val pair = withContext(Dispatchers.IO) {
                LFMRequester(
                    this@ListenAlongService,
                    coroutineScope!!
                ).execHere<Pair<String, PaginatedResult<Track>>> {
                    getFriendsRecents(currentUsername ?: "nobody")
                }
            }
            val track = pair?.second?.pageResults?.firstOrNull()
            if (track != null && (track.artist != currentTrack?.artist || track.name != currentTrack?.name)) {
                currentTrack = track

                val spotifyLink = withContext(Dispatchers.IO) {
                    SpotifyRequester.getSpotifyLink(track)
                } ?: continue

                val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyLink))
                    .setPackage(Stuff.PACKAGE_SPOTIFY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(spotifyIntent)

            }

            delay(5000)
        }
//        }
    }

    override fun onDestroy() {
        coroutineScope?.cancel()
        coroutineScope = null
        super.onDestroy()
    }

    private fun showNotification() {
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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