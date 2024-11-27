package com.arn.scrobble.charts

data class RandomLoaderInput(
    val username: String,
    val timePeriod: TimePeriod,
    val type: Int,
)