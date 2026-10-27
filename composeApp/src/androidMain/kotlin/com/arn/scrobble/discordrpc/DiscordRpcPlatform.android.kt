package com.arn.scrobble.discordrpc

import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff


actual object DiscordRpcPlatform {
    private val discordRpc = DiscordRpcAndroid(
        context = AndroidStuff.applicationContext,
        applicationId = Stuff.DISCORD_CLIENT_ID.toLong(),
        json = Stuff.myJson
    )

    actual fun update(activity: DiscordActivity): Boolean {
        // todo return and handle errors
        // todo do not retry too often when connection fails (e.g. discord is not installed, user logged out)

        when (activity) {
            is DiscordActivity.Activity -> {
                val now = System.currentTimeMillis()
                val (startTimeMillis, endTimeMillis) = activity.timestampsMillis(now)

                discordRpc.setActivity(
                    name = activity.name,
                    state = activity.state,
                    details = activity.details,
                    largeText = activity.largeText,
                    startTimeMillis = startTimeMillis,
                    endTimeMillis = endTimeMillis,
                    artUrl = activity.artUrl,
                    detailsUrl = activity.detailsUrl,
                    isPlaying = activity.isPlaying,
                    statusLine = activity.statusLine,
                    buttonText = activity.buttonText,
                    buttonUrl = activity.buttonUrl,
                )
            }

            is DiscordActivity.Clear -> {
                discordRpc.clearActivity(false)
            }

            is DiscordActivity.Stop -> {
                discordRpc.clearActivity(true)
            }
        }

        return true
    }
}