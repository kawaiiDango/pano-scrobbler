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
val Icons.CalendarViewWeek: ImageVector
  get() {
    if (_CalendarViewWeek != null) {
      return _CalendarViewWeek!!
    }
    _CalendarViewWeek =
      ImageVector.Builder(
          name = "CalendarViewWeek",
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
            moveToRelative(9f, -2f)
            horizontalLineToRelative(2.5f)
            verticalLineTo(6f)
            horizontalLineTo(13f)
            verticalLineTo(18f)
            close()
            moveTo(8.5f, 18f)
            horizontalLineTo(11f)
            verticalLineTo(6f)
            horizontalLineTo(8.5f)
            verticalLineTo(18f)
            close()
            moveTo(4f, 18f)
            horizontalLineTo(6.5f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(18f)
            close()
            moveToRelative(13.5f, 0f)
            horizontalLineTo(20f)
            verticalLineTo(6f)
            horizontalLineTo(17.5f)
            verticalLineTo(18f)
            close()
          }
        }
        .build()
    return _CalendarViewWeek!!
  }

private var _CalendarViewWeek: ImageVector? = null
