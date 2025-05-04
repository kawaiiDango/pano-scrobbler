package com.arn.scrobble.media

import android.content.Intent
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff


actual fun notifyPlayingTrackEventWithIpc(event: PlayingTrackNotifyEvent) {
    val context = AndroidStuff.application

    when (event) {
        is PlayingTrackNotifyEvent.TrackScrobbleLocked -> {
            if (event.hash == -1) return

            // do not scrobble until the dialog is dismissed

            val intent = Intent(NLService.iSCROBBLE_SUBMIT_LOCK)
                .setPackage(context.packageName)
                .putExtra(
                    Stuff.EXTRA_EVENT,
                    Stuff.myJson.encodeToString(event)
                )

            context.sendBroadcast(intent, NLService.BROADCAST_PERMISSION)

            // also cancel the notification on unlock
            if (!event.locked) {
                val cancelEvent = PlayingTrackNotifyEvent.TrackCancelled(
                    hash = event.hash,
                    showUnscrobbledNotification = false,
                    markAsScrobbled = false
                )

                context.sendBroadcast(
                    Intent(NLService.iCANCEL)
                        .setPackage(context.packageName)
                        .putExtra(Stuff.EXTRA_EVENT, Stuff.myJson.encodeToString(cancelEvent)),
                    NLService.BROADCAST_PERMISSION
                )
            }
        }

        is PlayingTrackNotifyEvent.TrackCancelled -> {
            val intent = Intent(NLService.iCANCEL)
                .setPackage(context.packageName)
                .putExtra(
                    Stuff.EXTRA_EVENT,
                    Stuff.myJson.encodeToString(event)
                )

            context.sendBroadcast(intent, NLService.BROADCAST_PERMISSION)
        }

        is PlayingTrackNotifyEvent.TrackLovedUnloved -> {
            val intent = Intent(
                if (event.loved) NLService.iLOVE
                else NLService.iUNLOVE
            )
                .setPackage(context.packageName)
                .putExtra(
                    Stuff.EXTRA_EVENT,
                    Stuff.myJson.encodeToString(event)
                )

            context.sendBroadcast(intent, NLService.BROADCAST_PERMISSION)
        }

        is PlayingTrackNotifyEvent.AppAllowedBlocked -> {
            val intent = Intent(NLService.iAPP_ALLOWED_BLOCKED)
                .setPackage(context.packageName)
                .putExtra(
                    Stuff.EXTRA_EVENT,
                    Stuff.myJson.encodeToString(event)
                )

            context.sendBroadcast(intent, NLService.BROADCAST_PERMISSION)
        }

        else -> {
            // Do nothing
        }
    }
}