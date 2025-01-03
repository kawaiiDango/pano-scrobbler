package com.arn.scrobble.charts

data class ChartsLoaderInput(
    val username: String,
    val timePeriod: TimePeriod,
    val prevPeriod: TimePeriod?,
    val firstPageOnly: Boolean,
)