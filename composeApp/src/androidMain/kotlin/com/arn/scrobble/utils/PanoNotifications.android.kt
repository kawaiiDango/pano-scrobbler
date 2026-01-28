package com.arn.scrobble.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.ForegroundInfo
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.blocked_metadata_noti
import pano_scrobbler.composeapp.generated.resources.charts
import pano_scrobbler.composeapp.generated.resources.create_collage
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.new_player
import pano_scrobbler.composeapp.generated.resources.new_player_prompt
import pano_scrobbler.composeapp.generated.resources.no
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_noti
import pano_scrobbler.composeapp.generated.resources.state_unscrobbled
import pano_scrobbler.composeapp.generated.resources.tap_to_edit
import pano_scrobbler.composeapp.generated.resources.unlove
import pano_scrobbler.composeapp.generated.resources.update_available
import pano_scrobbler.composeapp.generated.resources.yes

actual object PanoNotifications {
    private val context
        get() = AndroidStuff.applicationContext
    private val notificationManager by lazy {
        AndroidStuff.applicationContext.getSystemService(NotificationManager::class.java)!!
    }
    private val notiColor by lazy { context.getColor(R.color.pinkNoti) }
    private val nowPlayingScrobbleDataToHash = mutableMapOf<String, Pair<ScrobbleData, Int>>()
    private var channelsCreated = false

    private fun buildNotificationAction(
        icon: Int,
        emoji: String,
        text: String,
        pIntent: PendingIntent,
    ): Notification.Action {
        val icon = Icon.createWithResource(context, icon)
        return Notification.Action.Builder(icon, text, pIntent).build()
    }

    private fun Notification.Builder.setChannelIdCompat(channelId: String): Notification.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!channelsCreated) {
                channelsCreated = true
                createChannels()
            }

            setChannelId(channelId)
        }
        return this
    }

    private fun Notification.Builder.setTimeoutAfterCompat(ms: Long): Notification.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setTimeoutAfter(ms)
        }
        return this
    }

    actual suspend fun notifyScrobble(event: PlayingTrackNotifyEvent.TrackPlaying) {
        if (event.nowPlaying)
            nowPlayingScrobbleDataToHash[event.notiKey] =
                event.origScrobbleData to event.hash
        else
            nowPlayingScrobbleDataToHash.remove(event.notiKey)

        if (!isNotiChannelEnabled(Stuff.CHANNEL_NOTI_SCROBBLING))
            return

        val cancelEvent = PlayingTrackNotifyEvent.TrackCancelled(
            hash = event.hash,
            showUnscrobbledNotification = true,
        )

        val loveUnloveEvent = PlayingTrackNotifyEvent.TrackLovedUnloved(
            hash = event.hash,
            notiKey = event.notiKey,
            scrobbleData = event.scrobbleData,
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

        val loveAction = buildNotificationAction(
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

        val style = Notification.MediaStyle()
        val nb = Notification.Builder(context)
            .setChannelIdCompat(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setGroup(Stuff.GROUP_NOTI_SCROBBLES)
            .setShowWhen(false)
            .setColor(notiColor)
            .setCustomBigContentView(null)
            .setSmallIcon(R.drawable.vd_noti)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(style)
            .apply {
                val user =
                    PlatformStuff.mainPrefs.data.map { it.currentAccount?.user }.first()
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

            val editAction = buildNotificationAction(
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

            val cancelAction = buildNotificationAction(
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
            val blockAction = buildNotificationAction(
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
        if (!isNotiChannelEnabled(Stuff.CHANNEL_NOTI_SCR_ERR))
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

        val nb = Notification.Builder(context)
            .setGroup(Stuff.GROUP_NOTI_SCROBBLES)
            .setShowWhen(false)
            .setColor(notiColor)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                Notification.BigTextStyle()
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
                    it.setChannelIdCompat(Stuff.CHANNEL_NOTI_SCR_ERR)
                } else {
                    it.setChannelIdCompat(Stuff.CHANNEL_NOTI_SCROBBLING)
                }
            }

        notificationManager.notify(event.notiKey, 0, nb.build())
    }

    actual suspend fun notifyAppDetected(appId: String, appLabel: String) {
        if (!isNotiChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP))
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

        val n = Notification.Builder(context)
            .setChannelIdCompat(Stuff.CHANNEL_NOTI_NEW_APP)
            .setGroup(Stuff.GROUP_NOTI_SCROBBLES)
            .setShowWhen(false)
            .setColor(notiColor)
            .setCustomBigContentView(null)
            .setContentTitle(
                getString(
                    Res.string.new_player,
                    appLabel
                )
            )
            .setContentText(
                getString(Res.string.new_player_prompt)
            )
            .setSmallIcon(R.drawable.vd_appquestion_noti)
            .addAction(
                buildNotificationAction(
                    R.drawable.vd_ban,
                    "\uD83D\uDEAB",
                    getString(Res.string.no),
                    blockPi
                )
            )
            .addAction(
                buildNotificationAction(
                    R.drawable.vd_check,
                    "✔",
                    getString(Res.string.yes),
                    allowPi
                )
            )
            .setStyle(
                Notification.MediaStyle().setShowActionsInCompactView(0, 1)
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

        val nb = Notification.Builder(context)
            .setChannelIdCompat(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setGroup(Stuff.GROUP_NOTI_SCROBBLES)
            .setShowWhen(false)
            .setColor(notiColor)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(
                getString(Res.string.state_unscrobbled) + " • " +
                        getString(Res.string.blocked_metadata_noti)
            )
            .setContentIntent(blockPi)
            .setTimeoutAfterCompat(delayTime)
        notificationManager.notify(notiKey, 0, nb.build())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            GlobalScope.launch {
                delay(delayTime)
                notificationManager.cancel(notiKey, 0)
            }
    }

    actual suspend fun notifyDigest(lastfmPeriod: LastfmPeriod, title: String, text: String) {
        if (text.isEmpty()) {
            return
        }

        val channelId = if (lastfmPeriod == LastfmPeriod.WEEK)
            Stuff.CHANNEL_NOTI_DIGEST_WEEKLY
        else
            Stuff.CHANNEL_NOTI_DIGEST_MONTHLY

        val user =
            PlatformStuff.mainPrefs.data.map { it.currentAccount?.user }.first() ?: return
        val dialogArgs = PanoRoute.Modal.CollageGenerator(
            collageType = Stuff.TYPE_ALL,
            timePeriod = TimePeriod(lastfmPeriod),
            user = user
        )

        val collagePi = DeepLinkUtils.buildDialogPendingIntent(dialogArgs)

        val chartsDeepLinkUri =
            DeepLinkUtils.createMainActivityDeepLinkUri(PanoRoute.SelfHomePager(lastfmPeriod.name))
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

        val nb = Notification.Builder(context)
            .setChannelIdCompat(channelId)
            .setGroup(Stuff.GROUP_NOTI_DIGESTS)
            .setColor(notiColor)
            .setSmallIcon(R.drawable.vd_charts)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(title)
            .setContentIntent(chartsPi)
            .addAction(
                buildNotificationAction(
                    R.drawable.vd_mosaic,
                    "🖼️",
                    getString(Res.string.create_collage),
                    collagePi
                )
            )
            .addAction(
                buildNotificationAction(
                    R.drawable.vd_charts,
                    "📊",
                    getString(Res.string.charts),
                    chartsPi
                )
            )
            .setContentText(text)
            .setShowWhen(true)
            .setStyle(
                Notification.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(text)
            )

        notificationManager.notify(channelId, lastfmPeriod.ordinal, nb.build())
    }

    actual suspend fun notifyUpdater(updateAction: UpdateAction) {
        if (PlatformStuff.noUpdateCheck) return

        // create channel if not exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    Stuff.CHANNEL_NOTI_UPDATER,
                    getString(Res.string.update_available, ""),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        if (!isNotiChannelEnabled(Stuff.CHANNEL_NOTI_UPDATER))
            return

        val contentIntent = Intent(Intent.ACTION_VIEW).apply {
            data = updateAction.urlOrFilePath.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val contentPi = PendingIntent.getActivity(
            context, 32, contentIntent,
            AndroidStuff.updateCurrentOrImmutable
        )

        val nb = Notification.Builder(context)
            .setChannelIdCompat(Stuff.CHANNEL_NOTI_UPDATER)
            .setGroup(Stuff.GROUP_NOTI_UPDATER)
            .setShowWhen(false)
            .setColor(notiColor)
            .setSmallIcon(R.drawable.vd_noti)
            .setContentTitle(getString(Res.string.update_available, updateAction.version))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPi)
            .setAutoCancel(true)

        notificationManager.notify(Stuff.CHANNEL_NOTI_UPDATER, 0, nb.build())
    }


    actual fun isNotiChannelEnabled(channelId: String): Boolean {
        return when {
            PlatformStuff.isTv -> false

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                notificationManager.areNotificationsEnabled() &&
                        notificationManager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
            }

            else -> true
        }
    }

    actual fun removeNotificationByKey(key: String) {
        nowPlayingScrobbleDataToHash.remove(key)

        notificationManager.cancel(key, 0)
    }

    fun getNowPlayingFromBackgroundProcess(): Pair<ScrobbleData, Int>? {
        return nowPlayingScrobbleDataToHash.values.firstOrNull()
    }

    fun createForegroundInfo(title: String): ForegroundInfo {
        val intent = Intent(context, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            context, 8, intent,
            AndroidStuff.updateCurrentOrImmutable
        )
        val notification = Notification.Builder(context)
            .setChannelIdCompat(Stuff.CHANNEL_NOTI_FG_SERVICE)
            .setGroup(Stuff.GROUP_NOTI_FG_SERVICE)
            .setShowWhen(false)
            .setColor(notiColor)
            .setSmallIcon(R.drawable.vd_noti_persistent)
            .setPriority(Notification.PRIORITY_MIN)
            .setContentIntent(launchIntent)
            .setContentTitle(title)
            .build()

        return ForegroundInfo(
            title.hashCode(),
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else
                0
        )
    }

    fun persistentNotification(): Notification {
        val nb =
            Notification.Builder(context)
                .setChannelIdCompat(Stuff.CHANNEL_NOTI_FG_SERVICE)
                .setSmallIcon(R.drawable.vd_noti_persistent)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(notiColor)
                .setGroup(Stuff.GROUP_NOTI_FG_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, Stuff.CHANNEL_NOTI_FG_SERVICE)
            }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    100,
                    intent,
                    AndroidStuff.updateCurrentOrImmutable
                )
            nb.setContentIntent(pendingIntent)
            nb.setContentTitle(context.getString(R.string.persistent_noti_text))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                nb.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
        } else {
            nb.setContentTitle(BuildKonfig.APP_NAME)
            nb.setPriority(Notification.PRIORITY_MIN)
        }

        return nb.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        // Create channel groups first
        val groups = mutableListOf<NotificationChannelGroup>()

        groups += NotificationChannelGroup(
            Stuff.GROUP_NOTI_SCROBBLES,
            context.getString(R.string.scrobbles)
        )
        groups += NotificationChannelGroup(
            Stuff.GROUP_NOTI_DIGESTS,
            context.getString(R.string.charts)
        )
        groups += NotificationChannelGroup(
            Stuff.GROUP_NOTI_FG_SERVICE,
            context.getString(R.string.show_persistent_noti)
        )

        notificationManager.createNotificationChannelGroups(groups)

        // Now create channels
        val channels = mutableListOf<NotificationChannel>()

        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_SCROBBLING,
            context.getString(R.string.state_scrobbling),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_SCROBBLES
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_SCR_ERR,
            context.getString(R.string.channel_err),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            group = Stuff.GROUP_NOTI_SCROBBLES
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_NEW_APP,
            context.getString(
                R.string.new_player,
                context.getString(R.string.new_app)
            ),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_SCROBBLES
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_DIGEST_WEEKLY,
            context.getString(
                R.string.s_top_scrobbles,
                context.getString(R.string.weekly)
            ),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_DIGESTS
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_DIGEST_MONTHLY,
            context.getString(
                R.string.s_top_scrobbles,
                context.getString(R.string.monthly)
            ),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_DIGESTS
        }
        channels += NotificationChannel(
            Stuff.CHANNEL_NOTI_FG_SERVICE,
            context.getString(R.string.show_persistent_noti),
            // foreground service noti cannot be IMPORTANCE_MIN
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            group = Stuff.GROUP_NOTI_FG_SERVICE
        }

        notificationManager.createNotificationChannels(channels)
        notificationManager.deleteNotificationChannel("noti_persistent")
        notificationManager.deleteNotificationChannel("noti_pending_scrobbles")
    }
}