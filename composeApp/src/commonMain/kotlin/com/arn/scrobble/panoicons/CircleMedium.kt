package com.arn.scrobble.panoicons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp


val PanoIcons.CircleMedium: ImageVector
    get() {
        if (_CircleMedium != null) {
            return _CircleMedium!!
        }
        _CircleMedium = ImageVector.Builder(
            name = "CircleMedium",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8f, 12f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 16f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, 12f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 8f)
                close()
            }
        }.build()

        return _CircleMedium!!
    }

@Suppress("ObjectPropertyName")
private var _CircleMedium: ImageVector? = null
