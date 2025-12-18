package com.arn.scrobble.icons.automirrored

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons

val Icons.AutoMirrored.KeyboardArrowRight: ImageVector
    get() {
        if (_KeyboardArrowRight != null) {
            return _KeyboardArrowRight!!
        }
        _KeyboardArrowRight = ImageVector.Builder(
            name = "AutoMirrored.KeyboardArrowRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = true
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(504f, 480f)
                lineTo(348f, 324f)
                quadToRelative(-11f, -11f, -11f, -28f)
                reflectiveQuadToRelative(11f, -28f)
                quadToRelative(11f, -11f, 28f, -11f)
                reflectiveQuadToRelative(28f, 11f)
                lineToRelative(184f, 184f)
                quadToRelative(6f, 6f, 8.5f, 13f)
                reflectiveQuadToRelative(2.5f, 15f)
                quadToRelative(0f, 8f, -2.5f, 15f)
                reflectiveQuadToRelative(-8.5f, 13f)
                lineTo(404f, 692f)
                quadToRelative(-11f, 11f, -28f, 11f)
                reflectiveQuadToRelative(-28f, -11f)
                quadToRelative(-11f, -11f, -11f, -28f)
                reflectiveQuadToRelative(11f, -28f)
                lineToRelative(156f, -156f)
                close()
            }
        }.build()

        return _KeyboardArrowRight!!
    }

@Suppress("ObjectPropertyName")
private var _KeyboardArrowRight: ImageVector? = null
