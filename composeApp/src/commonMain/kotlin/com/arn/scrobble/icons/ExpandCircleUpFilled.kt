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
val Icons.ExpandCircleUpFilled: ImageVector
  get() {
    if (_ExpandCircleUpFilled != null) {
      return _ExpandCircleUpFilled!!
    }
    _ExpandCircleUpFilled =
      ImageVector.Builder(
          name = "ExpandCircleUpFilled",
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
            moveTo(12f, 11.33f)
            lineToRelative(2.38f, 2.38f)
            quadToRelative(0.28f, 0.28f, 0.69f, 0.28f)
            reflectiveQuadTo(15.78f, 13.7f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.71f)
            reflectiveQuadToRelative(-0.3f, -0.71f)
            lineTo(12.7f, 9.2f)
            quadTo(12.4f, 8.9f, 12f, 8.9f)
            reflectiveQuadTo(11.3f, 9.2f)
            lineTo(8.2f, 12.3f)
            quadTo(7.9f, 12.6f, 7.91f, 13f)
            reflectiveQuadToRelative(0.31f, 0.7f)
            quadToRelative(0.3f, 0.28f, 0.7f, 0.29f)
            reflectiveQuadTo(9.63f, 13.7f)
            lineTo(12f, 11.33f)
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
    return _ExpandCircleUpFilled!!
  }

private var _ExpandCircleUpFilled: ImageVector? = null
