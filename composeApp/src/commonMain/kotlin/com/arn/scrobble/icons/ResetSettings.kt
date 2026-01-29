package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.ResetSettings: ImageVector
    get() {
        if (_ResetSettings != null) {
            return _ResetSettings!!
        }
        _ResetSettings = ImageVector.Builder(
            name = "ResetSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(550f, 570f)
                horizontalLineToRelative(100f)
                quadToRelative(13f, 0f, 21.5f, 8.5f)
                reflectiveQuadTo(680f, 600f)
                quadToRelative(0f, 13f, -8.5f, 21.5f)
                reflectiveQuadTo(650f, 630f)
                lineTo(550f, 630f)
                quadToRelative(-13f, 0f, -21.5f, -8.5f)
                reflectiveQuadTo(520f, 600f)
                quadToRelative(0f, -13f, 8.5f, -21.5f)
                reflectiveQuadTo(550f, 570f)
                close()
                moveTo(580f, 810f)
                verticalLineToRelative(-20f)
                horizontalLineToRelative(-30f)
                quadToRelative(-13f, 0f, -21.5f, -8.5f)
                reflectiveQuadTo(520f, 760f)
                quadToRelative(0f, -13f, 8.5f, -21.5f)
                reflectiveQuadTo(550f, 730f)
                horizontalLineToRelative(30f)
                verticalLineToRelative(-20f)
                quadToRelative(0f, -13f, 8.5f, -21.5f)
                reflectiveQuadTo(610f, 680f)
                quadToRelative(13f, 0f, 21.5f, 8.5f)
                reflectiveQuadTo(640f, 710f)
                verticalLineToRelative(100f)
                quadToRelative(0f, 13f, -8.5f, 21.5f)
                reflectiveQuadTo(610f, 840f)
                quadToRelative(-13f, 0f, -21.5f, -8.5f)
                reflectiveQuadTo(580f, 810f)
                close()
                moveTo(710f, 730f)
                horizontalLineToRelative(100f)
                quadToRelative(13f, 0f, 21.5f, 8.5f)
                reflectiveQuadTo(840f, 760f)
                quadToRelative(0f, 13f, -8.5f, 21.5f)
                reflectiveQuadTo(810f, 790f)
                lineTo(710f, 790f)
                quadToRelative(-13f, 0f, -21.5f, -8.5f)
                reflectiveQuadTo(680f, 760f)
                quadToRelative(0f, -13f, 8.5f, -21.5f)
                reflectiveQuadTo(710f, 730f)
                close()
                moveTo(720f, 650f)
                verticalLineToRelative(-100f)
                quadToRelative(0f, -13f, 8.5f, -21.5f)
                reflectiveQuadTo(750f, 520f)
                quadToRelative(13f, 0f, 21.5f, 8.5f)
                reflectiveQuadTo(780f, 550f)
                verticalLineToRelative(20f)
                horizontalLineToRelative(30f)
                quadToRelative(13f, 0f, 21.5f, 8.5f)
                reflectiveQuadTo(840f, 600f)
                quadToRelative(0f, 13f, -8.5f, 21.5f)
                reflectiveQuadTo(810f, 630f)
                horizontalLineToRelative(-30f)
                verticalLineToRelative(20f)
                quadToRelative(0f, 13f, -8.5f, 21.5f)
                reflectiveQuadTo(750f, 680f)
                quadToRelative(-13f, 0f, -21.5f, -8.5f)
                reflectiveQuadTo(720f, 650f)
                close()
                moveTo(480f, 200f)
                quadToRelative(-117f, 0f, -198.5f, 81.5f)
                reflectiveQuadTo(200f, 480f)
                quadToRelative(0f, 72f, 32.5f, 132f)
                reflectiveQuadToRelative(87.5f, 98f)
                verticalLineToRelative(-70f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(360f, 600f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(400f, 640f)
                verticalLineToRelative(160f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(360f, 840f)
                lineTo(200f, 840f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(160f, 800f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(200f, 760f)
                horizontalLineToRelative(54f)
                quadToRelative(-62f, -50f, -98f, -122.5f)
                reflectiveQuadTo(120f, 480f)
                quadToRelative(0f, -75f, 28.5f, -140.5f)
                reflectiveQuadToRelative(77f, -114f)
                quadToRelative(48.5f, -48.5f, 114f, -77f)
                reflectiveQuadTo(480f, 120f)
                quadToRelative(114f, 0f, 204f, 62.5f)
                reflectiveQuadTo(814f, 345f)
                quadToRelative(6f, 16f, 0f, 31f)
                reflectiveQuadToRelative(-22f, 21f)
                quadToRelative(-16f, 6f, -31.5f, 0f)
                reflectiveQuadTo(739f, 375f)
                quadToRelative(-31f, -78f, -100.5f, -126.5f)
                reflectiveQuadTo(480f, 200f)
                close()
            }
        }.build()

        return _ResetSettings!!
    }

@Suppress("ObjectPropertyName")
private var _ResetSettings: ImageVector? = null
