package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Mic: ImageVector
    get() {
        if (_Mic != null) {
            return _Mic!!
        }
        _Mic = ImageVector.Builder(
            name = "Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(395f, 525f)
                quadToRelative(-35f, -35f, -35f, -85f)
                verticalLineToRelative(-240f)
                quadToRelative(0f, -50f, 35f, -85f)
                reflectiveQuadToRelative(85f, -35f)
                quadToRelative(50f, 0f, 85f, 35f)
                reflectiveQuadToRelative(35f, 85f)
                verticalLineToRelative(240f)
                quadToRelative(0f, 50f, -35f, 85f)
                reflectiveQuadToRelative(-85f, 35f)
                quadToRelative(-50f, 0f, -85f, -35f)
                close()
                moveTo(480f, 320f)
                close()
                moveTo(440f, 800f)
                verticalLineToRelative(-83f)
                quadToRelative(-92f, -13f, -157.5f, -78f)
                reflectiveQuadTo(203f, 481f)
                quadToRelative(-2f, -17f, 9f, -29f)
                reflectiveQuadToRelative(28f, -12f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(284f, 480f)
                quadToRelative(14f, 70f, 69.5f, 115f)
                reflectiveQuadTo(480f, 640f)
                quadToRelative(72f, 0f, 127f, -45.5f)
                reflectiveQuadTo(676f, 480f)
                quadToRelative(4f, -17f, 15.5f, -28.5f)
                reflectiveQuadTo(720f, 440f)
                quadToRelative(17f, 0f, 28f, 12f)
                reflectiveQuadToRelative(9f, 29f)
                quadToRelative(-14f, 91f, -79f, 157f)
                reflectiveQuadToRelative(-158f, 79f)
                verticalLineToRelative(83f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(480f, 840f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(440f, 800f)
                close()
                moveTo(508.5f, 468.5f)
                quadTo(520f, 457f, 520f, 440f)
                verticalLineToRelative(-240f)
                quadToRelative(0f, -17f, -11.5f, -28.5f)
                reflectiveQuadTo(480f, 160f)
                quadToRelative(-17f, 0f, -28.5f, 11.5f)
                reflectiveQuadTo(440f, 200f)
                verticalLineToRelative(240f)
                quadToRelative(0f, 17f, 11.5f, 28.5f)
                reflectiveQuadTo(480f, 480f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                close()
            }
        }.build()

        return _Mic!!
    }

@Suppress("ObjectPropertyName")
private var _Mic: ImageVector? = null
