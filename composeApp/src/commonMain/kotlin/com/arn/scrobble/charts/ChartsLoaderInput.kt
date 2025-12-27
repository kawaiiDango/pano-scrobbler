package com.arn.scrobble.charts

data class ChartsLoaderInput(
    val timePeriod: TimePeriod,
    val prevPeriod: TimePeriod?,
    val refreshCount: Int,
)