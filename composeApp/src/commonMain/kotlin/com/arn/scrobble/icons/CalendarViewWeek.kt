package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.CalendarViewWeek: ImageVector
    get() {
        if (_CalendarViewWeek != null) {
            return _CalendarViewWeek!!
        }
        _CalendarViewWeek = ImageVector.Builder(
            name = "CalendarViewWeek",
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
                moveTo(520f, 720f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(-480f)
                lineTo(520f, 240f)
                verticalLineToRelative(480f)
                close()
                moveTo(340f, 720f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(-480f)
                lineTo(340f, 240f)
                verticalLineToRelative(480f)
                close()
                moveTo(160f, 720f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(-480f)
                lineTo(160f, 240f)
                verticalLineToRelative(480f)
                close()
                moveTo(700f, 720f)
                horizontalLineToRelative(100f)
                verticalLineToRelative(-480f)
                lineTo(700f, 240f)
                verticalLineToRelative(480f)
                close()
            }
        }.build()

        return _CalendarViewWeek!!
    }

@Suppress("ObjectPropertyName")
private var _CalendarViewWeek: ImageVector? = null
