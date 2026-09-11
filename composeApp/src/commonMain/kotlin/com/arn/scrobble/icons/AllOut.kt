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
val Icons.AllOut: ImageVector
  get() {
    if (_AllOut != null) {
      return _AllOut!!
    }
    _AllOut =
      ImageVector.Builder(
          name = "AllOut",
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
            moveTo(7f, 21f)
            horizontalLineTo(5f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(17f)
            quadTo(3f, 16.58f, 3.29f, 16.29f)
            reflectiveQuadTo(4f, 16f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(5f, 17f)
            verticalLineToRelative(2f)
            horizontalLineTo(7f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(8f, 20f)
            reflectiveQuadTo(7.71f, 20.71f)
            reflectiveQuadTo(7f, 21f)
            close()
            moveToRelative(12f, 0f)
            horizontalLineTo(17f)
            quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
            quadTo(16f, 20.43f, 16f, 20f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(17f, 19f)
            horizontalLineToRelative(2f)
            verticalLineTo(17f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(20f, 16f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 17f)
            verticalLineToRelative(2f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            close()
            moveTo(7.05f, 16.95f)
            quadTo(5f, 14.9f, 5f, 12f)
            reflectiveQuadTo(7.05f, 7.05f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadToRelative(4.95f, 2.05f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadToRelative(-2.05f, 4.95f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadTo(7.05f, 16.95f)
            close()
            moveTo(12f, 17f)
            quadToRelative(2.08f, 0f, 3.54f, -1.46f)
            reflectiveQuadTo(17f, 12f)
            quadTo(17f, 9.92f, 15.54f, 8.46f)
            reflectiveQuadTo(12f, 7f)
            quadTo(9.93f, 7f, 8.46f, 8.46f)
            reflectiveQuadTo(7f, 12f)
            reflectiveQuadToRelative(1.46f, 3.54f)
            reflectiveQuadTo(12f, 17f)
            close()
            moveTo(3f, 5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(7f)
            quadTo(7.43f, 3f, 7.71f, 3.29f)
            reflectiveQuadTo(8f, 4f)
            quadTo(8f, 4.42f, 7.71f, 4.71f)
            reflectiveQuadTo(7f, 5f)
            horizontalLineTo(5f)
            verticalLineTo(7f)
            quadTo(5f, 7.43f, 4.71f, 7.71f)
            reflectiveQuadTo(4f, 8f)
            reflectiveQuadTo(3.29f, 7.71f)
            quadTo(3f, 7.43f, 3f, 7f)
            verticalLineTo(5f)
            close()
            moveTo(19.29f, 7.71f)
            quadTo(19f, 7.43f, 19f, 7f)
            verticalLineTo(5f)
            horizontalLineTo(17f)
            quadTo(16.58f, 5f, 16.29f, 4.71f)
            reflectiveQuadTo(16f, 4f)
            quadTo(16f, 3.57f, 16.29f, 3.29f)
            reflectiveQuadTo(17f, 3f)
            horizontalLineToRelative(2f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(7f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 8f)
            reflectiveQuadTo(19.29f, 7.71f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build()
    return _AllOut!!
  }

private var _AllOut: ImageVector? = null
