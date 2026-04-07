package com.arn.scrobble.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.Stuff

class PlayingTrackEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val eventStr = intent.getStringExtra(EXTRA_EVENT)
        val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE)

        if (eventStr == null || eventType == null) return

        val event = when (eventType) {
            PlayingTrackNotifyEvent.TrackCancelled::class.simpleName -> {
                eventStr.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.TrackCancelled>(it)
                }
            }

            PlayingTrackNotifyEvent.TrackLovedUnloved::class.simpleName -> {
                eventStr.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.TrackLovedUnloved>(it)
                }
            }

            PlayingTrackNotifyEvent.AppAllowedBlocked::class.simpleName -> {
                eventStr.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.AppAllowedBlocked>(it)
                }
            }

            PlayingTrackNotifyEvent.TrackScrobbleLocked::class.simpleName -> {
                eventStr.let {
                    Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.TrackScrobbleLocked>(it)
                }
            }

            else -> {
                Logger.e {
                    "Unknown PlayingTrackNotifyEvent type: $eventType, eventStr: $eventStr"
                }
                return
            }
        }

        notifyPlayingTrackEvent(event)
    }

    companion object {
        private const val EXTRA_EVENT = "event"
        private const val EXTRA_EVENT_TYPE = "event_type"

        fun createIntent(context: Context, event: PlayingTrackNotifyEvent): Intent =
            Intent(context, PlayingTrackEventReceiver::class.java)
                .putExtra(
                    EXTRA_EVENT,
                    Stuff.myJson.encodeToString(event)
                )
                .putExtra(
                    EXTRA_EVENT_TYPE,
                    event::class.simpleName
                )
    }
}