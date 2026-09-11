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
val Icons.OpenInBrowser: ImageVector
  get() {
    if (_OpenInBrowser != null) {
      return _OpenInBrowser!!
    }
    _OpenInBrowser =
      ImageVector.Builder(
          name = "OpenInBrowser",
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
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(16f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(15f, 20.43f, 15f, 20f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(16f, 19f)
            horizontalLineToRelative(3f)
            verticalLineTo(7f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineTo(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(9f, 20f)
            reflectiveQuadTo(8.71f, 20.71f)
            reflectiveQuadTo(8f, 21f)
            horizontalLineTo(5f)
            close()
            moveToRelative(6f, -1f)
            verticalLineTo(14.85f)
            lineToRelative(-0.88f, 0.88f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.29f)
            reflectiveQuadTo(8.7f, 15.7f)
            quadTo(8.43f, 15.4f, 8.41f, 15f)
            reflectiveQuadTo(8.7f, 14.3f)
            lineToRelative(2.6f, -2.6f)
            quadToRelative(0.15f, -0.15f, 0.32f, -0.21f)
            reflectiveQuadTo(12f, 11.43f)
            reflectiveQuadToRelative(0.38f, 0.06f)
            reflectiveQuadTo(12.7f, 11.7f)
            lineToRelative(2.6f, 2.6f)
            quadToRelative(0.3f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadTo(15.3f, 15.7f)
            quadTo(15f, 16f, 14.59f, 16.01f)
            reflectiveQuadTo(13.88f, 15.73f)
            lineTo(13f, 14.85f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 21f)
            reflectiveQuadTo(11.29f, 20.71f)
            quadTo(11f, 20.43f, 11f, 20f)
            close()
          }
        }
        .build()
    return _OpenInBrowser!!
  }

private var _OpenInBrowser: ImageVector? = null
