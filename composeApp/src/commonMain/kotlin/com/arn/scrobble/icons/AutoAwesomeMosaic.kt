package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.AutoAwesomeMosaic: ImageVector
    get() {
        if (_AutoAwesomeMosaic != null) {
            return _AutoAwesomeMosaic!!
        }
        _AutoAwesomeMosaic = ImageVector.Builder(
            name = "AutoAwesomeMosaic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(440f, 840f)
                lineTo(200f, 840f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 760f)
                verticalLineToRelative(-560f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(200f, 120f)
                horizontalLineToRelative(240f)
                verticalLineToRelative(720f)
                close()
                moveTo(360f, 760f)
                verticalLineToRelative(-560f)
                lineTo(200f, 200f)
                verticalLineToRelative(560f)
                horizontalLineToRelative(160f)
                close()
                moveTo(520f, 440f)
                verticalLineToRelative(-320f)
                horizontalLineToRelative(240f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 200f)
                verticalLineToRelative(240f)
                lineTo(520f, 440f)
                close()
                moveTo(600f, 360f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-160f)
                lineTo(600f, 200f)
                verticalLineToRelative(160f)
                close()
                moveTo(520f, 840f)
                verticalLineToRelative(-320f)
                horizontalLineToRelative(320f)
                verticalLineToRelative(240f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(760f, 840f)
                lineTo(520f, 840f)
                close()
                moveTo(600f, 760f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-160f)
                lineTo(600f, 600f)
                verticalLineToRelative(160f)
                close()
                moveTo(360f, 480f)
                close()
                moveTo(600f, 360f)
                close()
                moveTo(600f, 600f)
                close()
            }
        }.build()

        return _AutoAwesomeMosaic!!
    }

@Suppress("ObjectPropertyName")
private var _AutoAwesomeMosaic: ImageVector? = null
