package com.arn.scrobble.main

import kotlinx.serialization.Serializable

sealed interface ScrobblerState {

    @Serializable
    data object Unknown : ScrobblerState

    @Serializable
    data object NLSDisabled : ScrobblerState

    @Serializable
    data object Disabled : ScrobblerState

    @Serializable
    data class Killed(val reason: KilledReason?) :
        ScrobblerState

    @Serializable
    data object Running : ScrobblerState

    @Serializable

    data class KilledReason(
        val reasonCode: Int,
        val reason: String,
        val subReason: String,
        val desc: String,
        val pssMb: Int,
        val isProbablySystemKill: Boolean
    ) {
        fun formatted() = "$desc ($reason) $subReason".trimEnd()
    }
}