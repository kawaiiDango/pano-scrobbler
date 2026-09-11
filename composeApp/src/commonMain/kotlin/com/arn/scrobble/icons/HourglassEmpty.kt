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
val Icons.HourglassEmpty: ImageVector
  get() {
    if (_HourglassEmpty != null) {
      return _HourglassEmpty!!
    }
    _HourglassEmpty =
      ImageVector.Builder(
          name = "HourglassEmpty",
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
            moveTo(8f, 20f)
            horizontalLineToRelative(8f)
            verticalLineTo(17f)
            quadToRelative(0f, -1.65f, -1.17f, -2.83f)
            reflectiveQuadTo(12f, 13f)
            reflectiveQuadTo(9.18f, 14.18f)
            reflectiveQuadTo(8f, 17f)
            verticalLineToRelative(3f)
            close()
            moveTo(14.83f, 9.82f)
            quadTo(16f, 8.65f, 16f, 7f)
            verticalLineTo(4f)
            horizontalLineTo(8f)
            verticalLineTo(7f)
            quadTo(8f, 8.65f, 9.18f, 9.82f)
            reflectiveQuadTo(12f, 11f)
            reflectiveQuadTo(14.83f, 9.82f)
            close()
            moveTo(5f, 22f)
            quadTo(4.58f, 22f, 4.29f, 21.71f)
            quadTo(4f, 21.43f, 4f, 21f)
            reflectiveQuadTo(4.29f, 20.29f)
            reflectiveQuadTo(5f, 20f)
            horizontalLineTo(6f)
            verticalLineTo(17f)
            quadTo(6f, 15.48f, 6.71f, 14.14f)
            reflectiveQuadTo(8.7f, 12f)
            quadTo(7.43f, 11.2f, 6.71f, 9.86f)
            reflectiveQuadTo(6f, 7f)
            verticalLineTo(4f)
            horizontalLineTo(5f)
            quadTo(4.58f, 4f, 4.29f, 3.71f)
            reflectiveQuadTo(4f, 3f)
            quadTo(4f, 2.57f, 4.29f, 2.29f)
            reflectiveQuadTo(5f, 2f)
            horizontalLineTo(19f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 3f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(19f, 4f)
            horizontalLineTo(18f)
            verticalLineTo(7f)
            quadToRelative(0f, 1.52f, -0.71f, 2.86f)
            reflectiveQuadTo(15.3f, 12f)
            quadToRelative(1.27f, 0.8f, 1.99f, 2.14f)
            reflectiveQuadTo(18f, 17f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(1f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 21f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(19f, 22f)
            horizontalLineTo(5f)
            close()
          }
        }
        .build()
    return _HourglassEmpty!!
  }

private var _HourglassEmpty: ImageVector? = null
