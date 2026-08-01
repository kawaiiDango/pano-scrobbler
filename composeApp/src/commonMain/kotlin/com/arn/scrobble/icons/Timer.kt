package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Timer: ImageVector
    get() {
        if (_Timer != null) {
            return _Timer!!
        }
        _Timer = ImageVector.Builder(
            name = "Timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(400f, 120f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(360f, 80f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(400f, 40f)
                horizontalLineToRelative(160f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(600f, 80f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(560f, 120f)
                lineTo(400f, 120f)
                close()
                moveTo(508.5f, 548.5f)
                quadTo(520f, 537f, 520f, 520f)
                verticalLineToRelative(-160f)
                quadToRelative(0f, -17f, -11.5f, -28.5f)
                reflectiveQuadTo(480f, 320f)
                quadToRelative(-17f, 0f, -28.5f, 11.5f)
                reflectiveQuadTo(440f, 360f)
                verticalLineToRelative(160f)
                quadToRelative(0f, 17f, 11.5f, 28.5f)
                reflectiveQuadTo(480f, 560f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                close()
                moveTo(340.5f, 851.5f)
                quadTo(275f, 823f, 226f, 774f)
                reflectiveQuadToRelative(-77.5f, -114.5f)
                quadTo(120f, 594f, 120f, 520f)
                reflectiveQuadToRelative(28.5f, -139.5f)
                quadTo(177f, 315f, 226f, 266f)
                reflectiveQuadToRelative(114.5f, -77.5f)
                quadTo(406f, 160f, 480f, 160f)
                quadToRelative(62f, 0f, 119f, 20f)
                reflectiveQuadToRelative(107f, 58f)
                lineToRelative(28f, -28f)
                quadToRelative(11f, -11f, 28f, -11f)
                reflectiveQuadToRelative(28f, 11f)
                quadToRelative(11f, 11f, 11f, 28f)
                reflectiveQuadToRelative(-11f, 28f)
                lineToRelative(-28f, 28f)
                quadToRelative(38f, 50f, 58f, 107f)
                reflectiveQuadToRelative(20f, 119f)
                quadToRelative(0f, 74f, -28.5f, 139.5f)
                reflectiveQuadTo(734f, 774f)
                quadToRelative(-49f, 49f, -114.5f, 77.5f)
                reflectiveQuadTo(480f, 880f)
                quadToRelative(-74f, 0f, -139.5f, -28.5f)
                close()
                moveTo(678f, 718f)
                quadToRelative(82f, -82f, 82f, -198f)
                reflectiveQuadToRelative(-82f, -198f)
                quadToRelative(-82f, -82f, -198f, -82f)
                reflectiveQuadToRelative(-198f, 82f)
                quadToRelative(-82f, 82f, -82f, 198f)
                reflectiveQuadToRelative(82f, 198f)
                quadToRelative(82f, 82f, 198f, 82f)
                reflectiveQuadToRelative(198f, -82f)
                close()
                moveTo(480f, 520f)
                close()
            }
        }.build()

        return _Timer!!
    }

@Suppress("ObjectPropertyName")
private var _Timer: ImageVector? = null
