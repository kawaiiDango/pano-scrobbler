package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.BarChart4Bars: ImageVector
    get() {
        if (_BarChart4Bars != null) {
            return _BarChart4Bars!!
        }
        _BarChart4Bars = ImageVector.Builder(
            name = "BarChart4Bars",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(120f, 840f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(80f, 800f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(120f, 760f)
                horizontalLineToRelative(720f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(880f, 800f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(840f, 840f)
                lineTo(120f, 840f)
                close()
                moveTo(137.5f, 702.5f)
                quadTo(120f, 685f, 120f, 660f)
                verticalLineToRelative(-160f)
                quadToRelative(0f, -25f, 17.5f, -42.5f)
                reflectiveQuadTo(180f, 440f)
                quadToRelative(25f, 0f, 42.5f, 17.5f)
                reflectiveQuadTo(240f, 500f)
                verticalLineToRelative(160f)
                quadToRelative(0f, 25f, -17.5f, 42.5f)
                reflectiveQuadTo(180f, 720f)
                quadToRelative(-25f, 0f, -42.5f, -17.5f)
                close()
                moveTo(337.5f, 702.5f)
                quadTo(320f, 685f, 320f, 660f)
                verticalLineToRelative(-360f)
                quadToRelative(0f, -25f, 17.5f, -42.5f)
                reflectiveQuadTo(380f, 240f)
                quadToRelative(25f, 0f, 42.5f, 17.5f)
                reflectiveQuadTo(440f, 300f)
                verticalLineToRelative(360f)
                quadToRelative(0f, 25f, -17.5f, 42.5f)
                reflectiveQuadTo(380f, 720f)
                quadToRelative(-25f, 0f, -42.5f, -17.5f)
                close()
                moveTo(537.5f, 702.5f)
                quadTo(520f, 685f, 520f, 660f)
                verticalLineToRelative(-240f)
                quadToRelative(0f, -25f, 17.5f, -42.5f)
                reflectiveQuadTo(580f, 360f)
                quadToRelative(25f, 0f, 42.5f, 17.5f)
                reflectiveQuadTo(640f, 420f)
                verticalLineToRelative(240f)
                quadToRelative(0f, 25f, -17.5f, 42.5f)
                reflectiveQuadTo(580f, 720f)
                quadToRelative(-25f, 0f, -42.5f, -17.5f)
                close()
                moveTo(737.5f, 702.5f)
                quadTo(720f, 685f, 720f, 660f)
                verticalLineToRelative(-480f)
                quadToRelative(0f, -25f, 17.5f, -42.5f)
                reflectiveQuadTo(780f, 120f)
                quadToRelative(25f, 0f, 42.5f, 17.5f)
                reflectiveQuadTo(840f, 180f)
                verticalLineToRelative(480f)
                quadToRelative(0f, 25f, -17.5f, 42.5f)
                reflectiveQuadTo(780f, 720f)
                quadToRelative(-25f, 0f, -42.5f, -17.5f)
                close()
            }
        }.build()

        return _BarChart4Bars!!
    }

@Suppress("ObjectPropertyName")
private var _BarChart4Bars: ImageVector? = null
