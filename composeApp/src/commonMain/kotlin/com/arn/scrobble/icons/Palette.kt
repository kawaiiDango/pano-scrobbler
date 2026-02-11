package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Palette: ImageVector
    get() {
        if (_Palette != null) {
            return _Palette!!
        }
        _Palette = ImageVector.Builder(
            name = "Palette",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(480f, 880f)
                quadToRelative(-82f, 0f, -155f, -31.5f)
                reflectiveQuadToRelative(-127.5f, -86f)
                quadTo(143f, 708f, 111.5f, 635f)
                reflectiveQuadTo(80f, 480f)
                quadToRelative(0f, -83f, 32.5f, -156f)
                reflectiveQuadToRelative(88f, -127f)
                quadTo(256f, 143f, 330f, 111.5f)
                reflectiveQuadTo(488f, 80f)
                quadToRelative(80f, 0f, 151f, 27.5f)
                reflectiveQuadToRelative(124.5f, 76f)
                quadToRelative(53.5f, 48.5f, 85f, 115f)
                reflectiveQuadTo(880f, 442f)
                quadToRelative(0f, 115f, -70f, 176.5f)
                reflectiveQuadTo(640f, 680f)
                horizontalLineToRelative(-74f)
                quadToRelative(-9f, 0f, -12.5f, 5f)
                reflectiveQuadToRelative(-3.5f, 11f)
                quadToRelative(0f, 12f, 15f, 34.5f)
                reflectiveQuadToRelative(15f, 51.5f)
                quadToRelative(0f, 50f, -27.5f, 74f)
                reflectiveQuadTo(480f, 880f)
                close()
                moveTo(480f, 480f)
                close()
                moveTo(303f, 503f)
                quadToRelative(17f, -17f, 17f, -43f)
                reflectiveQuadToRelative(-17f, -43f)
                quadToRelative(-17f, -17f, -43f, -17f)
                reflectiveQuadToRelative(-43f, 17f)
                quadToRelative(-17f, 17f, -17f, 43f)
                reflectiveQuadToRelative(17f, 43f)
                quadToRelative(17f, 17f, 43f, 17f)
                reflectiveQuadToRelative(43f, -17f)
                close()
                moveTo(423f, 343f)
                quadToRelative(17f, -17f, 17f, -43f)
                reflectiveQuadToRelative(-17f, -43f)
                quadToRelative(-17f, -17f, -43f, -17f)
                reflectiveQuadToRelative(-43f, 17f)
                quadToRelative(-17f, 17f, -17f, 43f)
                reflectiveQuadToRelative(17f, 43f)
                quadToRelative(17f, 17f, 43f, 17f)
                reflectiveQuadToRelative(43f, -17f)
                close()
                moveTo(623f, 343f)
                quadToRelative(17f, -17f, 17f, -43f)
                reflectiveQuadToRelative(-17f, -43f)
                quadToRelative(-17f, -17f, -43f, -17f)
                reflectiveQuadToRelative(-43f, 17f)
                quadToRelative(-17f, 17f, -17f, 43f)
                reflectiveQuadToRelative(17f, 43f)
                quadToRelative(17f, 17f, 43f, 17f)
                reflectiveQuadToRelative(43f, -17f)
                close()
                moveTo(743f, 503f)
                quadToRelative(17f, -17f, 17f, -43f)
                reflectiveQuadToRelative(-17f, -43f)
                quadToRelative(-17f, -17f, -43f, -17f)
                reflectiveQuadToRelative(-43f, 17f)
                quadToRelative(-17f, 17f, -17f, 43f)
                reflectiveQuadToRelative(17f, 43f)
                quadToRelative(17f, 17f, 43f, 17f)
                reflectiveQuadToRelative(43f, -17f)
                close()
                moveTo(480f, 800f)
                quadToRelative(9f, 0f, 14.5f, -5f)
                reflectiveQuadToRelative(5.5f, -13f)
                quadToRelative(0f, -14f, -15f, -33f)
                reflectiveQuadToRelative(-15f, -57f)
                quadToRelative(0f, -42f, 29f, -67f)
                reflectiveQuadToRelative(71f, -25f)
                horizontalLineToRelative(70f)
                quadToRelative(66f, 0f, 113f, -38.5f)
                reflectiveQuadTo(800f, 442f)
                quadToRelative(0f, -121f, -92.5f, -201.5f)
                reflectiveQuadTo(488f, 160f)
                quadToRelative(-136f, 0f, -232f, 93f)
                reflectiveQuadToRelative(-96f, 227f)
                quadToRelative(0f, 133f, 93.5f, 226.5f)
                reflectiveQuadTo(480f, 800f)
                close()
            }
        }.build()

        return _Palette!!
    }

@Suppress("ObjectPropertyName")
private var _Palette: ImageVector? = null
