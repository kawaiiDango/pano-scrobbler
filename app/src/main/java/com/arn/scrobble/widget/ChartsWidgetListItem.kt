package com.arn.scrobble.widget


@kotlinx.serialization.Serializable
data class ChartsWidgetListItem(
    val title: String,
    val subtitle: String? = null,
    val number: Int,
    val imageUrl: String? = null,
    val stonksDelta: Int? = null,
)