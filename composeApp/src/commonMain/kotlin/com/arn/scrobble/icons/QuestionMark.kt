package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.QuestionMark: ImageVector
    get() {
        if (_QuestionMark != null) {
            return _QuestionMark!!
        }
        _QuestionMark = ImageVector.Builder(
            name = "QuestionMark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(584f, 323f)
                quadToRelative(0f, -43f, -28.5f, -69f)
                reflectiveQuadTo(480f, 228f)
                quadToRelative(-29f, 0f, -52.5f, 12.5f)
                reflectiveQuadTo(387f, 277f)
                quadToRelative(-16f, 23f, -43.5f, 26.5f)
                reflectiveQuadTo(296f, 289f)
                quadToRelative(-14f, -13f, -15.5f, -32f)
                reflectiveQuadToRelative(9.5f, -36f)
                quadToRelative(32f, -48f, 81.5f, -74.5f)
                reflectiveQuadTo(480f, 120f)
                quadToRelative(97f, 0f, 157.5f, 55f)
                reflectiveQuadTo(698f, 319f)
                quadToRelative(0f, 45f, -19f, 81f)
                reflectiveQuadToRelative(-70f, 85f)
                quadToRelative(-37f, 35f, -50f, 54.5f)
                reflectiveQuadTo(542f, 584f)
                quadToRelative(-4f, 24f, -20.5f, 40f)
                reflectiveQuadTo(482f, 640f)
                quadToRelative(-23f, 0f, -39.5f, -15.5f)
                reflectiveQuadTo(426f, 586f)
                quadToRelative(0f, -39f, 17f, -71.5f)
                reflectiveQuadToRelative(57f, -68.5f)
                quadToRelative(51f, -45f, 67.5f, -69.5f)
                reflectiveQuadTo(584f, 323f)
                close()
                moveTo(480f, 880f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(400f, 800f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(480f, 720f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(560f, 800f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(480f, 880f)
                close()
            }
        }.build()

        return _QuestionMark!!
    }

@Suppress("ObjectPropertyName")
private var _QuestionMark: ImageVector? = null
