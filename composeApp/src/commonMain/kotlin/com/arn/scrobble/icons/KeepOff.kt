package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val Icons.KeepOff: ImageVector
  get() {
    if (_KeepOff != null) {
      return _KeepOff!!
    }
    _KeepOff =
      ImageVector.Builder(
          name = "KeepOff",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
          ) {
            moveTo(14f, 5f)
            horizontalLineTo(10f)
            verticalLineTo(7.18f)
            lineTo(7.25f, 4.42f)
            quadTo(7.13f, 4.3f, 7.06f, 4.15f)
            reflectiveQuadTo(7f, 3.85f)
            quadTo(7f, 3.52f, 7.23f, 3.26f)
            reflectiveQuadTo(7.83f, 3f)
            horizontalLineTo(16f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 4f)
            quadToRelative(0f, 0.4f, -0.36f, 0.56f)
            quadTo(16.28f, 4.72f, 16f, 5f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(15f, 12f)
            reflectiveQuadTo(14.29f, 11.71f)
            quadTo(14f, 11.43f, 14f, 11f)
            verticalLineTo(5f)
            close()
            moveTo(11f, 22f)
            verticalLineTo(16f)
            horizontalLineTo(7.4f)
            quadToRelative(-0.63f, 0f, -1f, -0.44f)
            reflectiveQuadTo(6.03f, 14.58f)
            quadToRelative(0f, -0.28f, 0.11f, -0.55f)
            reflectiveQuadTo(6.5f, 13.5f)
            lineTo(8f, 12f)
            verticalLineTo(10.85f)
            lineTo(2.1f, 4.9f)
            quadTo(1.83f, 4.63f, 1.81f, 4.21f)
            reflectiveQuadTo(2.1f, 3.5f)
            quadTo(2.38f, 3.22f, 2.8f, 3.22f)
            reflectiveQuadTo(3.5f, 3.5f)
            lineTo(20.48f, 20.48f)
            quadToRelative(0.3f, 0.3f, 0.29f, 0.71f)
            reflectiveQuadTo(20.45f, 21.9f)
            quadToRelative(-0.3f, 0.28f, -0.7f, 0.29f)
            reflectiveQuadTo(19.05f, 21.9f)
            lineTo(13.15f, 16f)
            horizontalLineTo(13f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 23f)
            reflectiveQuadTo(11.29f, 22.71f)
            quadTo(11f, 22.43f, 11f, 22f)
            close()
            moveTo(8.85f, 14f)
            horizontalLineToRelative(2.3f)
            lineToRelative(-1.1f, -1.1f)
            lineTo(10f, 12.85f)
            lineTo(8.85f, 14f)
            close()
            moveTo(12f, 9.17f)
            close()
            moveTo(10.05f, 12.9f)
            close()
          }
        }
        .build()
    return _KeepOff!!
  }

private var _KeepOff: ImageVector? = null
