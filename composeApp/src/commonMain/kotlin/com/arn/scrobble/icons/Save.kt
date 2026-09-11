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
val Icons.Save: ImageVector
  get() {
    if (_Save != null) {
      return _Save!!
    }
    _Save =
      ImageVector.Builder(
          name = "Save",
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
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(16.18f)
            quadToRelative(0.4f, 0f, 0.76f, 0.15f)
            reflectiveQuadToRelative(0.64f, 0.43f)
            lineToRelative(2.85f, 2.85f)
            quadTo(20.7f, 6.7f, 20.85f, 7.06f)
            reflectiveQuadTo(21f, 7.82f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(19f, 7.85f)
            lineTo(16.15f, 5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(7.85f)
            close()
            moveToRelative(-4.88f, 9.28f)
            quadTo(15f, 16.25f, 15f, 15f)
            reflectiveQuadTo(14.13f, 12.88f)
            reflectiveQuadTo(12f, 12f)
            reflectiveQuadTo(9.88f, 12.88f)
            reflectiveQuadTo(9f, 15f)
            reflectiveQuadToRelative(0.88f, 2.13f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadToRelative(2.13f, -0.88f)
            close()
            moveTo(7f, 10f)
            horizontalLineToRelative(7f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            reflectiveQuadTo(15f, 9f)
            verticalLineTo(7f)
            quadTo(15f, 6.57f, 14.71f, 6.29f)
            reflectiveQuadTo(14f, 6f)
            horizontalLineTo(7f)
            quadTo(6.58f, 6f, 6.29f, 6.29f)
            reflectiveQuadTo(6f, 7f)
            verticalLineTo(9f)
            quadTo(6f, 9.42f, 6.29f, 9.71f)
            reflectiveQuadTo(7f, 10f)
            close()
            moveTo(5f, 7.85f)
            verticalLineTo(19f)
            verticalLineTo(5f)
            verticalLineTo(7.85f)
            close()
          }
        }
        .build()
    return _Save!!
  }

private var _Save: ImageVector? = null
