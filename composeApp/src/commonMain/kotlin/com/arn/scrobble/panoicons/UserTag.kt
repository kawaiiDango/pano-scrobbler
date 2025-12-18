package com.arn.scrobble.panoicons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PanoIcons.UserTag: ImageVector
    get() {
        if (_UserTag != null) {
            return _UserTag!!
        }
        _UserTag = ImageVector.Builder(
            name = "UserTag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16.5f, 14.5f)
                curveTo(14.66f, 14.5f, 11f, 15.51f, 11f, 17.5f)
                lineTo(11f, 19f)
                lineTo(22f, 19f)
                lineTo(22f, 17.5f)
                curveTo(22f, 15.51f, 18.34f, 14.5f, 16.5f, 14.5f)
                close()
                moveTo(17.71f, 12.68f)
                curveTo(18.47f, 12.25f, 19f, 11.44f, 19f, 10.5f)
                curveTo(19f, 9.12f, 17.88f, 8f, 16.5f, 8f)
                curveTo(15.12f, 8f, 14f, 9.12f, 14f, 10.5f)
                curveTo(14f, 11.44f, 14.53f, 12.25f, 15.29f, 12.68f)
                curveTo(15.65f, 12.88f, 16.06f, 13f, 16.5f, 13f)
                curveTo(16.94f, 13f, 17.35f, 12.88f, 17.71f, 12.68f)
                close()
                moveTo(4f, 2f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                lineTo(2f, 11f)
                curveTo(2f, 11.55f, 2.2198f, 12.0499f, 2.5898f, 12.4199f)
                lineTo(8.9121f, 18.7422f)
                curveTo(9.502f, 17.4034f, 8.9351f, 15.9712f, 8.1367f, 15.1426f)
                lineTo(4f, 11f)
                lineTo(4f, 4f)
                lineTo(11f, 4f)
                lineTo(11f, 3.9902f)
                lineTo(13.5972f, 6.5771f)
                curveTo(14.3768f, 7.2064f, 15.6155f, 6.7728f, 16.4041f, 6.6047f)
                lineTo(12.4102f, 2.5801f)
                curveTo(12.0515f, 2.2187f, 11.55f, 2f, 11f, 2f)
                close()
                moveTo(8f, 6.5f)
                curveTo(8f, 7.3284f, 7.3284f, 8f, 6.5f, 8f)
                curveTo(5.6716f, 8f, 5f, 7.3284f, 5f, 6.5f)
                curveTo(5f, 5.6716f, 5.6716f, 5f, 6.5f, 5f)
                curveTo(7.3284f, 5f, 8f, 5.6716f, 8f, 6.5f)
                close()
            }
        }.build()
        return _UserTag!!
    }

private var _UserTag: ImageVector? = null
