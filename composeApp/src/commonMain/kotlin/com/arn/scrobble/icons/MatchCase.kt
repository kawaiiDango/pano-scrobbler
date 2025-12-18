package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.MatchCase: ImageVector
    get() {
        if (_MatchCase != null) {
            return _MatchCase!!
        }
        _MatchCase = ImageVector.Builder(
            name = "MatchCase",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(247f, 596f)
                lineToRelative(-32f, 89f)
                quadToRelative(-4f, 11f, -13f, 17f)
                reflectiveQuadToRelative(-20f, 6f)
                quadToRelative(-19f, 0f, -29.5f, -15.5f)
                reflectiveQuadTo(149f, 660f)
                lineToRelative(138f, -368f)
                quadToRelative(4f, -11f, 13.5f, -17.5f)
                reflectiveQuadTo(321f, 268f)
                horizontalLineToRelative(28f)
                quadToRelative(11f, 0f, 21f, 6.5f)
                reflectiveQuadToRelative(14f, 17.5f)
                lineToRelative(138f, 369f)
                quadToRelative(7f, 17f, -4f, 32f)
                reflectiveQuadToRelative(-29f, 15f)
                quadToRelative(-11f, 0f, -20f, -6.5f)
                reflectiveQuadTo(456f, 684f)
                lineToRelative(-31f, -88f)
                lineTo(247f, 596f)
                close()
                moveTo(270f, 532f)
                horizontalLineToRelative(131f)
                lineToRelative(-64f, -182f)
                horizontalLineToRelative(-4f)
                lineToRelative(-63f, 182f)
                close()
                moveTo(665f, 718f)
                quadToRelative(-51f, 0f, -81f, -27.5f)
                reflectiveQuadTo(554f, 618f)
                quadToRelative(0f, -44f, 34.5f, -72.5f)
                reflectiveQuadTo(677f, 517f)
                quadToRelative(23f, 0f, 45f, 4f)
                reflectiveQuadToRelative(38f, 11f)
                verticalLineToRelative(-12f)
                quadToRelative(0f, -29f, -20.5f, -47f)
                reflectiveQuadTo(685f, 455f)
                quadToRelative(-15f, 0f, -29.5f, 4.5f)
                reflectiveQuadTo(629f, 473f)
                quadToRelative(-13f, 10f, -24.5f, 7f)
                reflectiveQuadTo(586f, 469f)
                quadToRelative(-7f, -8f, -7f, -19f)
                reflectiveQuadToRelative(11f, -19f)
                quadToRelative(20f, -16f, 45f, -23.5f)
                reflectiveQuadToRelative(51f, -7.5f)
                quadToRelative(69f, 0f, 103f, 32.5f)
                reflectiveQuadToRelative(34f, 97.5f)
                verticalLineToRelative(147f)
                quadToRelative(0f, 13f, -9.5f, 22f)
                reflectiveQuadToRelative(-22.5f, 9f)
                quadToRelative(-13f, 0f, -22f, -9.5f)
                reflectiveQuadToRelative(-9f, -22.5f)
                verticalLineToRelative(-5f)
                horizontalLineToRelative(-4f)
                quadToRelative(-14f, 23f, -38f, 35f)
                reflectiveQuadToRelative(-53f, 12f)
                close()
                moveTo(677f, 664f)
                quadToRelative(35f, 0f, 59.5f, -24f)
                reflectiveQuadToRelative(24.5f, -56f)
                quadToRelative(-14f, -8f, -33.5f, -12.5f)
                reflectiveQuadTo(689f, 567f)
                quadToRelative(-32f, 0f, -50f, 14f)
                reflectiveQuadToRelative(-18f, 37f)
                quadToRelative(0f, 20f, 16f, 33f)
                reflectiveQuadToRelative(40f, 13f)
                close()
            }
        }.build()

        return _MatchCase!!
    }

@Suppress("ObjectPropertyName")
private var _MatchCase: ImageVector? = null
