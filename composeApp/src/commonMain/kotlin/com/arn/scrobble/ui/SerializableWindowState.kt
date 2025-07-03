package com.arn.scrobble.ui

import kotlinx.serialization.Serializable

@Serializable
data class SerializableWindowState(
    val width: Float,
    val height: Float,
    val isMaximized: Boolean,
)