package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.SkipNext: ImageVector
    get() {
        if (_SkipNext != null) {
            return _SkipNext!!
        }
        _SkipNext = ImageVector.Builder(
            name = "SkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(660f, 680f)
                verticalLineToRelative(-400f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(700f, 240f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(740f, 280f)
                verticalLineToRelative(400f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(700f, 720f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(660f, 680f)
                close()
                moveTo(220f, 645f)
                verticalLineToRelative(-330f)
                quadToRelative(0f, -18f, 12f, -29f)
                reflectiveQuadToRelative(28f, -11f)
                quadToRelative(5f, 0f, 11f, 1f)
                reflectiveQuadToRelative(11f, 5f)
                lineToRelative(248f, 166f)
                quadToRelative(9f, 6f, 13.5f, 14.5f)
                reflectiveQuadTo(548f, 480f)
                quadToRelative(0f, 10f, -4.5f, 18.5f)
                reflectiveQuadTo(530f, 513f)
                lineTo(282f, 679f)
                quadToRelative(-5f, 4f, -11f, 5f)
                reflectiveQuadToRelative(-11f, 1f)
                quadToRelative(-16f, 0f, -28f, -11f)
                reflectiveQuadToRelative(-12f, -29f)
                close()
                moveTo(300f, 480f)
                close()
                moveTo(300f, 570f)
                lineTo(436f, 480f)
                lineTo(300f, 390f)
                verticalLineToRelative(180f)
                close()
            }
        }.build()

        return _SkipNext!!
    }

@Suppress("ObjectPropertyName")
private var _SkipNext: ImageVector? = null
