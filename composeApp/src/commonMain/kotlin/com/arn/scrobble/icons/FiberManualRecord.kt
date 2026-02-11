package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FiberManualRecord: ImageVector
    get() {
        if (_FiberManualRecord != null) {
            return _FiberManualRecord!!
        }
        _FiberManualRecord = ImageVector.Builder(
            name = "FiberManualRecord",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(480f, 480f)
                close()
                moveTo(282f, 678f)
                quadToRelative(-82f, -82f, -82f, -198f)
                reflectiveQuadToRelative(82f, -198f)
                quadToRelative(82f, -82f, 198f, -82f)
                reflectiveQuadToRelative(198f, 82f)
                quadToRelative(82f, 82f, 82f, 198f)
                reflectiveQuadToRelative(-82f, 198f)
                quadToRelative(-82f, 82f, -198f, 82f)
                reflectiveQuadToRelative(-198f, -82f)
                close()
                moveTo(621.5f, 621.5f)
                quadTo(680f, 563f, 680f, 480f)
                reflectiveQuadToRelative(-58.5f, -141.5f)
                quadTo(563f, 280f, 480f, 280f)
                reflectiveQuadToRelative(-141.5f, 58.5f)
                quadTo(280f, 397f, 280f, 480f)
                reflectiveQuadToRelative(58.5f, 141.5f)
                quadTo(397f, 680f, 480f, 680f)
                reflectiveQuadToRelative(141.5f, -58.5f)
                close()
            }
        }.build()

        return _FiberManualRecord!!
    }

@Suppress("ObjectPropertyName")
private var _FiberManualRecord: ImageVector? = null
