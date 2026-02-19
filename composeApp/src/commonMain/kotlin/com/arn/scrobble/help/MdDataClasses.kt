package com.arn.scrobble.help

import kotlinx.serialization.Serializable

@Serializable
data class MdEntity(
    val type: MdEntityType? = null, // null in case I add a new type later
    val start: Int,     // inclusive
    val end: Int,       // exclusive
    val url: String? = null
)

enum class MdEntityType {
    bold, italic, code, link
}

enum class MdTag {
    android, desktop, tv, nonplay
}

@Serializable
data class MdItem(
    val header: String,
    val content: String,
    val tag: MdTag? = null,
    val entities: List<MdEntity> = emptyList()
)