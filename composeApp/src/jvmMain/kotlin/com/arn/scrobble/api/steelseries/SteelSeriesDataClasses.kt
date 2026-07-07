package com.arn.scrobble.api.steelseries

import kotlinx.serialization.Serializable

@Serializable
data class SteelSeriesSeverAddress(
    val address: String,
)

@Serializable
data class SteelSeriesGameEvent(
    val data: SteelSeriesData,
    val event: String,
    val game: String
)

@Serializable
data class SteelSeriesData(
    val frame: SteelSeriesFrame,
    val value: Int
)

@Serializable
data class SteelSeriesFrame(
    val album: String? = null,
    val artist: String,
//    val duration: Int,
    val imageUrl: String? = null,
    val state: String,
    val time: Int,
    val title: String,
//    val url: String
)