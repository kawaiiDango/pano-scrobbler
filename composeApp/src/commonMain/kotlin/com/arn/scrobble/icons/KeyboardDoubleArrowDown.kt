package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.KeyboardDoubleArrowDown: ImageVector
    get() {
        if (_KeyboardDoubleArrowDown != null) {
            return _KeyboardDoubleArrowDown!!
        }
        _KeyboardDoubleArrowDown = ImageVector.Builder(
            name = "KeyboardDoubleArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(480f, 647f)
                lineToRelative(156f, -155f)
                quadToRelative(11f, -11f, 27.5f, -11.5f)
                reflectiveQuadTo(692f, 492f)
                quadToRelative(11f, 11f, 11f, 28f)
                reflectiveQuadToRelative(-11f, 28f)
                lineTo(508f, 732f)
                quadToRelative(-6f, 6f, -13f, 8.5f)
                reflectiveQuadToRelative(-15f, 2.5f)
                quadToRelative(-8f, 0f, -15f, -2.5f)
                reflectiveQuadToRelative(-13f, -8.5f)
                lineTo(268f, 548f)
                quadToRelative(-11f, -11f, -11.5f, -27.5f)
                reflectiveQuadTo(268f, 492f)
                quadToRelative(11f, -11f, 28f, -11f)
                reflectiveQuadToRelative(28f, 11f)
                lineToRelative(156f, 155f)
                close()
                moveTo(480f, 407f)
                lineTo(636f, 252f)
                quadToRelative(11f, -11f, 27.5f, -11.5f)
                reflectiveQuadTo(692f, 252f)
                quadToRelative(11f, 11f, 11f, 28f)
                reflectiveQuadToRelative(-11f, 28f)
                lineTo(508f, 492f)
                quadToRelative(-6f, 6f, -13f, 8.5f)
                reflectiveQuadToRelative(-15f, 2.5f)
                quadToRelative(-8f, 0f, -15f, -2.5f)
                reflectiveQuadToRelative(-13f, -8.5f)
                lineTo(268f, 308f)
                quadToRelative(-11f, -11f, -11.5f, -27.5f)
                reflectiveQuadTo(268f, 252f)
                quadToRelative(11f, -11f, 28f, -11f)
                reflectiveQuadToRelative(28f, 11f)
                lineToRelative(156f, 155f)
                close()
            }
        }.build()

        return _KeyboardDoubleArrowDown!!
    }

@Suppress("ObjectPropertyName")
private var _KeyboardDoubleArrowDown: ImageVector? = null
