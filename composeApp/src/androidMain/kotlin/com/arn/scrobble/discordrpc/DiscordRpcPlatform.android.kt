package com.arn.scrobble.discordrpc

import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff


actual object DiscordRpcPlatform {
    private val discordRpc = DiscordRpcAndroid(
        context = AndroidStuff.applicationContext,
        applicationId = Stuff.DISCORD_CLIENT_ID.toLong(),
        json = Stuff.myJson
    )

    /**
     * Activity: true only once Discord acknowledged it. While the backoff gate in [DiscordRpcAndroid]
     * is closed this returns false immediately without touching Discord, so the caller's retry loop
     * is free to poll. Clear / Stop never connect and always return true.
     */
    actual suspend fun update(activity: DiscordActivity): Boolean {
        return when (activity) {
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
                discordRpc.clearActivity(disconnect = false)
                true
            }

            is DiscordActivity.Stop -> {
                discordRpc.clearActivity(disconnect = true)
                true
            }
        }
    }
}
