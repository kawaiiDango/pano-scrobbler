package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Cake: ImageVector
    get() {
        if (_Cake != null) {
            return _Cake!!
        }
        _Cake = ImageVector.Builder(
            name = "Cake",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 880f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(120f, 840f)
                verticalLineToRelative(-200f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(200f, 560f)
                verticalLineToRelative(-160f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(280f, 320f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-58f)
                quadToRelative(-18f, -12f, -29f, -29f)
                reflectiveQuadToRelative(-11f, -41f)
                quadToRelative(0f, -15f, 6f, -29.5f)
                reflectiveQuadToRelative(18f, -26.5f)
                lineToRelative(42f, -42f)
                quadToRelative(2f, -2f, 14f, -6f)
                quadToRelative(2f, 0f, 14f, 6f)
                lineToRelative(42f, 42f)
                quadToRelative(12f, 12f, 18f, 26.5f)
                reflectiveQuadToRelative(6f, 29.5f)
                quadToRelative(0f, 24f, -11f, 41f)
                reflectiveQuadToRelative(-29f, 29f)
                verticalLineToRelative(58f)
                horizontalLineToRelative(160f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(760f, 400f)
                verticalLineToRelative(160f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 640f)
                verticalLineToRelative(200f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(800f, 880f)
                lineTo(160f, 880f)
                close()
                moveTo(280f, 560f)
                horizontalLineToRelative(400f)
                verticalLineToRelative(-160f)
                lineTo(280f, 400f)
                verticalLineToRelative(160f)
                close()
                moveTo(200f, 800f)
                horizontalLineToRelative(560f)
                verticalLineToRelative(-160f)
                lineTo(200f, 640f)
                verticalLineToRelative(160f)
                close()
                moveTo(280f, 560f)
                horizontalLineToRelative(400f)
                horizontalLineToRelative(-400f)
                close()
                moveTo(200f, 800f)
                horizontalLineToRelative(560f)
                horizontalLineToRelative(-560f)
                close()
                moveTo(760f, 560f)
                lineTo(200f, 560f)
                horizontalLineToRelative(560f)
                close()
            }
        }.build()

        return _Cake!!
    }

@Suppress("ObjectPropertyName")
private var _Cake: ImageVector? = null
