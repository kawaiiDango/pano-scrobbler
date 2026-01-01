package com.arn.scrobble.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.text.Html
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media.app.NotificationCompat.MediaStyle
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.media.PlayingTrackEventReceiver
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.navigation.DeepLinkUtils
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.updates.UpdateAction
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.blocked_metadata_noti
import pano_scrobbler.composeapp.generated.resources.charts
import pano_scrobbler.composeapp.generated.resources.create_collage
import pano_scrobbler.composeapp.generated.resources.digest_monthly
import pano_scrobbler.composeapp.generated.resources.digest_weekly
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.graph_yearly
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.new_player
import pano_scrobbler.composeapp.generated.resources.new_player_prompt
import pano_scrobbler.composeapp.generated.resources.no
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_noti
import pano_scrobbler.composeapp.generated.resources.state_unscrobbled
import pano_scrobbler.composeapp.generated.resources.tap_to_edit
import pano_scrobbler.composeapp.generated.resources.top_albums
import pano_scrobbler.composeapp.generated.resources.top_artists
import pano_scrobbler.composeapp.generated.resources.top_tracks
import pano_scrobbler.composeapp.generated.resources.unlove
import pano_scrobbler.composeapp.generated.resources.update_available
import pano_scrobbler.composeapp.generated.resources.yes

actual object PanoNotifications {
    private val context
        get() = AndroidStuff.applicationContext
    private val notificationManager
        get() = AndroidStuff.notificationManager
    private val notiColor by lazy { context.getColor(R.color.pinkNoti) }
    private val nowPlayingScrobbleDataToHash = mutableMapOf<String, Pair<ScrobbleData, Int>>()

    private fun buildNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context)
            .setShowWhen(false)
            .setColor(notiColor)
            .setCustomBigContentView(null)
    }


    actual suspend fun notifyScrobble(event: PlayingTrackNotifyEvent.TrackPlaying) {
        if (event.nowPlaying)
            nowPlayingScrobbleDataToHash[event.notiKey] =
                event.origScrobbleData to event.hash
        else
            nowPlayingScrobbleDataToHash.remove(event.notiKey)

        if (!PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_SCROBBLING))
            return

        val cancelEvent = PlayingTrackNotifyEvent.TrackCancelled(
            hash = event.hash,
            showUnscrobbledNotification = true,
        )

        val loveUnloveEvent = PlayingTrackNotifyEvent.TrackLovedUnloved(
            hash = event.hash,
            loved = !event.userLoved
        )

        val loveIntent = Intent(PlayingTrackEventReceiver.BROADCAST_PLAYING_TRACK_EVENT)
            .setPackage(context.packageName)
            .putExtra(
                PlayingTrackEventReceiver.EXTRA_EVENT,
                Stuff.myJson.encodeToString(loveUnloveEvent)
            )
            .putExtra(PlayingTrackEventReceiver.EXTRA_EVENT_TYPE, loveUnloveEvent::class.simpleName)

        val lovePi = PendingIntent.getBroadcast(
            context, 4, loveIntent,
            AndroidStuff.updateCurrentOrImmutable
        )

        val loveAction = AndroidStuff.getNotificationAction(
            if (event.userLoved) R.drawable.vd_heart_filled else R.drawable.vd_heart,
            if (event.userLoved) "❤️" else "🤍",
            getString(
                if (event.userLoved) Res.string.unlove
                else Res.string.love
            ),
            lovePi
        )


        val state =
            if (event.nowPlaying)
                ""
//                    "▷ "
            else
                "✓ "

        val style = MediaStyle()
        val nb = buildNotification()
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(style)
            .apply {
                val user = Scrobblables.currentAccount.value?.user
                if (user != null) {
                    val dialogArgs = PanoRoute.Modal.MusicEntryInfo(
                        track = event.scrobbleData.toTrack(),
                        appId = event.origScrobbleData.appId,
                        user = user
                    )

                    val launchPi = DeepLinkUtils.buildDialogPendingIntent(dialogArgs)

                    setContentIntent(launchPi)
                }
            }

        if (event.userPlayCount > 0) {
            nb.setContentTitle(
                state + Stuff.formatBigHyphen(
                    event.scrobbleData.artist,
                    event.scrobbleData.track
                )
            )
                .setContentText(
                    getPluralString(
                        Res.plurals.num_scrobbles_noti,
                        event.userPlayCount,
                        "~" + event.userPlayCount.format()
                    )
                )
        } else {
            nb.setContentTitle(state + event.scrobbleData.track)
                .setContentText(event.scrobbleData.artist)
        }

        if (event.nowPlaying) {
            val editDialogArgs = PanoRoute.Modal.EditScrobble(
                origScrobbleData = event.origScrobbleData,
                hash = event.hash
            )

            val editPi =
                DeepLinkUtils.buildDialogPendingIntent(editDialogArgs)

            val editAction = AndroidStuff.getNotificationAction(
                R.drawable.vd_edit,
                "✏️",
                getString(Res.string.edit),
                editPi
            )

            val cancelIntent = Intent(PlayingTrackEventReceiver.BROADCAST_PLAYING_TRACK_EVENT)
                .setPackage(context.packageName)
                .putExtra(
                    PlayingTrackEventReceiver.EXTRA_EVENT,
                    Stuff.myJson.encodeToString(cancelEvent)
                )
                .putExtra(PlayingTrackEventReceiver.EXTRA_EVENT_TYPE, cancelEvent::class.simpleName)

            val cancelToastIntent = PendingIntent.getBroadcast(
                context, 5, cancelIntent,
                AndroidStuff.updateCurrentOrImmutable
            )

            val cancelAction = AndroidStuff.getNotificationAction(
                R.drawable.vd_remove,
                "⛔️",
                context.getString(android.R.string.cancel),
                cancelToastIntent
            )

            nb.addAction(cancelAction)
            nb.addAction(editAction)
            nb.addAction(loveAction)
            style.setShowActionsInCompactView(0, 1, 2)
        } else {
            val blockedMetadata = BlockedMetadata(
                track = event.scrobbleData.track,
                album = event.scrobbleData.album.orEmpty(),
                artist = event.scrobbleData.artist,
                albumArtist = event.scrobbleData.albumArtist.orEmpty(),
            )

            val dialogArgs = PanoRoute.Modal.BlockedMetadataAdd(
                blockedMetadata = blockedMetadata,
                hash = event.hash
            )

            val blockPi = DeepLinkUtils.buildDialogPendingIntent(dialogArgs)
            val blockAction = AndroidStuff.getNotificationAction(
                R.drawable.vd_ban,
                "🚫",
                getString(Res.string.block),
                blockPi
            )
            nb.addAction(blockAction)
            nb.addAction(loveAction)
            style.setShowActionsInCompactView(0, 1)
        }

        try {
            notificationManager.notify(event.notiKey, 0, nb.build())
        } catch (e: RuntimeException) {
            val nExpandable = nb.setLargeIcon(null as Bitmap?)
                .setStyle(null)
                .build()
            notificationManager.notify(event.notiKey, 0, nExpandable)
        }
    }

    actual suspend fun notifyError(event: PlayingTrackNotifyEvent.Error) {
        if (!PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_SCR_ERR))
            return

        val subtitle = event.scrobbleError.description
            ?: Stuff.formatBigHyphen(
                event.scrobbleData.artist,
                event.scrobbleData.track
            )

        val title = if (event.scrobbleError.canFixMetadata)
            getString(Res.string.tap_to_edit) +
                    " • " + event.scrobbleError.title
        else
            event.scrobbleError.title

        val nb = buildNotification()
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(event.scrobbleError.title)
                    .bigText(subtitle)
            )
            .also {
                if (event.scrobbleError.canFixMetadata) {
                    val editDialogArgs = PanoRoute.Modal.EditScrobble(
                        origScrobbleData = event.scrobbleData,
                        hash = event.hash
                    )

                    val editPi =
                        DeepLinkUtils.buildDialogPendingIntent(editDialogArgs)

                    it.setContentIntent(editPi)
                    it.setChannelId(Stuff.CHANNEL_NOTI_SCR_ERR)
                } else {
                    it.setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
                }
            }

        notificationManager.notify(event.notiKey, 0, nb.build())
    }

    actual suspend fun notifyAppDetected(appId: String, appLabel: String) {
        if (!PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP))
            return

        val allowEvent = PlayingTrackNotifyEvent.AppAllowedBlocked(
            appId = appId,
            allowed = true
        )

        val blockEvent = PlayingTrackNotifyEvent.AppAllowedBlocked(
            appId = appId,
            allowed = false
        )

        val blockIntent = Intent(PlayingTrackEventReceiver.BROADCAST_PLAYING_TRACK_EVENT)
            .setPackage(context.packageName)
            .putExtra(
                PlayingTrackEventReceiver.EXTRA_EVENT,
                Stuff.myJson.encodeToString(blockEvent)
            )
            .putExtra(PlayingTrackEventReceiver.EXTRA_EVENT_TYPE, blockEvent::class.simpleName)
        val blockPi = PendingIntent.getBroadcast(
            context, 1, blockIntent,
            AndroidStuff.updateCurrentOrImmutable
        )

        val allowIntent = Intent(PlayingTrackEventReceiver.BROADCAST_PLAYING_TRACK_EVENT)
            .setPackage(context.packageName)
            .putExtra(
                PlayingTrackEventReceiver.EXTRA_EVENT,
                Stuff.myJson.encodeToString(allowEvent)
            )
            .putExtra(PlayingTrackEventReceiver.EXTRA_EVENT_TYPE, allowEvent::class.simpleName)

        val allowPi = PendingIntent.getBroadcast(
            context, 2, allowIntent,
            AndroidStuff.updateCurrentOrImmutable
        )

        val n = buildNotification()
            .setContentTitle(
                getString(
                    Res.string.new_player,
                    appLabel
                )
            )
            .setContentText(
                getString(Res.string.new_player_prompt)
            )
            .setChannelId(Stuff.CHANNEL_NOTI_NEW_APP)
            .setSmallIcon(R.drawable.vd_appquestion_noti)
            .addAction(
                AndroidStuff.getNotificationAction(
                    R.drawable.vd_ban,
                    "\uD83D\uDEAB",
                    getString(Res.string.no),
                    blockPi
                )
            )
            .addAction(
                AndroidStuff.getNotificationAction(
                    R.drawable.vd_check,
                    "✔",
                    getString(Res.string.yes),
                    allowPi
                )
            )
            .setStyle(
                MediaStyle().setShowActionsInCompactView(0, 1)
            )
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Stuff.CHANNEL_NOTI_NEW_APP, 0, n)
    }


    actual suspend fun notifyUnscrobbled(notiKey: String, scrobbleData: ScrobbleData, hash: Int) {
        val delayTime = 4000L

        val blockedMetadata = BlockedMetadata(
            track = scrobbleData.track,
            album = scrobbleData.album.orEmpty(),
            artist = scrobbleData.artist,
            albumArtist = scrobbleData.albumArtist.orEmpty(),
        )

        val dialogArgs = PanoRoute.Modal.BlockedMetadataAdd(
            blockedMetadata = blockedMetadata,
            hash = hash
        )

        val blockPi = DeepLinkUtils.buildDialogPendingIntent(dialogArgs)

        val nb = buildNotification()
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(
                getString(Res.string.state_unscrobbled) + " • " +
                        getString(Res.string.blocked_metadata_noti)
            )
            .setContentIntent(blockPi)
            .setTimeoutAfter(delayTime)
        notificationManager.notify(notiKey, 0, nb.build())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            GlobalScope.launch {
                delay(delayTime)
                notificationManager.cancel(notiKey, 0)
            }
    }

    actual suspend fun notifyDigest(timePeriod: TimePeriod, resultsList: List<Pair<Int, String>>) {
        if (resultsList.isEmpty()) {
            return
        }

        val notificationTextList = mutableListOf<String>()
        resultsList.forEach { (type, text) ->
            val title = when (type) {
                Stuff.TYPE_ARTISTS -> getString(Res.string.top_artists)
                Stuff.TYPE_ALBUMS -> getString(Res.string.top_albums)
                Stuff.TYPE_TRACKS -> getString(Res.string.top_tracks)
                else -> throw IllegalArgumentException("Invalid musicEntry type")
            }
            notificationTextList += "<b>$title:</b>\n$text"
        }

        val notificationTitle = when (timePeriod.lastfmPeriod) {
            LastfmPeriod.WEEK -> getString(Res.string.digest_weekly)
            LastfmPeriod.MONTH -> getString(Res.string.digest_monthly)
            LastfmPeriod.YEAR -> getString(Res.string.graph_yearly)
            else -> throw IllegalArgumentException("Invalid period")
        }


        val notificationText = Html.fromHtml(
            notificationTextList.joinToString("\n"),
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        )

        val channelId = if (timePeriod.lastfmPeriod == LastfmPeriod.WEEK)
            Stuff.CHANNEL_NOTI_DIGEST_WEEKLY
        else
            Stuff.CHANNEL_NOTI_DIGEST_MONTHLY

        val dialogArgs = PanoRoute.Modal.CollageGenerator(
            collageType = Stuff.TYPE_ALL,
            timePeriod = timePeriod,
            user = Scrobblables.currentAccount.value?.user ?: return
        )

        val collagePi = DeepLinkUtils.buildDialogPendingIntent(dialogArgs)

        val chartsDeepLinkUri =
            DeepLinkUtils.createMainActivityDeepLinkUri(PanoRoute.SelfHomePager(timePeriod.lastfmPeriod.name))
        val chartsPi =
            PendingIntent.getActivity(
                context,
                chartsDeepLinkUri.hashCode(),
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    action = Intent.ACTION_VIEW
                    data = chartsDeepLinkUri
                },
                AndroidStuff.updateCurrentOrImmutable
            )!!

        val nb = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.vd_charts)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(notificationTitle)
            .setContentIntent(chartsPi)
            .apply { color = context.getColor(R.color.pinkNoti) }
            .addAction(
                AndroidStuff.getNotificationAction(
                    R.drawable.vd_mosaic,
                    "🖼️",
                    getString(Res.string.create_collage),
                    collagePi
                )
            )
            .addAction(
                AndroidStuff.getNotificationAction(
                    R.drawable.vd_charts,
                    "📊",
                    getString(Res.string.charts),
                    chartsPi
                )
            )
            .setContentText(notificationText)
            .setShowWhen(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(notificationTitle)
                    .bigText(notificationText)
            )

        notificationManager.notify(channelId, timePeriod.lastfmPeriod.ordinal, nb.build())
    }

    actual suspend fun notifyUpdater(updateAction: UpdateAction) {
        if (PlatformStuff.noUpdateCheck) return

        // create channel if not exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ContextCompat.getSystemService(
                AndroidStuff.applicationContext,
                NotificationManager::class.java
            )!!

            nm.createNotificationChannel(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_UPDATER,
                    getString(Res.string.update_available, ""),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        if (!PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_UPDATER))
            return

        val contentIntent = Intent(Intent.ACTION_VIEW).apply {
            data = updateAction.urlOrFilePath.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val contentPi = PendingIntent.getActivity(
            context, 32, contentIntent,
            AndroidStuff.updateCurrentOrImmutable
        )

        val nb = buildNotification()
            .setSmallIcon(R.drawable.vd_noti)
            .setContentTitle(getString(Res.string.update_available, updateAction.version))
            .setChannelId(Stuff.CHANNEL_NOTI_UPDATER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPi)
            .setAutoCancel(true)

        notificationManager.notify(Stuff.CHANNEL_NOTI_UPDATER, 0, nb.build())
    }

    actual fun removeNotificationByKey(key: String) {
        nowPlayingScrobbleDataToHash.remove(key)

        notificationManager.cancel(key, 0)
    }

    fun getNowPlayingFromBackgroundProcess(): Pair<ScrobbleData, Int>? {
        return nowPlayingScrobbleDataToHash.values.firstOrNull()
    }
}