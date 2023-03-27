package com.arn.scrobble

import kotlinx.serialization.Serializable

@Serializable
data class DrawerData(
    val scrobblesTotal: Int,
    val scrobblesToday: Int = -1,
    val artistCount: Int = -1,
    val albumCount: Int = -1,
    val trackCount: Int = -1,
    val profilePicUrl: String? = null,
)