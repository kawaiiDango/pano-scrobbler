package com.arn.scrobble.widget


@kotlinx.serialization.Serializable
data class ChartsWidgetListItem(
    val title: String,
    val subtitle: String,
    val number: Int,
    val imageUrl: String = "",
    val stonksDelta: Int? = null,
)