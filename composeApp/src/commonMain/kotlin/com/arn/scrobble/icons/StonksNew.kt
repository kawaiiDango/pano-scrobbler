package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PanoIcons.StonksNew: ImageVector
    get() {
        if (_StonksNew != null) {
            return _StonksNew!!
        }
        _StonksNew = ImageVector.Builder(
            name = "StonksNew",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                pivotX = 12.0f,
                pivotY = 12.0f,
                scaleX = 0.7f,
                scaleY = 0.7f,
            ) {
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
                    moveTo(12f, 1f)
                    lineTo(9f, 9f)
                    lineTo(1f, 12f)
                    lineTo(9f, 15f)
                    lineTo(12f, 23f)
                    lineTo(15f, 15f)
                    lineTo(23f, 12f)
                    lineTo(15f, 9f)
                    lineTo(12f, 1f)
                    close()
                }
            }
        }.build()
        return _StonksNew!!
    }

private var _StonksNew: ImageVector? = null
