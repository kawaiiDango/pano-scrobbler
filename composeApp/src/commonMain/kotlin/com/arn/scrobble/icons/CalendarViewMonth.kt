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
val Icons.CalendarViewMonth: ImageVector
  get() {
    if (_CalendarViewMonth != null) {
      return _CalendarViewMonth!!
    }
    _CalendarViewMonth =
      ImageVector.Builder(
          name = "CalendarViewMonth",
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
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 11f)
            horizontalLineTo(8f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineToRelative(5f)
            close()
            moveToRelative(6f, 0f)
            horizontalLineToRelative(4f)
            verticalLineTo(6f)
            horizontalLineTo(10f)
            verticalLineToRelative(5f)
            close()
            moveToRelative(6f, 0f)
            horizontalLineToRelative(4f)
            verticalLineTo(6f)
            horizontalLineTo(16f)
            verticalLineToRelative(5f)
            close()
            moveTo(8f, 18f)
            verticalLineTo(13f)
            horizontalLineTo(4f)
            verticalLineToRelative(5f)
            horizontalLineTo(8f)
            close()
            moveToRelative(2f, 0f)
            horizontalLineToRelative(4f)
            verticalLineTo(13f)
            horizontalLineTo(10f)
            verticalLineToRelative(5f)
            close()
            moveToRelative(6f, 0f)
            horizontalLineToRelative(4f)
            verticalLineTo(13f)
            horizontalLineTo(16f)
            verticalLineToRelative(5f)
            close()
          }
        }
        .build()
    return _CalendarViewMonth!!
  }

private var _CalendarViewMonth: ImageVector? = null
