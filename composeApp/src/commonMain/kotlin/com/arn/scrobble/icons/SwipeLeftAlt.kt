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
val Icons.SwipeLeftAlt: ImageVector
  get() {
    if (_SwipeLeftAlt != null) {
      return _SwipeLeftAlt!!
    }
    _SwipeLeftAlt =
      ImageVector.Builder(
          name = "SwipeLeftAlt",
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
            moveTo(15f, 17f)
            quadToRelative(-1.82f, 0f, -3.19f, -1.14f)
            quadTo(10.45f, 14.73f, 10.1f, 13f)
            horizontalLineTo(5.83f)
            lineToRelative(0.9f, 0.9f)
            quadTo(7f, 14.18f, 7f, 14.59f)
            reflectiveQuadTo(6.7f, 15.3f)
            quadTo(6.43f, 15.58f, 6f, 15.58f)
            quadToRelative(-0.42f, 0f, -0.7f, -0.28f)
            lineTo(2.7f, 12.7f)
            quadTo(2.55f, 12.55f, 2.49f, 12.38f)
            reflectiveQuadTo(2.43f, 12f)
            reflectiveQuadTo(2.49f, 11.63f)
            reflectiveQuadTo(2.7f, 11.3f)
            lineTo(5.3f, 8.7f)
            quadTo(5.58f, 8.42f, 5.99f, 8.42f)
            reflectiveQuadTo(6.7f, 8.7f)
            quadTo(7f, 9f, 7f, 9.41f)
            reflectiveQuadTo(6.7f, 10.13f)
            lineTo(5.83f, 11f)
            horizontalLineTo(10.1f)
            quadTo(10.45f, 9.27f, 11.81f, 8.14f)
            reflectiveQuadTo(15f, 7f)
            quadToRelative(2.07f, 0f, 3.54f, 1.46f)
            reflectiveQuadTo(20f, 12f)
            reflectiveQuadToRelative(-1.46f, 3.54f)
            reflectiveQuadTo(15f, 17f)
            close()
            moveToRelative(2.13f, -2.88f)
            quadTo(18f, 13.25f, 18f, 12f)
            reflectiveQuadTo(17.13f, 9.88f)
            reflectiveQuadTo(15f, 9f)
            reflectiveQuadTo(12.88f, 9.88f)
            reflectiveQuadTo(12f, 12f)
            reflectiveQuadToRelative(0.88f, 2.13f)
            reflectiveQuadTo(15f, 15f)
            reflectiveQuadToRelative(2.13f, -0.88f)
            close()
            moveTo(15f, 12f)
            close()
          }
        }
        .build()
    return _SwipeLeftAlt!!
  }

private var _SwipeLeftAlt: ImageVector? = null
