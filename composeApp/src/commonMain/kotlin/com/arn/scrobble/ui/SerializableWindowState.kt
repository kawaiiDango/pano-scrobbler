package com.arn.scrobble.ui

import kotlinx.serialization.Serializable

@Serializable
data class SerializableWindowState(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isMaximized: Boolean,
)