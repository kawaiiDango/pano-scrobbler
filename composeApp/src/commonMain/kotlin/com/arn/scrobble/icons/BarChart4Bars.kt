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
val Icons.BarChart4Bars: ImageVector
  get() {
    if (_BarChart4Bars != null) {
      return _BarChart4Bars!!
    }
    _BarChart4Bars =
      ImageVector.Builder(
          name = "BarChart4Bars",
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
            moveTo(3f, 21f)
            quadTo(2.58f, 21f, 2.29f, 20.71f)
            quadTo(2f, 20.43f, 2f, 20f)
            reflectiveQuadTo(2.29f, 19.29f)
            reflectiveQuadTo(3f, 19f)
            horizontalLineTo(21f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 20f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(21f, 21f)
            horizontalLineTo(3f)
            close()
            moveTo(3.44f, 17.56f)
            quadTo(3f, 17.13f, 3f, 16.5f)
            verticalLineToRelative(-4f)
            quadTo(3f, 11.88f, 3.44f, 11.44f)
            reflectiveQuadTo(4.5f, 11f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(6f, 12.5f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.63f, -0.44f, 1.06f)
            reflectiveQuadTo(4.5f, 18f)
            reflectiveQuadTo(3.44f, 17.56f)
            close()
            moveToRelative(5f, 0f)
            quadTo(8f, 17.13f, 8f, 16.5f)
            verticalLineToRelative(-9f)
            quadTo(8f, 6.88f, 8.44f, 6.44f)
            reflectiveQuadTo(9.5f, 6f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(11f, 7.5f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 0.63f, -0.44f, 1.06f)
            reflectiveQuadTo(9.5f, 18f)
            reflectiveQuadTo(8.44f, 17.56f)
            close()
            moveToRelative(5f, 0f)
            quadTo(13f, 17.13f, 13f, 16.5f)
            verticalLineToRelative(-6f)
            quadTo(13f, 9.88f, 13.44f, 9.44f)
            reflectiveQuadTo(14.5f, 9f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(16f, 10.5f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.63f, -0.44f, 1.06f)
            reflectiveQuadTo(14.5f, 18f)
            reflectiveQuadTo(13.44f, 17.56f)
            close()
            moveToRelative(5f, 0f)
            quadTo(18f, 17.13f, 18f, 16.5f)
            verticalLineTo(4.5f)
            quadTo(18f, 3.88f, 18.44f, 3.44f)
            reflectiveQuadTo(19.5f, 3f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(21f, 4.5f)
            verticalLineToRelative(12f)
            quadToRelative(0f, 0.63f, -0.44f, 1.06f)
            reflectiveQuadTo(19.5f, 18f)
            reflectiveQuadTo(18.44f, 17.56f)
            close()
          }
        }
        .build()
    return _BarChart4Bars!!
  }

private var _BarChart4Bars: ImageVector? = null
