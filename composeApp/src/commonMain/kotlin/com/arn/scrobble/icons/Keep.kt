package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Keep: ImageVector
    get() {
        if (_Keep != null) {
            return _Keep!!
        }
        _Keep = ImageVector.Builder(
            name = "Keep",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(640f, 200f)
                verticalLineToRelative(280f)
                lineToRelative(68f, 68f)
                quadToRelative(6f, 6f, 9f, 13.5f)
                reflectiveQuadToRelative(3f, 15.5f)
                verticalLineToRelative(23f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(680f, 640f)
                lineTo(520f, 640f)
                verticalLineToRelative(234f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(480f, 914f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(440f, 874f)
                verticalLineToRelative(-234f)
                lineTo(280f, 640f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(240f, 600f)
                verticalLineToRelative(-23f)
                quadToRelative(0f, -8f, 3f, -15.5f)
                reflectiveQuadToRelative(9f, -13.5f)
                lineToRelative(68f, -68f)
                verticalLineToRelative(-280f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(280f, 160f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(320f, 120f)
                horizontalLineToRelative(320f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(680f, 160f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(640f, 200f)
                close()
                moveTo(354f, 560f)
                horizontalLineToRelative(252f)
                lineToRelative(-46f, -46f)
                verticalLineToRelative(-314f)
                lineTo(400f, 200f)
                verticalLineToRelative(314f)
                lineToRelative(-46f, 46f)
                close()
                moveTo(480f, 560f)
                close()
            }
        }.build()

        return _Keep!!
    }

@Suppress("ObjectPropertyName")
private var _Keep: ImageVector? = null
