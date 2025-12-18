package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.FindReplace: ImageVector
    get() {
        if (_FindReplace != null) {
            return _FindReplace!!
        }
        _FindReplace = ImageVector.Builder(
            name = "FindReplace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(440f, 240f)
                quadToRelative(-63f, 0f, -113.5f, 34.5f)
                reflectiveQuadTo(254f, 365f)
                quadToRelative(-6f, 15f, -19.5f, 22.5f)
                reflectiveQuadTo(205f, 392f)
                quadToRelative(-16f, -3f, -25.5f, -15.5f)
                reflectiveQuadTo(175f, 350f)
                quadToRelative(27f, -84f, 99f, -137f)
                reflectiveQuadToRelative(166f, -53f)
                quadToRelative(59f, 0f, 110.5f, 22.5f)
                reflectiveQuadTo(640f, 244f)
                verticalLineToRelative(-44f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(680f, 160f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(720f, 200f)
                verticalLineToRelative(160f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(680f, 400f)
                lineTo(520f, 400f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(480f, 360f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(520f, 320f)
                horizontalLineToRelative(80f)
                quadToRelative(-29f, -36f, -69.5f, -58f)
                reflectiveQuadTo(440f, 240f)
                close()
                moveTo(440f, 720f)
                quadToRelative(-59f, 0f, -110.5f, -22.5f)
                reflectiveQuadTo(240f, 636f)
                verticalLineToRelative(44f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(200f, 720f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(160f, 680f)
                verticalLineToRelative(-160f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(200f, 480f)
                horizontalLineToRelative(160f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(400f, 520f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(360f, 560f)
                horizontalLineToRelative(-80f)
                quadToRelative(29f, 36f, 69.5f, 58f)
                reflectiveQuadToRelative(90.5f, 22f)
                quadToRelative(62f, 0f, 111.5f, -33f)
                reflectiveQuadToRelative(72.5f, -87f)
                quadToRelative(7f, -16f, 20.5f, -25.5f)
                reflectiveQuadTo(675f, 488f)
                quadToRelative(17f, 4f, 25f, 18f)
                reflectiveQuadToRelative(3f, 30f)
                quadToRelative(-7f, 20f, -16.5f, 37.5f)
                reflectiveQuadTo(664f, 608f)
                lineToRelative(148f, 148f)
                quadToRelative(11f, 11f, 11f, 28f)
                reflectiveQuadToRelative(-11f, 28f)
                quadToRelative(-11f, 11f, -28f, 11f)
                reflectiveQuadToRelative(-28f, -11f)
                lineTo(608f, 664f)
                quadToRelative(-36f, 27f, -78.5f, 41.5f)
                reflectiveQuadTo(440f, 720f)
                close()
            }
        }.build()

        return _FindReplace!!
    }

@Suppress("ObjectPropertyName")
private var _FindReplace: ImageVector? = null
