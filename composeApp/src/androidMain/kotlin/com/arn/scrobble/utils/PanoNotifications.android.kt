package com.arn.scrobble.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.text.Html
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.app.NotificationCompat.MediaStyle
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainDialogActivity
import com.arn.scrobble.media.NLService
import com.arn.scrobble.media.PlayingTrackInfo
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.ScrobbleError
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.serializableType
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.digest_monthly
import pano_scrobbler.composeapp.generated.resources.digest_weekly
import pano_scrobbler.composeapp.generated.resources.graph_yearly
import pano_scrobbler.composeapp.generated.resources.top_albums
import pano_scrobbler.composeapp.generated.resources.top_artists
import pano_scrobbler.composeapp.generated.resources.top_tracks

actual object PanoNotifications {
    private val context = AndroidStuff.application
    private val notificationManager = AndroidStuff.notificationManager
    private val notiColor by lazy { context.getColor(R.color.pinkNoti) }
    private var notiIconBitmap: Bitmap? = null

    private fun buildNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context)
            .setShowWhen(false)
            .apply {
                // coloring on android 6 looks very ugly
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    setColor(notiColor)
            }
            .setAutoCancel(true)
            .setCustomBigContentView(null)
    }


    actual fun notifyScrobble(trackInfo: PlayingTrackInfo, nowPlaying: Boolean) {
        if (!PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_SCROBBLING))
            return

        val cancelEvent = PlayingTrackNotifyEvent.TrackCancelled(
            hash = trackInfo.hash,
            showUnscrobbledNotification = true,
            markAsScrobbled = true
        )

        val loveUnloveEvent = PlayingTrackNotifyEvent.TrackLovedUnloved(
            hash = trackInfo.hash,
            loved = !trackInfo.userLoved
        )

        val loveAction = if (trackInfo.userLoved) {
            val i = Intent(NLService.iUNLOVE)
                .setPackage(context.packageName)
                .putExtra(Stuff.EXTRA_EVENT, Stuff.myJson.encodeToString(loveUnloveEvent))

            val loveIntent = PendingIntent.getBroadcast(
                context, 4, i,
                AndroidStuff.updateCurrentOrImmutable
            )
            AndroidStuff.getNotificationAction(
                R.drawable.vd_heart_filled,
                "\uD83E\uDD0D",
                context.getString(R.string.unlove),
                loveIntent
            )
        } else {
            val i = Intent(NLService.iLOVE)
                .setPackage(context.packageName)
                .putExtra(Stuff.EXTRA_EVENT, Stuff.myJson.encodeToString(loveUnloveEvent))

            val loveIntent = PendingIntent.getBroadcast(
                context, 3, i,
                AndroidStuff.updateCurrentOrImmutable
            )
            AndroidStuff.getNotificationAction(
                R.drawable.vd_heart,
                "\uD83E\uDD0D",
                context.getString(R.string.love),
                loveIntent
            )
        }

        val i = Intent(NLService.iCANCEL)
            .setPackage(context.packageName)
            .putExtra(Stuff.EXTRA_EVENT, Stuff.myJson.encodeToString(cancelEvent))

        val cancelToastIntent = PendingIntent.getBroadcast(
            context, 5, i,
            AndroidStuff.updateCurrentOrImmutable
        )


        val state =
            if (nowPlaying)
                ""
//                    "▷ "
            else
                "✓ "

        val style = MediaStyle()
        val nb = buildNotification()
            .setAutoCancel(false)
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(style)
            .addAction(loveAction)
            .apply {
                val user = Scrobblables.currentScrobblableUser
                if (user != null) {
                    val route = PanoRoute.MusicEntryInfo(
                        track = trackInfo.toTrack(),
                        pkgName = null,
                        user = user
                    )

                    val deepLinkUri =
                        Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MusicEntryInfo::class.simpleName + "/" +
                                serializableType<UserCached>().serializeAsValue(route.user) + "?" +
                                route::track.name + "=" + serializableType<Track>().serializeAsValue(
                            route.track!!
                        )
                    val launchPi =
                        MainDialogActivity.createDestinationPendingIntent(deepLinkUri)

                    setContentIntent(launchPi)
                }
            }

        if (trackInfo.userPlayCount > 0)
            nb.setContentTitle(
                state + Stuff.formatBigHyphen(
                    trackInfo.artist,
                    trackInfo.title
                )
            )
                .setContentText(
                    context.resources.getQuantityString(
                        R.plurals.num_scrobbles_noti,
                        trackInfo.userPlayCount,
                        "~" + trackInfo.userPlayCount.format()
                    )
                )
        else
            nb.setContentTitle(state + trackInfo.title)
                .setContentText(trackInfo.artist)

        if (nowPlaying) {
            val editRoute = PanoRoute.EditScrobble(
                scrobbleData = trackInfo.toScrobbleData(),
                hash = trackInfo.hash
            )

            val editDeepLinkUri =
                Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.EditScrobble::class.simpleName + "/" +
                        serializableType<ScrobbleData>().serializeAsValue(editRoute.scrobbleData) +
                        "?" + editRoute::hash.name + "=" + editRoute.hash

            val editPi =
                MainDialogActivity.createDestinationPendingIntent(editDeepLinkUri)

            val editAction = AndroidStuff.getNotificationAction(
                R.drawable.vd_edit,
                "✏️",
                context.getString(R.string.edit),
                editPi
            )

            val unscrobbleAction = AndroidStuff.getNotificationAction(
                R.drawable.vd_remove,
                "⛔️",
                context.getString(R.string.unscrobble),
                cancelToastIntent
            )

            nb.addAction(editAction)
            nb.addAction(unscrobbleAction)
            style.setShowActionsInCompactView(0, 1, 2)
        } else {
            style.setShowActionsInCompactView(0)
        }

        try {
            notificationManager.notify(trackInfo.appId, 0, nb.buildWithMediaStyleMod())
        } catch (e: RuntimeException) {
            val nExpandable = nb.setLargeIcon(null as Bitmap?)
                .setStyle(null)
                .build()
            notificationManager.notify(trackInfo.appId, 0, nExpandable)
        }
    }

    actual fun notifyBadMeta(trackInfo: PlayingTrackInfo, scrobbleError: ScrobbleError) {
        val editRoute = PanoRoute.EditScrobble(
            scrobbleData = trackInfo.toScrobbleData(),
            hash = trackInfo.hash
        )

        val editDeepLinkUri =
            Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.EditScrobble::class.simpleName + "/" +
                    serializableType<ScrobbleData>().serializeAsValue(editRoute.scrobbleData) +
                    "?" + editRoute::hash.name + "=" + editRoute.hash

        val editPi =
            MainDialogActivity.createDestinationPendingIntent(editDeepLinkUri)

        val subtitleSpanned = if (scrobbleError.description != null)
            Html.fromHtml(scrobbleError.description)
        else
            Stuff.formatBigHyphen(
                trackInfo.artist,
                trackInfo.title
            )

        val nb = buildNotification()
            .setAutoCancel(false)
            .setChannelId(Stuff.CHANNEL_NOTI_SCR_ERR)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentIntent(editPi)
            .setContentText(subtitleSpanned)
            .setContentTitle(
                "${trackInfo.title} " + context.getString(R.string.tap_to_edit)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(scrobbleError.title)
                    .bigText(subtitleSpanned)
            )

        notificationManager.notify(trackInfo.appId, 0, nb.build())
    }

    actual fun notifyOtherError(trackInfo: PlayingTrackInfo, scrobbleError: ScrobbleError) {
        val intent = Intent(context, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            context, 8, intent,
            AndroidStuff.updateCurrentOrImmutable
        )
        val spanned = Html.fromHtml(scrobbleError.description)

        val nb = buildNotification()
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setContentIntent(launchIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentText(spanned) //required on recent oneplus devices

        val isMinimised = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(Stuff.CHANNEL_NOTI_SCROBBLING).importance < NotificationManager.IMPORTANCE_LOW
        if (isMinimised)
            nb.setContentTitle(scrobbleError.description?.replace("</?br?>".toRegex(), ""))
        else
            nb.setContentTitle(scrobbleError.title)

        nb.setStyle(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(scrobbleError.title)
                .bigText(spanned)
        )

        notificationManager.notify(scrobbleError.appId, 0, nb.build())
    }

    actual fun notifyAppDetected(appId: String, appLabel: String) {
        val allowEvent = PlayingTrackNotifyEvent.AppAllowedBlocked(
            appId = appId,
            allowed = true
        )

        val blockEvent = PlayingTrackNotifyEvent.AppAllowedBlocked(
            appId = appId,
            allowed = false
        )

        val blockIntent = Intent(NLService.iAPP_ALLOWED_BLOCKED)
            .setPackage(context.packageName)
            .putExtra(Stuff.EXTRA_EVENT, Stuff.myJson.encodeToString(blockEvent))
        val blockPi = PendingIntent.getBroadcast(
            context, 1, blockIntent,
            AndroidStuff.updateCurrentOrImmutable
        )

        val allowIntent = Intent(NLService.iAPP_ALLOWED_BLOCKED)
            .setPackage(context.packageName)
            .putExtra(Stuff.EXTRA_EVENT, Stuff.myJson.encodeToString(allowEvent))
        val allowPi = PendingIntent.getBroadcast(
            context, 2, allowIntent,
            AndroidStuff.updateCurrentOrImmutable
        )

        val n = buildNotification()
            .setContentTitle(
                context.getString(
                    R.string.new_player,
                    appLabel
                )
            )
            .setContentText(
                context.getString(R.string.new_player_prompt)
            )
            .setChannelId(Stuff.CHANNEL_NOTI_NEW_APP)
            .setSmallIcon(R.drawable.vd_appquestion_noti)
            .addAction(
                AndroidStuff.getNotificationAction(
                    R.drawable.vd_ban,
                    "\uD83D\uDEAB",
                    context.getString(R.string.no),
                    blockPi
                )
            )
            .addAction(
                AndroidStuff.getNotificationAction(
                    R.drawable.vd_check,
                    "✔",
                    context.getString(R.string.yes),
                    allowPi
                )
            )
            .setStyle(
                MediaStyle().setShowActionsInCompactView(0, 1)
            )
            .buildWithMediaStyleMod()
        notificationManager.notify(Stuff.CHANNEL_NOTI_NEW_APP, 0, n)
    }


    actual fun notifyUnscrobbled(trackInfo: PlayingTrackInfo) {
        val delayTime = 4000L

        val blockedMetadata = BlockedMetadata(
            track = trackInfo.title,
            album = trackInfo.album,
            artist = trackInfo.artist,
            albumArtist = trackInfo.albumArtist,
        )

        val route = PanoRoute.BlockedMetadataAdd(
            blockedMetadata = blockedMetadata,
            ignoredArtist = trackInfo.origArtist,
            hash = trackInfo.hash
        )

        val deepLinkUri =
            Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.BlockedMetadataAdd::class.simpleName + "?" +
                    route::blockedMetadata.name + "=" +
                    serializableType<BlockedMetadata>().serializeAsValue(route.blockedMetadata) +
                    "&" + route::ignoredArtist.name + "=" + Uri.encode(route.ignoredArtist) + "&hash=" + route.hash

        val blockPi =
            MainDialogActivity.createDestinationPendingIntent(deepLinkUri)

        val nb = buildNotification()
            .setChannelId(Stuff.CHANNEL_NOTI_SCROBBLING)
            .setSmallIcon(R.drawable.vd_noti_err)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(
                context.getString(R.string.state_unscrobbled) + " • " +
                        context.getString(R.string.blocked_metadata_noti)
            )
            .setContentIntent(blockPi)
            .setTimeoutAfter(delayTime)
        notificationManager.notify(trackInfo.appId, 0, nb.build())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            GlobalScope.launch {
                delay(delayTime)
                notificationManager.cancel(trackInfo.appId, 0)
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


        val notificationText = Html.fromHtml(notificationTextList.joinToString("<br>\n"))

        val channelId = if (timePeriod.lastfmPeriod == LastfmPeriod.WEEK)
            Stuff.CHANNEL_NOTI_DIGEST_WEEKLY
        else
            Stuff.CHANNEL_NOTI_DIGEST_MONTHLY

        val route = PanoRoute.CollageGenerator(
            collageType = Stuff.TYPE_ALL,
            timePeriod = timePeriod,
            user = Scrobblables.currentScrobblableUser ?: return
        )

        val deepLinkUri =
            Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.CollageGenerator::class.simpleName + "/" +
                    route.collageType + "/" + route.timePeriod + "/" +
                    serializableType<UserCached>().serializeAsValue(route.user)

        val launchPi =
            MainDialogActivity.createDestinationPendingIntent(deepLinkUri)

        val nb = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.vd_charts)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(notificationTitle)
            .setContentIntent(launchPi)
            .apply { color = context.getColor(R.color.pinkNoti) }
            .addAction(
                AndroidStuff.getNotificationAction(
                    R.drawable.vd_mosaic,
                    "🖼️",
                    context.getString(R.string.create_collage),
                    launchPi
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

    actual fun removeNotificationByTag(tag: String) {
        notificationManager.cancel(tag, 0)
    }

    @SuppressLint("RestrictedApi")
    private fun NotificationCompat.Builder.buildWithMediaStyleMod(): Notification {
        val modNeeded =
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && !mActions.isNullOrEmpty()
        if (modNeeded) {
            if (notiIconBitmap == null || notiIconBitmap?.isRecycled == true) {
                notiIconBitmap =
                    AppCompatResources.getDrawable(context, R.mipmap.ic_launcher)
                        ?.toBitmap()
            }
//                icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
            setLargeIcon(notiIconBitmap)
        }
        return build()
    }
}