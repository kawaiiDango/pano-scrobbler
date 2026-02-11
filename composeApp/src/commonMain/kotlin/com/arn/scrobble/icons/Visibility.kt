package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Visibility: ImageVector
    get() {
        if (_Visibility != null) {
            return _Visibility!!
        }
        _Visibility = ImageVector.Builder(
            name = "Visibility",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(607.5f, 587.5f)
                quadTo(660f, 535f, 660f, 460f)
                reflectiveQuadToRelative(-52.5f, -127.5f)
                quadTo(555f, 280f, 480f, 280f)
                reflectiveQuadToRelative(-127.5f, 52.5f)
                quadTo(300f, 385f, 300f, 460f)
                reflectiveQuadToRelative(52.5f, 127.5f)
                quadTo(405f, 640f, 480f, 640f)
                reflectiveQuadToRelative(127.5f, -52.5f)
                close()
                moveTo(403.5f, 536.5f)
                quadTo(372f, 505f, 372f, 460f)
                reflectiveQuadToRelative(31.5f, -76.5f)
                quadTo(435f, 352f, 480f, 352f)
                reflectiveQuadToRelative(76.5f, 31.5f)
                quadTo(588f, 415f, 588f, 460f)
                reflectiveQuadToRelative(-31.5f, 76.5f)
                quadTo(525f, 568f, 480f, 568f)
                reflectiveQuadToRelative(-76.5f, -31.5f)
                close()
                moveTo(235.5f, 688f)
                quadTo(125f, 616f, 61f, 498f)
                quadToRelative(-5f, -9f, -7.5f, -18.5f)
                reflectiveQuadTo(51f, 460f)
                quadToRelative(0f, -10f, 2.5f, -19.5f)
                reflectiveQuadTo(61f, 422f)
                quadToRelative(64f, -118f, 174.5f, -190f)
                reflectiveQuadTo(480f, 160f)
                quadToRelative(134f, 0f, 244.5f, 72f)
                reflectiveQuadTo(899f, 422f)
                quadToRelative(5f, 9f, 7.5f, 18.5f)
                reflectiveQuadTo(909f, 460f)
                quadToRelative(0f, 10f, -2.5f, 19.5f)
                reflectiveQuadTo(899f, 498f)
                quadToRelative(-64f, 118f, -174.5f, 190f)
                reflectiveQuadTo(480f, 760f)
                quadToRelative(-134f, 0f, -244.5f, -72f)
                close()
                moveTo(480f, 460f)
                close()
                moveTo(687.5f, 620.5f)
                quadTo(782f, 561f, 832f, 460f)
                quadToRelative(-50f, -101f, -144.5f, -160.5f)
                reflectiveQuadTo(480f, 240f)
                quadToRelative(-113f, 0f, -207.5f, 59.5f)
                reflectiveQuadTo(128f, 460f)
                quadToRelative(50f, 101f, 144.5f, 160.5f)
                reflectiveQuadTo(480f, 680f)
                quadToRelative(113f, 0f, 207.5f, -59.5f)
                close()
            }
        }.build()

        return _Visibility!!
    }

@Suppress("ObjectPropertyName")
private var _Visibility: ImageVector? = null
