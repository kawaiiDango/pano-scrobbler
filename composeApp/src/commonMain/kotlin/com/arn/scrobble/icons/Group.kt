package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Group: ImageVector
    get() {
        if (_Group != null) {
            return _Group!!
        }
        _Group = ImageVector.Builder(
            name = "Group",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(40f, 688f)
                quadToRelative(0f, -34f, 17.5f, -62.5f)
                reflectiveQuadTo(104f, 582f)
                quadToRelative(62f, -31f, 126f, -46.5f)
                reflectiveQuadTo(360f, 520f)
                quadToRelative(66f, 0f, 130f, 15.5f)
                reflectiveQuadTo(616f, 582f)
                quadToRelative(29f, 15f, 46.5f, 43.5f)
                reflectiveQuadTo(680f, 688f)
                verticalLineToRelative(32f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(600f, 800f)
                lineTo(120f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(40f, 720f)
                verticalLineToRelative(-32f)
                close()
                moveTo(840f, 800f)
                lineTo(738f, 800f)
                quadToRelative(11f, -18f, 16.5f, -38.5f)
                reflectiveQuadTo(760f, 720f)
                verticalLineToRelative(-40f)
                quadToRelative(0f, -44f, -24.5f, -84.5f)
                reflectiveQuadTo(666f, 526f)
                quadToRelative(51f, 6f, 96f, 20.5f)
                reflectiveQuadToRelative(84f, 35.5f)
                quadToRelative(36f, 20f, 55f, 44.5f)
                reflectiveQuadToRelative(19f, 53.5f)
                verticalLineToRelative(40f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(840f, 800f)
                close()
                moveTo(247f, 433f)
                quadToRelative(-47f, -47f, -47f, -113f)
                reflectiveQuadToRelative(47f, -113f)
                quadToRelative(47f, -47f, 113f, -47f)
                reflectiveQuadToRelative(113f, 47f)
                quadToRelative(47f, 47f, 47f, 113f)
                reflectiveQuadToRelative(-47f, 113f)
                quadToRelative(-47f, 47f, -113f, 47f)
                reflectiveQuadToRelative(-113f, -47f)
                close()
                moveTo(713f, 433f)
                quadToRelative(-47f, 47f, -113f, 47f)
                quadToRelative(-11f, 0f, -28f, -2.5f)
                reflectiveQuadToRelative(-28f, -5.5f)
                quadToRelative(27f, -32f, 41.5f, -71f)
                reflectiveQuadToRelative(14.5f, -81f)
                quadToRelative(0f, -42f, -14.5f, -81f)
                reflectiveQuadTo(544f, 168f)
                quadToRelative(14f, -5f, 28f, -6.5f)
                reflectiveQuadToRelative(28f, -1.5f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 66f, -47f, 113f)
                close()
                moveTo(120f, 720f)
                horizontalLineToRelative(480f)
                verticalLineToRelative(-32f)
                quadToRelative(0f, -11f, -5.5f, -20f)
                reflectiveQuadTo(580f, 654f)
                quadToRelative(-54f, -27f, -109f, -40.5f)
                reflectiveQuadTo(360f, 600f)
                quadToRelative(-56f, 0f, -111f, 13.5f)
                reflectiveQuadTo(140f, 654f)
                quadToRelative(-9f, 5f, -14.5f, 14f)
                reflectiveQuadToRelative(-5.5f, 20f)
                verticalLineToRelative(32f)
                close()
                moveTo(416.5f, 376.5f)
                quadTo(440f, 353f, 440f, 320f)
                reflectiveQuadToRelative(-23.5f, -56.5f)
                quadTo(393f, 240f, 360f, 240f)
                reflectiveQuadToRelative(-56.5f, 23.5f)
                quadTo(280f, 287f, 280f, 320f)
                reflectiveQuadToRelative(23.5f, 56.5f)
                quadTo(327f, 400f, 360f, 400f)
                reflectiveQuadToRelative(56.5f, -23.5f)
                close()
                moveTo(360f, 720f)
                close()
                moveTo(360f, 320f)
                close()
            }
        }.build()

        return _Group!!
    }

@Suppress("ObjectPropertyName")
private var _Group: ImageVector? = null
