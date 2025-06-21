package com.arn.scrobble.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.Stuff

class PlayingTrackEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action != BROADCAST_PLAYING_TRACK_EVENT) return

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
        const val EXTRA_EVENT = "event"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val BROADCAST_PLAYING_TRACK_EVENT = "com.arn.scrobble.PLAYING_TRACK_EVENT"
    }
}