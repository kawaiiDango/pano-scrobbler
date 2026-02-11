package com.arn.scrobble.icons.automirrored

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons

val Icons.AutoMirrored.Help: ImageVector
    get() {
        if (_Help != null) {
            return _Help!!
        }
        _Help = ImageVector.Builder(
            name = "AutoMirrored.Help",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = true
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(513.5f, 705.5f)
                quadTo(528f, 691f, 528f, 670f)
                reflectiveQuadToRelative(-14.5f, -35.5f)
                quadTo(499f, 620f, 478f, 620f)
                reflectiveQuadToRelative(-35.5f, 14.5f)
                quadTo(428f, 649f, 428f, 670f)
                reflectiveQuadToRelative(14.5f, 35.5f)
                quadTo(457f, 720f, 478f, 720f)
                reflectiveQuadToRelative(35.5f, -14.5f)
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
                moveTo(480f, 800f)
                quadToRelative(134f, 0f, 227f, -93f)
                reflectiveQuadToRelative(93f, -227f)
                quadToRelative(0f, -134f, -93f, -227f)
                reflectiveQuadToRelative(-227f, -93f)
                quadToRelative(-134f, 0f, -227f, 93f)
                reflectiveQuadToRelative(-93f, 227f)
                quadToRelative(0f, 134f, 93f, 227f)
                reflectiveQuadToRelative(227f, 93f)
                close()
                moveTo(480f, 480f)
                close()
                moveTo(484f, 308f)
                quadToRelative(25f, 0f, 43.5f, 16f)
                reflectiveQuadToRelative(18.5f, 40f)
                quadToRelative(0f, 22f, -13.5f, 39f)
                reflectiveQuadTo(502f, 435f)
                quadToRelative(-23f, 20f, -40.5f, 44f)
                reflectiveQuadTo(444f, 533f)
                quadToRelative(0f, 14f, 10.5f, 23.5f)
                reflectiveQuadTo(479f, 566f)
                quadToRelative(15f, 0f, 25.5f, -10f)
                reflectiveQuadToRelative(13.5f, -25f)
                quadToRelative(4f, -21f, 18f, -37.5f)
                reflectiveQuadToRelative(30f, -31.5f)
                quadToRelative(23f, -22f, 39.5f, -48f)
                reflectiveQuadToRelative(16.5f, -58f)
                quadToRelative(0f, -51f, -41.5f, -83.5f)
                reflectiveQuadTo(484f, 240f)
                quadToRelative(-38f, 0f, -72.5f, 16f)
                reflectiveQuadTo(359f, 305f)
                quadToRelative(-7f, 12f, -4.5f, 25.5f)
                reflectiveQuadTo(368f, 351f)
                quadToRelative(14f, 8f, 29f, 5f)
                reflectiveQuadToRelative(25f, -17f)
                quadToRelative(11f, -15f, 27.5f, -23f)
                reflectiveQuadToRelative(34.5f, -8f)
                close()
            }
        }.build()

        return _Help!!
    }

@Suppress("ObjectPropertyName")
private var _Help: ImageVector? = null
