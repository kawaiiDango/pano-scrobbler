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
val Icons.ExpandCircleDownFilled: ImageVector
  get() {
    if (_ExpandCircleDownFilled != null) {
      return _ExpandCircleDownFilled!!
    }
    _ExpandCircleDownFilled =
      ImageVector.Builder(
          name = "ExpandCircleDownFilled",
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
            moveTo(12f, 12.68f)
            lineTo(9.63f, 10.3f)
            quadTo(9.35f, 10.02f, 8.94f, 10.02f)
            reflectiveQuadTo(8.23f, 10.3f)
            quadToRelative(-0.3f, 0.3f, -0.3f, 0.71f)
            reflectiveQuadToRelative(0.3f, 0.71f)
            lineTo(11.3f, 14.8f)
            quadToRelative(0.3f, 0.3f, 0.7f, 0.3f)
            reflectiveQuadToRelative(0.7f, -0.3f)
            lineToRelative(3.1f, -3.1f)
            quadTo(16.1f, 11.4f, 16.09f, 11f)
            reflectiveQuadTo(15.78f, 10.3f)
            quadToRelative(-0.3f, -0.28f, -0.7f, -0.29f)
            reflectiveQuadToRelative(-0.7f, 0.29f)
            lineTo(12f, 12.68f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _ExpandCircleDownFilled!!
  }

private var _ExpandCircleDownFilled: ImageVector? = null
