package com.arn.scrobble.pref

import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random


expect class Mdns(
    namePrefix: String = "pano-scrobbler-",
) {
    val status: StateFlow<MdnsStatus>
    fun register(
        ip: String,
        port: Int,
        suffix: String = Random.nextBytes(2).toHexString(HexFormat.UpperCase)
    )

    fun discover()
    fun stop()
}

sealed interface MdnsStatus {
    data class ServiceInfo(val name: String, val ip: String, val port: Int)

    data object None : MdnsStatus

    sealed interface Registration : MdnsStatus
    data class Registered(val name: String) : Registration
    data object RegistrationError : Registration
    data object UnregistrationError : Registration

    sealed interface Discovery : MdnsStatus
    data object Discovering : Discovery
    data object DiscoveryError : Discovery
    data class Discovered(val services: List<ServiceInfo>) : Discovery
    data class DiscoveredAndConfirmed(val service: ServiceInfo) : Discovery
}