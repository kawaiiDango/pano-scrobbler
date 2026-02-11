package com.arn.scrobble.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons

val Icons.Filled.Favorite: ImageVector
    get() {
        if (_Favorite != null) {
            return _Favorite!!
        }
        _Favorite = ImageVector.Builder(
            name = "Filled.Favorite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(451.5f, 808f)
                quadToRelative(-14.5f, -5f, -25.5f, -16f)
                lineToRelative(-69f, -63f)
                quadToRelative(-106f, -97f, -191.5f, -192.5f)
                reflectiveQuadTo(80f, 326f)
                quadToRelative(0f, -94f, 63f, -157f)
                reflectiveQuadToRelative(157f, -63f)
                quadToRelative(53f, 0f, 100f, 22.5f)
                reflectiveQuadToRelative(80f, 61.5f)
                quadToRelative(33f, -39f, 80f, -61.5f)
                reflectiveQuadTo(660f, 106f)
                quadToRelative(94f, 0f, 157f, 63f)
                reflectiveQuadToRelative(63f, 157f)
                quadToRelative(0f, 115f, -85f, 211f)
                reflectiveQuadTo(602f, 730f)
                lineToRelative(-68f, 62f)
                quadToRelative(-11f, 11f, -25.5f, 16f)
                reflectiveQuadToRelative(-28.5f, 5f)
                quadToRelative(-14f, 0f, -28.5f, -5f)
                close()
            }
        }.build()

        return _Favorite!!
    }

@Suppress("ObjectPropertyName")
private var _Favorite: ImageVector? = null
