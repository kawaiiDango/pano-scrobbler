package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.WorkspacePremium: ImageVector
    get() {
        if (_WorkspacePremium != null) {
            return _WorkspacePremium!!
        }
        _WorkspacePremium = ImageVector.Builder(
            name = "WorkspacePremium",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(480f, 477f)
                lineToRelative(-68f, 52f)
                quadToRelative(-6f, 5f, -12f, 0.5f)
                reflectiveQuadToRelative(-4f, -11.5f)
                lineToRelative(26f, -84f)
                lineToRelative(-70f, -56f)
                quadToRelative(-5f, -5f, -3f, -11.5f)
                reflectiveQuadToRelative(9f, -6.5f)
                horizontalLineToRelative(86f)
                lineToRelative(26f, -82f)
                quadToRelative(2f, -7f, 10f, -7f)
                reflectiveQuadToRelative(10f, 7f)
                lineToRelative(26f, 82f)
                horizontalLineToRelative(85f)
                quadToRelative(7f, 0f, 9.5f, 6.5f)
                reflectiveQuadTo(608f, 378f)
                lineToRelative(-71f, 56f)
                lineToRelative(26f, 84f)
                quadToRelative(2f, 7f, -4f, 11.5f)
                reflectiveQuadToRelative(-12f, -0.5f)
                lineToRelative(-67f, -52f)
                close()
                moveTo(480f, 840f)
                lineTo(293f, 902f)
                quadToRelative(-20f, 7f, -36.5f, -5f)
                reflectiveQuadTo(240f, 865f)
                verticalLineToRelative(-254f)
                quadToRelative(-38f, -42f, -59f, -96f)
                reflectiveQuadToRelative(-21f, -115f)
                quadToRelative(0f, -134f, 93f, -227f)
                reflectiveQuadToRelative(227f, -93f)
                quadToRelative(134f, 0f, 227f, 93f)
                reflectiveQuadToRelative(93f, 227f)
                quadToRelative(0f, 61f, -21f, 115f)
                reflectiveQuadToRelative(-59f, 96f)
                verticalLineToRelative(254f)
                quadToRelative(0f, 20f, -16.5f, 32f)
                reflectiveQuadTo(667f, 902f)
                lineToRelative(-187f, -62f)
                close()
                moveTo(650f, 570f)
                quadToRelative(70f, -70f, 70f, -170f)
                reflectiveQuadToRelative(-70f, -170f)
                quadToRelative(-70f, -70f, -170f, -70f)
                reflectiveQuadToRelative(-170f, 70f)
                quadToRelative(-70f, 70f, -70f, 170f)
                reflectiveQuadToRelative(70f, 170f)
                quadToRelative(70f, 70f, 170f, 70f)
                reflectiveQuadToRelative(170f, -70f)
                close()
                moveTo(320f, 801f)
                lineToRelative(160f, -41f)
                lineToRelative(160f, 41f)
                verticalLineToRelative(-124f)
                quadToRelative(-35f, 20f, -75.5f, 31.5f)
                reflectiveQuadTo(480f, 720f)
                quadToRelative(-44f, 0f, -84.5f, -11.5f)
                reflectiveQuadTo(320f, 677f)
                verticalLineToRelative(124f)
                close()
                moveTo(480f, 739f)
                close()
            }
        }.build()

        return _WorkspacePremium!!
    }

@Suppress("ObjectPropertyName")
private var _WorkspacePremium: ImageVector? = null
