package com.arn.scrobble.discordrpc

expect object DiscordRpcPlatform {
    suspend fun update(activity: DiscordActivity): Boolean
}