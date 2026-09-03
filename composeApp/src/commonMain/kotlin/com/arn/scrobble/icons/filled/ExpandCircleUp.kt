package com.arn.scrobble.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons

val Icons.Filled.ExpandCircleUp: ImageVector
    get() {
        if (_ExpandCircleUp != null) {
            return _ExpandCircleUp!!
        }
        _ExpandCircleUp = ImageVector.Builder(
            name = "Filled.ExpandCircleUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(480f, 453f)
                lineToRelative(95f, 95f)
                quadToRelative(11f, 11f, 27.5f, 11f)
                reflectiveQuadToRelative(28.5f, -11f)
                quadToRelative(12f, -12f, 12f, -28.5f)
                reflectiveQuadTo(631f, 491f)
                lineTo(508f, 368f)
                quadToRelative(-12f, -12f, -28f, -12f)
                reflectiveQuadToRelative(-28f, 12f)
                lineTo(328f, 492f)
                quadToRelative(-12f, 12f, -11.5f, 28f)
                reflectiveQuadToRelative(12.5f, 28f)
                quadToRelative(12f, 11f, 28f, 11.5f)
                reflectiveQuadToRelative(28f, -11.5f)
                lineToRelative(95f, -95f)
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

        return _ExpandCircleUp!!
    }

@Suppress("ObjectPropertyName")
private var _ExpandCircleUp: ImageVector? = null
