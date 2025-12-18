package com.arn.scrobble.panoicons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

val PanoIcons.Nothing: ImageVector
    get() {
        if (_Nothing != null) {
            return _Nothing!!
        }
        _Nothing = ImageVector.Builder(
            name = "Nothing",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).build()
        return _Nothing!!
    }

private var _Nothing: ImageVector? = null
