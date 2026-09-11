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
val Icons.Casino: ImageVector
  get() {
    if (_Casino != null) {
      return _Casino!!
    }
    _Casino =
      ImageVector.Builder(
          name = "Casino",
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
            moveTo(8.56f, 17.56f)
            quadTo(9f, 17.13f, 9f, 16.5f)
            reflectiveQuadTo(8.56f, 15.44f)
            reflectiveQuadTo(7.5f, 15f)
            reflectiveQuadTo(6.44f, 15.44f)
            reflectiveQuadTo(6f, 16.5f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadTo(7.5f, 18f)
            reflectiveQuadTo(8.56f, 17.56f)
            close()
            moveToRelative(0f, -9f)
            quadTo(9f, 8.13f, 9f, 7.5f)
            reflectiveQuadTo(8.56f, 6.44f)
            reflectiveQuadTo(7.5f, 6f)
            reflectiveQuadTo(6.44f, 6.44f)
            reflectiveQuadTo(6f, 7.5f)
            reflectiveQuadTo(6.44f, 8.56f)
            reflectiveQuadTo(7.5f, 9f)
            reflectiveQuadTo(8.56f, 8.56f)
            close()
            moveToRelative(4.5f, 4.5f)
            quadTo(13.5f, 12.63f, 13.5f, 12f)
            reflectiveQuadTo(13.06f, 10.94f)
            reflectiveQuadTo(12f, 10.5f)
            reflectiveQuadToRelative(-1.06f, 0.44f)
            reflectiveQuadTo(10.5f, 12f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadTo(12f, 13.5f)
            reflectiveQuadToRelative(1.06f, -0.44f)
            close()
            moveToRelative(4.5f, 4.5f)
            quadTo(18f, 17.13f, 18f, 16.5f)
            reflectiveQuadTo(17.56f, 15.44f)
            reflectiveQuadTo(16.5f, 15f)
            reflectiveQuadToRelative(-1.06f, 0.44f)
            reflectiveQuadTo(15f, 16.5f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadTo(16.5f, 18f)
            reflectiveQuadToRelative(1.06f, -0.44f)
            close()
            moveToRelative(0f, -9f)
            quadTo(18f, 8.13f, 18f, 7.5f)
            reflectiveQuadTo(17.56f, 6.44f)
            reflectiveQuadTo(16.5f, 6f)
            reflectiveQuadTo(15.44f, 6.44f)
            reflectiveQuadTo(15f, 7.5f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadTo(16.5f, 9f)
            reflectiveQuadTo(17.56f, 8.56f)
            close()
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
            horizontalLineTo(5f)
            close()
            moveTo(5f, 19f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            close()
            moveTo(5f, 5f)
            verticalLineTo(19f)
            verticalLineTo(5f)
            close()
          }
        }
        .build()
    return _Casino!!
  }

private var _Casino: ImageVector? = null
