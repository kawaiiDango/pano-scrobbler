package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Translate: ImageVector
    get() {
        if (_Translate != null) {
            return _Translate!!
        }
        _Translate = ImageVector.Builder(
            name = "Translate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(603f, 758f)
                lineToRelative(-34f, 97f)
                quadToRelative(-4f, 11f, -14f, 18f)
                reflectiveQuadToRelative(-22f, 7f)
                quadToRelative(-20f, 0f, -32.5f, -16.5f)
                reflectiveQuadTo(496f, 827f)
                lineToRelative(152f, -402f)
                quadToRelative(5f, -11f, 15f, -18f)
                reflectiveQuadToRelative(22f, -7f)
                horizontalLineToRelative(30f)
                quadToRelative(12f, 0f, 22f, 7f)
                reflectiveQuadToRelative(15f, 18f)
                lineToRelative(152f, 403f)
                quadToRelative(8f, 19f, -4f, 35.5f)
                reflectiveQuadTo(868f, 880f)
                quadToRelative(-13f, 0f, -22.5f, -7f)
                reflectiveQuadTo(831f, 854f)
                lineToRelative(-34f, -96f)
                lineTo(603f, 758f)
                close()
                moveTo(362f, 559f)
                lineTo(188f, 732f)
                quadToRelative(-11f, 11f, -27.5f, 11.5f)
                reflectiveQuadTo(132f, 732f)
                quadToRelative(-11f, -11f, -11f, -28f)
                reflectiveQuadToRelative(11f, -28f)
                lineToRelative(174f, -174f)
                quadToRelative(-35f, -35f, -63.5f, -80f)
                reflectiveQuadTo(190f, 320f)
                horizontalLineToRelative(84f)
                quadToRelative(20f, 39f, 40f, 68f)
                reflectiveQuadToRelative(48f, 58f)
                quadToRelative(33f, -33f, 68.5f, -92.5f)
                reflectiveQuadTo(484f, 240f)
                lineTo(80f, 240f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(40f, 200f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(80f, 160f)
                horizontalLineToRelative(240f)
                verticalLineToRelative(-40f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(360f, 80f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(400f, 120f)
                verticalLineToRelative(40f)
                horizontalLineToRelative(240f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(680f, 200f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(640f, 240f)
                horizontalLineToRelative(-76f)
                quadToRelative(-21f, 72f, -63f, 148f)
                reflectiveQuadToRelative(-83f, 116f)
                lineToRelative(96f, 98f)
                lineToRelative(-30f, 82f)
                lineToRelative(-122f, -125f)
                close()
                moveTo(628f, 688f)
                horizontalLineToRelative(144f)
                lineToRelative(-72f, -204f)
                lineToRelative(-72f, 204f)
                close()
            }
        }.build()

        return _Translate!!
    }

@Suppress("ObjectPropertyName")
private var _Translate: ImageVector? = null
