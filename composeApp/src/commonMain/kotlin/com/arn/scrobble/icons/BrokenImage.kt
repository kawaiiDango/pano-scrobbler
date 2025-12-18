package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.BrokenImage: ImageVector
    get() {
        if (_BrokenImage != null) {
            return _BrokenImage!!
        }
        _BrokenImage = ImageVector.Builder(
            name = "BrokenImage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
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
                moveTo(240f, 503f)
                lineTo(372f, 371f)
                quadToRelative(12f, -12f, 28f, -12f)
                reflectiveQuadToRelative(28f, 12f)
                lineToRelative(132f, 132f)
                lineToRelative(132f, -132f)
                quadToRelative(12f, -12f, 28f, -12f)
                reflectiveQuadToRelative(28f, 12f)
                lineToRelative(12f, 12f)
                verticalLineToRelative(-183f)
                lineTo(200f, 200f)
                verticalLineToRelative(263f)
                lineToRelative(40f, 40f)
                close()
                moveTo(200f, 760f)
                horizontalLineToRelative(560f)
                verticalLineToRelative(-264f)
                lineToRelative(-40f, -40f)
                lineToRelative(-132f, 132f)
                quadToRelative(-12f, 12f, -28f, 12f)
                reflectiveQuadToRelative(-28f, -12f)
                lineTo(400f, 456f)
                lineTo(268f, 588f)
                quadToRelative(-12f, 12f, -28f, 12f)
                reflectiveQuadToRelative(-28f, -12f)
                lineToRelative(-12f, -12f)
                verticalLineToRelative(184f)
                close()
                moveTo(200f, 760f)
                verticalLineToRelative(-264f)
                verticalLineToRelative(80f)
                verticalLineToRelative(-376f)
                verticalLineToRelative(560f)
                close()
            }
        }.build()

        return _BrokenImage!!
    }

@Suppress("ObjectPropertyName")
private var _BrokenImage: ImageVector? = null
