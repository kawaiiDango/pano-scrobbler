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
val Icons.GridView: ImageVector
  get() {
    if (_GridView != null) {
      return _GridView!!
    }
    _GridView =
      ImageVector.Builder(
          name = "GridView",
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
            moveTo(5f, 11f)
            quadTo(4.18f, 11f, 3.59f, 10.41f)
            reflectiveQuadTo(3f, 9f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(9f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(11f, 5f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(9f, 11f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(15f)
            quadTo(3f, 14.18f, 3.59f, 13.59f)
            reflectiveQuadTo(5f, 13f)
            horizontalLineTo(9f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(11f, 14.18f, 11f, 15f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(9f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(15f, 11f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(13f, 9f)
            verticalLineTo(5f)
            quadTo(13f, 4.17f, 13.59f, 3.59f)
            reflectiveQuadTo(15f, 3f)
            horizontalLineToRelative(4f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 11f)
            horizontalLineTo(15f)
            close()
            moveToRelative(0f, 10f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(13f, 19f)
            verticalLineTo(15f)
            quadToRelative(0f, -0.83f, 0.59f, -1.41f)
            reflectiveQuadTo(15f, 13f)
            horizontalLineToRelative(4f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(21f, 14.18f, 21f, 15f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(15f)
            close()
            moveTo(5f, 9f)
            horizontalLineTo(9f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(9f)
            close()
            moveTo(15f, 9f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(15f)
            verticalLineTo(9f)
            close()
            moveToRelative(0f, 10f)
            horizontalLineToRelative(4f)
            verticalLineTo(15f)
            horizontalLineTo(15f)
            verticalLineToRelative(4f)
            close()
            moveTo(5f, 19f)
            horizontalLineTo(9f)
            verticalLineTo(15f)
            horizontalLineTo(5f)
            verticalLineToRelative(4f)
            close()
            moveTo(15f, 9f)
            close()
            moveToRelative(0f, 6f)
            close()
            moveTo(9f, 15f)
            close()
            moveTo(9f, 9f)
            close()
          }
        }
        .build()
    return _GridView!!
  }

private var _GridView: ImageVector? = null
