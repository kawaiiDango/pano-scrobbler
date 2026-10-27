package com.arn.scrobble.discordrpc

expect object DiscordRpcPlatform {
    fun update(activity: DiscordActivity): Boolean
}