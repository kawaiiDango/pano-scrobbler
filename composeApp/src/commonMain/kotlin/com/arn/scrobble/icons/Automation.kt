package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Automation: ImageVector
    get() {
        if (_Automation != null) {
            return _Automation!!
        }
        _Automation = ImageVector.Builder(
            name = "Automation",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(296f, 690f)
                quadToRelative(-42f, 35f, -87.5f, 32f)
                reflectiveQuadTo(129f, 691f)
                quadToRelative(-34f, -28f, -46.5f, -73.5f)
                reflectiveQuadTo(99f, 524f)
                lineToRelative(75f, -124f)
                quadToRelative(-25f, -22f, -39.5f, -53f)
                reflectiveQuadTo(120f, 280f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 66f, -47f, 113f)
                reflectiveQuadToRelative(-113f, 47f)
                quadToRelative(-9f, 0f, -18f, -1f)
                reflectiveQuadToRelative(-17f, -3f)
                lineToRelative(-77f, 130f)
                quadToRelative(-11f, 18f, -7f, 35.5f)
                reflectiveQuadToRelative(17f, 28.5f)
                quadToRelative(13f, 11f, 31f, 12.5f)
                reflectiveQuadToRelative(35f, -12.5f)
                lineToRelative(420f, -361f)
                quadToRelative(42f, -35f, 88f, -31.5f)
                reflectiveQuadToRelative(80f, 31.5f)
                quadToRelative(34f, 28f, 46f, 73.5f)
                reflectiveQuadTo(861f, 436f)
                lineToRelative(-75f, 124f)
                quadToRelative(25f, 22f, 39.5f, 53f)
                reflectiveQuadToRelative(14.5f, 67f)
                quadToRelative(0f, 66f, -47f, 113f)
                reflectiveQuadToRelative(-113f, 47f)
                quadToRelative(-66f, 0f, -113f, -47f)
                reflectiveQuadToRelative(-47f, -113f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(9f, 0f, 17.5f, 1f)
                reflectiveQuadToRelative(16.5f, 3f)
                lineToRelative(78f, -130f)
                quadToRelative(11f, -18f, 7f, -35.5f)
                reflectiveQuadTo(782f, 330f)
                quadToRelative(-13f, -11f, -31f, -12.5f)
                reflectiveQuadTo(716f, 330f)
                lineTo(296f, 690f)
                close()
                moveTo(336.5f, 336.5f)
                quadTo(360f, 313f, 360f, 280f)
                reflectiveQuadToRelative(-23.5f, -56.5f)
                quadTo(313f, 200f, 280f, 200f)
                reflectiveQuadToRelative(-56.5f, 23.5f)
                quadTo(200f, 247f, 200f, 280f)
                reflectiveQuadToRelative(23.5f, 56.5f)
                quadTo(247f, 360f, 280f, 360f)
                reflectiveQuadToRelative(56.5f, -23.5f)
                close()
                moveTo(736.5f, 736.5f)
                quadTo(760f, 713f, 760f, 680f)
                reflectiveQuadToRelative(-23.5f, -56.5f)
                quadTo(713f, 600f, 680f, 600f)
                reflectiveQuadToRelative(-56.5f, 23.5f)
                quadTo(600f, 647f, 600f, 680f)
                reflectiveQuadToRelative(23.5f, 56.5f)
                quadTo(647f, 760f, 680f, 760f)
                reflectiveQuadToRelative(56.5f, -23.5f)
                close()
                moveTo(280f, 280f)
                close()
                moveTo(680f, 680f)
                close()
            }
        }.build()

        return _Automation!!
    }

@Suppress("ObjectPropertyName")
private var _Automation: ImageVector? = null
