package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PanoIcons.ContentSaveOffOutline: ImageVector
    get() {
        if (_ContentSaveOffOutline != null) {
            return _ContentSaveOffOutline!!
        }
        _ContentSaveOffOutline = ImageVector.Builder(
            name = "ContentSaveOffOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.2f, 5f)
                lineTo(6.2f, 3f)
                horizontalLineTo(17f)
                lineTo(21f, 7f)
                verticalLineTo(17.8f)
                lineTo(19f, 15.8f)
                verticalLineTo(7.83f)
                lineTo(16.17f, 5f)
                horizontalLineTo(8.2f)
                moveTo(15f, 10f)
                verticalLineTo(6f)
                horizontalLineTo(9.2f)
                lineTo(13.2f, 10f)
                horizontalLineTo(15f)
                moveTo(22.11f, 21.46f)
                lineTo(20.84f, 22.73f)
                lineTo(19.1f, 21f)
                curveTo(19.07f, 21f, 19.03f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.89f, 21f, 3f, 20.1f, 3f, 19f)
                verticalLineTo(5f)
                curveTo(3f, 4.97f, 3f, 4.93f, 3f, 4.9f)
                lineTo(1.11f, 3f)
                lineTo(2.39f, 1.73f)
                lineTo(22.11f, 21.46f)
                moveTo(17.11f, 19f)
                lineTo(14.59f, 16.5f)
                curveTo(14.08f, 17.39f, 13.12f, 18f, 12f, 18f)
                curveTo(10.34f, 18f, 9f, 16.66f, 9f, 15f)
                curveTo(9f, 13.88f, 9.61f, 12.92f, 10.5f, 12.41f)
                lineTo(8.11f, 10f)
                horizontalLineTo(6f)
                verticalLineTo(7.89f)
                lineTo(5f, 6.89f)
                verticalLineTo(19f)
                horizontalLineTo(17.11f)
                close()
            }
        }.build()

        return _ContentSaveOffOutline!!
    }

@Suppress("ObjectPropertyName")
private var _ContentSaveOffOutline: ImageVector? = null
