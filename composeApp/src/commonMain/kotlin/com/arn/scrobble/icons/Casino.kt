package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Casino: ImageVector
    get() {
        if (_Casino != null) {
            return _Casino!!
        }
        _Casino = ImageVector.Builder(
            name = "Casino",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(342.5f, 702.5f)
                quadTo(360f, 685f, 360f, 660f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                quadTo(325f, 600f, 300f, 600f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                quadTo(240f, 635f, 240f, 660f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                quadTo(275f, 720f, 300f, 720f)
                reflectiveQuadToRelative(42.5f, -17.5f)
                close()
                moveTo(342.5f, 342.5f)
                quadTo(360f, 325f, 360f, 300f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                quadTo(325f, 240f, 300f, 240f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                quadTo(240f, 275f, 240f, 300f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                quadTo(275f, 360f, 300f, 360f)
                reflectiveQuadToRelative(42.5f, -17.5f)
                close()
                moveTo(522.5f, 522.5f)
                quadTo(540f, 505f, 540f, 480f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                quadTo(505f, 420f, 480f, 420f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                quadTo(420f, 455f, 420f, 480f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                quadTo(455f, 540f, 480f, 540f)
                reflectiveQuadToRelative(42.5f, -17.5f)
                close()
                moveTo(702.5f, 702.5f)
                quadTo(720f, 685f, 720f, 660f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                quadTo(685f, 600f, 660f, 600f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                quadTo(600f, 635f, 600f, 660f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                quadTo(635f, 720f, 660f, 720f)
                reflectiveQuadToRelative(42.5f, -17.5f)
                close()
                moveTo(702.5f, 342.5f)
                quadTo(720f, 325f, 720f, 300f)
                reflectiveQuadToRelative(-17.5f, -42.5f)
                quadTo(685f, 240f, 660f, 240f)
                reflectiveQuadToRelative(-42.5f, 17.5f)
                quadTo(600f, 275f, 600f, 300f)
                reflectiveQuadToRelative(17.5f, 42.5f)
                quadTo(635f, 360f, 660f, 360f)
                reflectiveQuadToRelative(42.5f, -17.5f)
                close()
                moveTo(200f, 840f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 760f)
                verticalLineToRelative(-560f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(200f, 120f)
                horizontalLineToRelative(560f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 200f)
                verticalLineToRelative(560f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(760f, 840f)
                lineTo(200f, 840f)
                close()
                moveTo(200f, 760f)
                horizontalLineToRelative(560f)
                verticalLineToRelative(-560f)
                lineTo(200f, 200f)
                verticalLineToRelative(560f)
                close()
                moveTo(200f, 200f)
                verticalLineToRelative(560f)
                verticalLineToRelative(-560f)
                close()
            }
        }.build()

        return _Casino!!
    }

@Suppress("ObjectPropertyName")
private var _Casino: ImageVector? = null
