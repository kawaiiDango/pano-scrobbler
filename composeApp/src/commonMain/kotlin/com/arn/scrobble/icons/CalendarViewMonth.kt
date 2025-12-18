package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.CalendarViewMonth: ImageVector
    get() {
        if (_CalendarViewMonth != null) {
            return _CalendarViewMonth!!
        }
        _CalendarViewMonth = ImageVector.Builder(
            name = "CalendarViewMonth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 720f)
                verticalLineToRelative(-480f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 160f)
                horizontalLineToRelative(640f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(880f, 240f)
                verticalLineToRelative(480f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(800f, 800f)
                lineTo(160f, 800f)
                close()
                moveTo(160f, 440f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-200f)
                lineTo(160f, 240f)
                verticalLineToRelative(200f)
                close()
                moveTo(400f, 440f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-200f)
                lineTo(400f, 240f)
                verticalLineToRelative(200f)
                close()
                moveTo(640f, 440f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-200f)
                lineTo(640f, 240f)
                verticalLineToRelative(200f)
                close()
                moveTo(320f, 720f)
                verticalLineToRelative(-200f)
                lineTo(160f, 520f)
                verticalLineToRelative(200f)
                horizontalLineToRelative(160f)
                close()
                moveTo(400f, 720f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-200f)
                lineTo(400f, 520f)
                verticalLineToRelative(200f)
                close()
                moveTo(640f, 720f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-200f)
                lineTo(640f, 520f)
                verticalLineToRelative(200f)
                close()
            }
        }.build()

        return _CalendarViewMonth!!
    }

@Suppress("ObjectPropertyName")
private var _CalendarViewMonth: ImageVector? = null
