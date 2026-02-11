package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Share: ImageVector
    get() {
        if (_Share != null) {
            return _Share!!
        }
        _Share = ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(680f, 880f)
                quadToRelative(-50f, 0f, -85f, -35f)
                reflectiveQuadToRelative(-35f, -85f)
                quadToRelative(0f, -6f, 3f, -28f)
                lineTo(282f, 568f)
                quadToRelative(-16f, 15f, -37f, 23.5f)
                reflectiveQuadToRelative(-45f, 8.5f)
                quadToRelative(-50f, 0f, -85f, -35f)
                reflectiveQuadToRelative(-35f, -85f)
                quadToRelative(0f, -50f, 35f, -85f)
                reflectiveQuadToRelative(85f, -35f)
                quadToRelative(24f, 0f, 45f, 8.5f)
                reflectiveQuadToRelative(37f, 23.5f)
                lineToRelative(281f, -164f)
                quadToRelative(-2f, -7f, -2.5f, -13.5f)
                reflectiveQuadTo(560f, 200f)
                quadToRelative(0f, -50f, 35f, -85f)
                reflectiveQuadToRelative(85f, -35f)
                quadToRelative(50f, 0f, 85f, 35f)
                reflectiveQuadToRelative(35f, 85f)
                quadToRelative(0f, 50f, -35f, 85f)
                reflectiveQuadToRelative(-85f, 35f)
                quadToRelative(-24f, 0f, -45f, -8.5f)
                reflectiveQuadTo(598f, 288f)
                lineTo(317f, 452f)
                quadToRelative(2f, 7f, 2.5f, 13.5f)
                reflectiveQuadToRelative(0.5f, 14.5f)
                quadToRelative(0f, 8f, -0.5f, 14.5f)
                reflectiveQuadTo(317f, 508f)
                lineToRelative(281f, 164f)
                quadToRelative(16f, -15f, 37f, -23.5f)
                reflectiveQuadToRelative(45f, -8.5f)
                quadToRelative(50f, 0f, 85f, 35f)
                reflectiveQuadToRelative(35f, 85f)
                quadToRelative(0f, 50f, -35f, 85f)
                reflectiveQuadToRelative(-85f, 35f)
                close()
                moveTo(680f, 800f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                reflectiveQuadTo(720f, 760f)
                quadToRelative(0f, -17f, -11.5f, -28.5f)
                reflectiveQuadTo(680f, 720f)
                quadToRelative(-17f, 0f, -28.5f, 11.5f)
                reflectiveQuadTo(640f, 760f)
                quadToRelative(0f, 17f, 11.5f, 28.5f)
                reflectiveQuadTo(680f, 800f)
                close()
                moveTo(200f, 520f)
                quadToRelative(17f, 0f, 28.5f, -11.5f)
                reflectiveQuadTo(240f, 480f)
                quadToRelative(0f, -17f, -11.5f, -28.5f)
                reflectiveQuadTo(200f, 440f)
                quadToRelative(-17f, 0f, -28.5f, 11.5f)
                reflectiveQuadTo(160f, 480f)
                quadToRelative(0f, 17f, 11.5f, 28.5f)
                reflectiveQuadTo(200f, 520f)
                close()
                moveTo(708.5f, 228.5f)
                quadTo(720f, 217f, 720f, 200f)
                reflectiveQuadToRelative(-11.5f, -28.5f)
                quadTo(697f, 160f, 680f, 160f)
                reflectiveQuadToRelative(-28.5f, 11.5f)
                quadTo(640f, 183f, 640f, 200f)
                reflectiveQuadToRelative(11.5f, 28.5f)
                quadTo(663f, 240f, 680f, 240f)
                reflectiveQuadToRelative(28.5f, -11.5f)
                close()
                moveTo(680f, 760f)
                close()
                moveTo(200f, 480f)
                close()
                moveTo(680f, 200f)
                close()
            }
        }.build()

        return _Share!!
    }

@Suppress("ObjectPropertyName")
private var _Share: ImageVector? = null
