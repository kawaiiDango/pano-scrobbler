package com.arn.scrobble.help

import kotlinx.serialization.Serializable

@Serializable
data class FaqEntity(
    val type: FaqEntityType? = null, // null in case I add a new type later
    val start: Int,     // inclusive
    val end: Int,       // exclusive
    val url: String? = null
)

enum class FaqEntityType {
    bold, code, link
}

enum class FaqPlatform {
    android, desktop, tv
}

@Serializable
data class FaqItem(
    val question: String,
    val answer: String,
    val platform: FaqPlatform? = null,
    val entities: List<FaqEntity> = emptyList()
)