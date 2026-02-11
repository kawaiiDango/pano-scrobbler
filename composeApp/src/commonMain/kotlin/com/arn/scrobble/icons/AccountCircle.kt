package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.AccountCircle: ImageVector
    get() {
        if (_AccountCircle != null) {
            return _AccountCircle!!
        }
        _AccountCircle = ImageVector.Builder(
            name = "AccountCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(234f, 684f)
                quadToRelative(51f, -39f, 114f, -61.5f)
                reflectiveQuadTo(480f, 600f)
                quadToRelative(69f, 0f, 132f, 22.5f)
                reflectiveQuadTo(726f, 684f)
                quadToRelative(35f, -41f, 54.5f, -93f)
                reflectiveQuadTo(800f, 480f)
                quadToRelative(0f, -133f, -93.5f, -226.5f)
                reflectiveQuadTo(480f, 160f)
                quadToRelative(-133f, 0f, -226.5f, 93.5f)
                reflectiveQuadTo(160f, 480f)
                quadToRelative(0f, 59f, 19.5f, 111f)
                reflectiveQuadToRelative(54.5f, 93f)
                close()
                moveTo(380.5f, 479.5f)
                quadTo(340f, 439f, 340f, 380f)
                reflectiveQuadToRelative(40.5f, -99.5f)
                quadTo(421f, 240f, 480f, 240f)
                reflectiveQuadToRelative(99.5f, 40.5f)
                quadTo(620f, 321f, 620f, 380f)
                reflectiveQuadToRelative(-40.5f, 99.5f)
                quadTo(539f, 520f, 480f, 520f)
                reflectiveQuadToRelative(-99.5f, -40.5f)
                close()
                moveTo(480f, 880f)
                quadToRelative(-83f, 0f, -156f, -31.5f)
                reflectiveQuadTo(197f, 763f)
                quadToRelative(-54f, -54f, -85.5f, -127f)
                reflectiveQuadTo(80f, 480f)
                quadToRelative(0f, -83f, 31.5f, -156f)
                reflectiveQuadTo(197f, 197f)
                quadToRelative(54f, -54f, 127f, -85.5f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(83f, 0f, 156f, 31.5f)
                reflectiveQuadTo(763f, 197f)
                quadToRelative(54f, 54f, 85.5f, 127f)
                reflectiveQuadTo(880f, 480f)
                quadToRelative(0f, 83f, -31.5f, 156f)
                reflectiveQuadTo(763f, 763f)
                quadToRelative(-54f, 54f, -127f, 85.5f)
                reflectiveQuadTo(480f, 880f)
                close()
                moveTo(580f, 784.5f)
                quadToRelative(47f, -15.5f, 86f, -44.5f)
                quadToRelative(-39f, -29f, -86f, -44.5f)
                reflectiveQuadTo(480f, 680f)
                quadToRelative(-53f, 0f, -100f, 15.5f)
                reflectiveQuadTo(294f, 740f)
                quadToRelative(39f, 29f, 86f, 44.5f)
                reflectiveQuadTo(480f, 800f)
                quadToRelative(53f, 0f, 100f, -15.5f)
                close()
                moveTo(523f, 423f)
                quadToRelative(17f, -17f, 17f, -43f)
                reflectiveQuadToRelative(-17f, -43f)
                quadToRelative(-17f, -17f, -43f, -17f)
                reflectiveQuadToRelative(-43f, 17f)
                quadToRelative(-17f, 17f, -17f, 43f)
                reflectiveQuadToRelative(17f, 43f)
                quadToRelative(17f, 17f, 43f, 17f)
                reflectiveQuadToRelative(43f, -17f)
                close()
                moveTo(480f, 380f)
                close()
                moveTo(480f, 740f)
                close()
            }
        }.build()

        return _AccountCircle!!
    }

@Suppress("ObjectPropertyName")
private var _AccountCircle: ImageVector? = null
