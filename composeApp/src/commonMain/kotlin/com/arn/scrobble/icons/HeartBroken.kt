package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.HeartBroken: ImageVector
    get() {
        if (_HeartBroken != null) {
            return _HeartBroken!!
        }
        _HeartBroken = ImageVector.Builder(
            name = "HeartBroken",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 340f)
                quadToRelative(0f, 30f, 10.5f, 57f)
                reflectiveQuadToRelative(37.5f, 61.5f)
                quadToRelative(27f, 34.5f, 73f, 82.5f)
                reflectiveQuadToRelative(117f, 120f)
                quadToRelative(4f, 4f, 8.5f, 2.5f)
                reflectiveQuadToRelative(5.5f, -6.5f)
                lineToRelative(19f, -177f)
                quadToRelative(-52f, 0f, -83f, -40.5f)
                reflectiveQuadTo(331f, 350f)
                lineToRelative(38f, -133f)
                quadToRelative(-16f, -8f, -33.5f, -12.5f)
                reflectiveQuadTo(300f, 200f)
                quadToRelative(-58f, 0f, -99f, 41f)
                reflectiveQuadToRelative(-41f, 99f)
                close()
                moveTo(800f, 340f)
                quadToRelative(0f, -58f, -41f, -99f)
                reflectiveQuadToRelative(-99f, -41f)
                quadToRelative(-11f, 0f, -22f, 1.5f)
                reflectiveQuadToRelative(-22f, 5.5f)
                lineToRelative(-24f, 73f)
                quadToRelative(45f, 12f, 66.5f, 53.5f)
                reflectiveQuadTo(666f, 420f)
                lineToRelative(-52f, 175f)
                quadToRelative(-2f, 5f, 3.5f, 8.5f)
                reflectiveQuadTo(628f, 602f)
                quadToRelative(55f, -56f, 89f, -93.5f)
                reflectiveQuadToRelative(52.5f, -66f)
                quadTo(788f, 414f, 794f, 391f)
                reflectiveQuadToRelative(6f, -51f)
                close()
                moveTo(666f, 420f)
                close()
                moveTo(331f, 350f)
                close()
                moveTo(453.5f, 809f)
                quadToRelative(-13.5f, -5f, -24.5f, -15f)
                quadToRelative(-109f, -96f, -177f, -161.5f)
                reflectiveQuadToRelative(-106.5f, -115f)
                quadTo(107f, 468f, 93.5f, 427f)
                reflectiveQuadTo(80f, 340f)
                quadToRelative(0f, -92f, 64f, -156f)
                reflectiveQuadToRelative(156f, -64f)
                quadToRelative(66f, 0f, 116f, 25.5f)
                reflectiveQuadToRelative(36f, 73.5f)
                lineToRelative(-37f, 130f)
                quadToRelative(-5f, 20f, 6.5f, 35.5f)
                reflectiveQuadTo(453f, 400f)
                horizontalLineToRelative(67f)
                lineToRelative(-26f, 254f)
                quadToRelative(-1f, 8f, 6.5f, 9f)
                reflectiveQuadToRelative(9.5f, -6f)
                lineToRelative(74f, -245f)
                quadToRelative(6f, -20f, -6f, -36f)
                reflectiveQuadToRelative(-32f, -16f)
                horizontalLineToRelative(-66f)
                lineToRelative(61f, -181f)
                quadToRelative(11f, -35f, 46f, -47f)
                reflectiveQuadToRelative(73f, -12f)
                quadToRelative(92f, 0f, 156f, 64f)
                reflectiveQuadToRelative(64f, 156f)
                quadToRelative(0f, 44f, -16f, 87f)
                reflectiveQuadToRelative(-55.5f, 95f)
                quadToRelative(-39.5f, 52f, -106f, 117.5f)
                reflectiveQuadTo(534f, 794f)
                quadToRelative(-11f, 10f, -25f, 15f)
                reflectiveQuadToRelative(-28f, 5f)
                quadToRelative(-14f, 0f, -27.5f, -5f)
                close()
            }
        }.build()

        return _HeartBroken!!
    }

@Suppress("ObjectPropertyName")
private var _HeartBroken: ImageVector? = null
