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
val Icons.Stop: ImageVector
  get() {
    if (_Stop != null) {
      return _Stop!!
    }
    _Stop =
      ImageVector.Builder(
          name = "Stop",
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
            moveTo(6f, 16f)
            verticalLineTo(8f)
            quadTo(6f, 7.18f, 6.59f, 6.59f)
            reflectiveQuadTo(8f, 6f)
            horizontalLineToRelative(8f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            quadTo(18f, 7.18f, 18f, 8f)
            verticalLineToRelative(8f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(16f, 18f)
            horizontalLineTo(8f)
            quadTo(7.18f, 18f, 6.59f, 17.41f)
            reflectiveQuadTo(6f, 16f)
            close()
            moveToRelative(2f, 0f)
            horizontalLineToRelative(8f)
            verticalLineTo(8f)
            horizontalLineTo(8f)
            verticalLineToRelative(8f)
            close()
            moveToRelative(4f, -4f)
            close()
          }
        }
        .build()
    return _Stop!!
  }

private var _Stop: ImageVector? = null
