package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.ToggleOff: ImageVector
    get() {
        if (_ToggleOff != null) {
            return _ToggleOff!!
        }
        _ToggleOff = ImageVector.Builder(
            name = "ToggleOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(280f, 720f)
                quadToRelative(-100f, 0f, -170f, -70f)
                reflectiveQuadTo(40f, 480f)
                quadToRelative(0f, -100f, 70f, -170f)
                reflectiveQuadToRelative(170f, -70f)
                horizontalLineToRelative(400f)
                quadToRelative(100f, 0f, 170f, 70f)
                reflectiveQuadToRelative(70f, 170f)
                quadToRelative(0f, 100f, -70f, 170f)
                reflectiveQuadToRelative(-170f, 70f)
                lineTo(280f, 720f)
                close()
                moveTo(280f, 640f)
                horizontalLineToRelative(400f)
                quadToRelative(66f, 0f, 113f, -47f)
                reflectiveQuadToRelative(47f, -113f)
                quadToRelative(0f, -66f, -47f, -113f)
                reflectiveQuadToRelative(-113f, -47f)
                lineTo(280f, 320f)
                quadToRelative(-66f, 0f, -113f, 47f)
                reflectiveQuadToRelative(-47f, 113f)
                quadToRelative(0f, 66f, 47f, 113f)
                reflectiveQuadToRelative(113f, 47f)
                close()
                moveTo(365f, 565f)
                quadToRelative(35f, -35f, 35f, -85f)
                reflectiveQuadToRelative(-35f, -85f)
                quadToRelative(-35f, -35f, -85f, -35f)
                reflectiveQuadToRelative(-85f, 35f)
                quadToRelative(-35f, 35f, -35f, 85f)
                reflectiveQuadToRelative(35f, 85f)
                quadToRelative(35f, 35f, 85f, 35f)
                reflectiveQuadToRelative(85f, -35f)
                close()
                moveTo(480f, 480f)
                close()
            }
        }.build()

        return _ToggleOff!!
    }

@Suppress("ObjectPropertyName")
private var _ToggleOff: ImageVector? = null
