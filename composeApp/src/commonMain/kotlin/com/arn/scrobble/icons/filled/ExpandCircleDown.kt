package com.arn.scrobble.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons

val Icons.Filled.ExpandCircleDown: ImageVector
    get() {
        if (_ExpandCircleDown != null) {
            return _ExpandCircleDown!!
        }
        _ExpandCircleDown = ImageVector.Builder(
            name = "Filled.ExpandCircleDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(480f, 507f)
                lineToRelative(-95f, -95f)
                quadToRelative(-11f, -11f, -27.5f, -11f)
                reflectiveQuadTo(329f, 412f)
                quadToRelative(-12f, 12f, -12f, 28.5f)
                reflectiveQuadToRelative(12f, 28.5f)
                lineToRelative(123f, 123f)
                quadToRelative(12f, 12f, 28f, 12f)
                reflectiveQuadToRelative(28f, -12f)
                lineToRelative(124f, -124f)
                quadToRelative(12f, -12f, 11.5f, -28f)
                reflectiveQuadTo(631f, 412f)
                quadToRelative(-12f, -11f, -28f, -11.5f)
                reflectiveQuadTo(575f, 412f)
                lineToRelative(-95f, 95f)
                close()
                moveTo(480f, 880f)
                quadToRelative(-83f, 0f, -156f, -31.5f)
                reflectiveQuadTo(197f, 763f)
                quadToRelative(-54f, -54f, -85.5f, -127f)
                reflectiveQuadTo(80f, 480f)
                quadToRelative(0f, -83f, 31.5f, -156f)
                reflectiveQuadTo(197f, 197f)
                quadToRelative(54f, -54f, 127f, -85.5f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(83f, 0f, 156f, 31.5f)
                reflectiveQuadTo(763f, 197f)
                quadToRelative(54f, 54f, 85.5f, 127f)
                reflectiveQuadTo(880f, 480f)
                quadToRelative(0f, 83f, -31.5f, 156f)
                reflectiveQuadTo(763f, 763f)
                quadToRelative(-54f, 54f, -127f, 85.5f)
                reflectiveQuadTo(480f, 880f)
                close()
            }
        }.build()

        return _ExpandCircleDown!!
    }

@Suppress("ObjectPropertyName")
private var _ExpandCircleDown: ImageVector? = null
