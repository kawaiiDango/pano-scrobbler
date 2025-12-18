package com.arn.scrobble.icons.automirrored

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons

val Icons.AutoMirrored.VolumeOff: ImageVector
    get() {
        if (_VolumeOff != null) {
            return _VolumeOff!!
        }
        _VolumeOff = ImageVector.Builder(
            name = "AutoMirrored.VolumeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = true
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(671f, 783f)
                quadToRelative(-11f, 7f, -22f, 13f)
                reflectiveQuadToRelative(-23f, 11f)
                quadToRelative(-15f, 7f, -30.5f, 0f)
                reflectiveQuadTo(574f, 784f)
                quadToRelative(-6f, -15f, 1.5f, -29.5f)
                reflectiveQuadTo(598f, 733f)
                quadToRelative(4f, -2f, 7.5f, -4f)
                reflectiveQuadToRelative(7.5f, -4f)
                lineTo(480f, 592f)
                verticalLineToRelative(111f)
                quadToRelative(0f, 27f, -24.5f, 37.5f)
                reflectiveQuadTo(412f, 732f)
                lineTo(280f, 600f)
                lineTo(160f, 600f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(120f, 560f)
                verticalLineToRelative(-160f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(160f, 360f)
                horizontalLineToRelative(88f)
                lineTo(84f, 196f)
                quadToRelative(-11f, -11f, -11f, -28f)
                reflectiveQuadToRelative(11f, -28f)
                quadToRelative(11f, -11f, 28f, -11f)
                reflectiveQuadToRelative(28f, 11f)
                lineToRelative(680f, 680f)
                quadToRelative(11f, 11f, 11f, 28f)
                reflectiveQuadToRelative(-11f, 28f)
                quadToRelative(-11f, 11f, -28f, 11f)
                reflectiveQuadToRelative(-28f, -11f)
                lineToRelative(-93f, -93f)
                close()
                moveTo(760f, 479f)
                quadToRelative(0f, -83f, -44f, -151.5f)
                reflectiveQuadTo(598f, 225f)
                quadToRelative(-15f, -7f, -22f, -21.5f)
                reflectiveQuadToRelative(-2f, -29.5f)
                quadToRelative(6f, -16f, 21.5f, -23f)
                reflectiveQuadToRelative(31.5f, 0f)
                quadToRelative(97f, 43f, 155f, 131f)
                reflectiveQuadToRelative(58f, 197f)
                quadToRelative(0f, 33f, -6f, 65.5f)
                reflectiveQuadTo(817f, 607f)
                quadToRelative(-8f, 22f, -24.5f, 27.5f)
                reflectiveQuadToRelative(-30.5f, 0.5f)
                quadToRelative(-14f, -5f, -22.5f, -18f)
                reflectiveQuadToRelative(-0.5f, -30f)
                quadToRelative(11f, -26f, 16f, -52.5f)
                reflectiveQuadToRelative(5f, -55.5f)
                close()
                moveTo(591f, 337f)
                quadToRelative(33f, 21f, 51f, 63f)
                reflectiveQuadToRelative(18f, 80f)
                verticalLineToRelative(10f)
                quadToRelative(0f, 5f, -1f, 10f)
                quadToRelative(-2f, 13f, -14f, 17f)
                reflectiveQuadToRelative(-22f, -6f)
                lineToRelative(-51f, -51f)
                quadToRelative(-6f, -6f, -9f, -13.5f)
                reflectiveQuadToRelative(-3f, -15.5f)
                verticalLineToRelative(-77f)
                quadToRelative(0f, -12f, 10.5f, -17.5f)
                reflectiveQuadToRelative(20.5f, 0.5f)
                close()
                moveTo(390f, 278f)
                quadToRelative(-6f, -6f, -6f, -14f)
                reflectiveQuadToRelative(6f, -14f)
                lineToRelative(22f, -22f)
                quadToRelative(19f, -19f, 43.5f, -8.5f)
                reflectiveQuadTo(480f, 257f)
                verticalLineToRelative(63f)
                quadToRelative(0f, 14f, -12f, 19f)
                reflectiveQuadToRelative(-22f, -5f)
                lineToRelative(-56f, -56f)
                close()
                moveTo(400f, 606f)
                verticalLineToRelative(-94f)
                lineToRelative(-72f, -72f)
                lineTo(200f, 440f)
                verticalLineToRelative(80f)
                horizontalLineToRelative(114f)
                lineToRelative(86f, 86f)
                close()
                moveTo(364f, 476f)
                close()
            }
        }.build()

        return _VolumeOff!!
    }

@Suppress("ObjectPropertyName")
private var _VolumeOff: ImageVector? = null
