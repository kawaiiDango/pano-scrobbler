package com.arn.scrobble.discordrpc

import com.arn.scrobble.PanoNativeComponents

actual object DiscordRpcPlatform {
    actual fun update(activity: DiscordActivity): Boolean =
        when (activity) {
            is DiscordActivity.Activity -> {
                val now = System.currentTimeMillis()
                val (startTimeMillis, endTimeMillis) = activity.timestampsMillis(now)

                PanoNativeComponents.updateDiscordActivity(
                    clientId = activity.discordClientId,
                    name = activity.name,
                    state = activity.state,
                    details = activity.details,
                    largeText = activity.largeText,
                    startTime = startTimeMillis / 1000,
                    endTime = endTimeMillis / 1000,
                    artUrl = activity.artUrl,
                    detailsUrl = activity.detailsUrl,
                    isPlaying = activity.isPlaying,
                    statusLine = activity.statusLine,
                    buttonText = activity.buttonText,
                    buttonUrl = activity.buttonUrl,
                )

            }

            is DiscordActivity.Clear -> {
                PanoNativeComponents.clearDiscordActivity(false)
            }

            is DiscordActivity.Stop -> {
                PanoNativeComponents.clearDiscordActivity(true)
            }
        }
}