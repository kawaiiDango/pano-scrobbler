package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.SwapVert: ImageVector
    get() {
        if (_SwapVert != null) {
            return _SwapVert!!
        }
        _SwapVert = ImageVector.Builder(
            name = "SwapVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(331.5f, 508.5f)
                quadTo(320f, 497f, 320f, 480f)
                verticalLineToRelative(-247f)
                lineToRelative(-75f, 75f)
                quadToRelative(-11f, 11f, -27.5f, 11f)
                reflectiveQuadTo(189f, 308f)
                quadToRelative(-12f, -12f, -12f, -28.5f)
                reflectiveQuadToRelative(12f, -28.5f)
                lineToRelative(143f, -143f)
                quadToRelative(6f, -6f, 13f, -8.5f)
                reflectiveQuadToRelative(15f, -2.5f)
                quadToRelative(8f, 0f, 15f, 2.5f)
                reflectiveQuadToRelative(13f, 8.5f)
                lineToRelative(144f, 144f)
                quadToRelative(12f, 12f, 11.5f, 28f)
                reflectiveQuadTo(531f, 308f)
                quadToRelative(-12f, 11f, -28f, 11.5f)
                reflectiveQuadTo(475f, 308f)
                lineToRelative(-75f, -75f)
                verticalLineToRelative(247f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(360f, 520f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                close()
                moveTo(585f, 860.5f)
                quadToRelative(-7f, -2.5f, -13f, -8.5f)
                lineTo(428f, 708f)
                quadToRelative(-12f, -12f, -11.5f, -28f)
                reflectiveQuadToRelative(12.5f, -28f)
                quadToRelative(12f, -11f, 28f, -11.5f)
                reflectiveQuadToRelative(28f, 11.5f)
                lineToRelative(75f, 75f)
                verticalLineToRelative(-247f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(600f, 440f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(640f, 480f)
                verticalLineToRelative(247f)
                lineToRelative(75f, -75f)
                quadToRelative(11f, -11f, 27.5f, -11f)
                reflectiveQuadToRelative(28.5f, 11f)
                quadToRelative(12f, 12f, 12f, 28.5f)
                reflectiveQuadTo(771f, 709f)
                lineTo(628f, 852f)
                quadToRelative(-6f, 6f, -13f, 8.5f)
                reflectiveQuadTo(600f, 863f)
                quadToRelative(-8f, 0f, -15f, -2.5f)
                close()
            }
        }.build()

        return _SwapVert!!
    }

@Suppress("ObjectPropertyName")
private var _SwapVert: ImageVector? = null
