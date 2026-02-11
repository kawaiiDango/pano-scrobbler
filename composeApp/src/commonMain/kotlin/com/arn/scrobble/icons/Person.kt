package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Person: ImageVector
    get() {
        if (_Person != null) {
            return _Person!!
        }
        _Person = ImageVector.Builder(
            name = "Person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(367f, 433f)
                quadToRelative(-47f, -47f, -47f, -113f)
                reflectiveQuadToRelative(47f, -113f)
                quadToRelative(47f, -47f, 113f, -47f)
                reflectiveQuadToRelative(113f, 47f)
                quadToRelative(47f, 47f, 47f, 113f)
                reflectiveQuadToRelative(-47f, 113f)
                quadToRelative(-47f, 47f, -113f, 47f)
                reflectiveQuadToRelative(-113f, -47f)
                close()
                moveTo(160f, 720f)
                verticalLineToRelative(-32f)
                quadToRelative(0f, -34f, 17.5f, -62.5f)
                reflectiveQuadTo(224f, 582f)
                quadToRelative(62f, -31f, 126f, -46.5f)
                reflectiveQuadTo(480f, 520f)
                quadToRelative(66f, 0f, 130f, 15.5f)
                reflectiveQuadTo(736f, 582f)
                quadToRelative(29f, 15f, 46.5f, 43.5f)
                reflectiveQuadTo(800f, 688f)
                verticalLineToRelative(32f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(720f, 800f)
                lineTo(240f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(160f, 720f)
                close()
                moveTo(240f, 720f)
                horizontalLineToRelative(480f)
                verticalLineToRelative(-32f)
                quadToRelative(0f, -11f, -5.5f, -20f)
                reflectiveQuadTo(700f, 654f)
                quadToRelative(-54f, -27f, -109f, -40.5f)
                reflectiveQuadTo(480f, 600f)
                quadToRelative(-56f, 0f, -111f, 13.5f)
                reflectiveQuadTo(260f, 654f)
                quadToRelative(-9f, 5f, -14.5f, 14f)
                reflectiveQuadToRelative(-5.5f, 20f)
                verticalLineToRelative(32f)
                close()
                moveTo(536.5f, 376.5f)
                quadTo(560f, 353f, 560f, 320f)
                reflectiveQuadToRelative(-23.5f, -56.5f)
                quadTo(513f, 240f, 480f, 240f)
                reflectiveQuadToRelative(-56.5f, 23.5f)
                quadTo(400f, 287f, 400f, 320f)
                reflectiveQuadToRelative(23.5f, 56.5f)
                quadTo(447f, 400f, 480f, 400f)
                reflectiveQuadToRelative(56.5f, -23.5f)
                close()
                moveTo(480f, 320f)
                close()
                moveTo(480f, 720f)
                close()
            }
        }.build()

        return _Person!!
    }

@Suppress("ObjectPropertyName")
private var _Person: ImageVector? = null
