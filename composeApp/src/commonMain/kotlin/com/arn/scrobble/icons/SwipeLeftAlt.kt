package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.SwipeLeftAlt: ImageVector
    get() {
        if (_SwipeLeftAlt != null) {
            return _SwipeLeftAlt!!
        }
        _SwipeLeftAlt = ImageVector.Builder(
            name = "SwipeLeftAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(600f, 680f)
                quadToRelative(-73f, 0f, -127.5f, -45.5f)
                reflectiveQuadTo(404f, 520f)
                lineTo(233f, 520f)
                lineToRelative(36f, 36f)
                quadToRelative(11f, 11f, 11f, 27.5f)
                reflectiveQuadTo(268f, 612f)
                quadToRelative(-11f, 11f, -28f, 11f)
                reflectiveQuadToRelative(-28f, -11f)
                lineTo(108f, 508f)
                quadToRelative(-6f, -6f, -8.5f, -13f)
                reflectiveQuadTo(97f, 480f)
                quadToRelative(0f, -8f, 2.5f, -15f)
                reflectiveQuadToRelative(8.5f, -13f)
                lineToRelative(104f, -104f)
                quadToRelative(11f, -11f, 27.5f, -11f)
                reflectiveQuadToRelative(28.5f, 11f)
                quadToRelative(12f, 12f, 12f, 28.5f)
                reflectiveQuadTo(268f, 405f)
                lineToRelative(-35f, 35f)
                horizontalLineToRelative(171f)
                quadToRelative(14f, -69f, 68.5f, -114.5f)
                reflectiveQuadTo(600f, 280f)
                quadToRelative(83f, 0f, 141.5f, 58.5f)
                reflectiveQuadTo(800f, 480f)
                quadToRelative(0f, 83f, -58.5f, 141.5f)
                reflectiveQuadTo(600f, 680f)
                close()
                moveTo(685f, 565f)
                quadToRelative(35f, -35f, 35f, -85f)
                reflectiveQuadToRelative(-35f, -85f)
                quadToRelative(-35f, -35f, -85f, -35f)
                reflectiveQuadToRelative(-85f, 35f)
                quadToRelative(-35f, 35f, -35f, 85f)
                reflectiveQuadToRelative(35f, 85f)
                quadToRelative(35f, 35f, 85f, 35f)
                reflectiveQuadToRelative(85f, -35f)
                close()
                moveTo(600f, 480f)
                close()
            }
        }.build()

        return _SwipeLeftAlt!!
    }

@Suppress("ObjectPropertyName")
private var _SwipeLeftAlt: ImageVector? = null
