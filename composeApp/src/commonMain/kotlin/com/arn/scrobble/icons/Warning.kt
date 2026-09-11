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
val Icons.Warning: ImageVector
  get() {
    if (_Warning != null) {
      return _Warning!!
    }
    _Warning =
      ImageVector.Builder(
          name = "Warning",
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
            moveTo(2.73f, 21f)
            quadTo(2.45f, 21f, 2.23f, 20.86f)
            reflectiveQuadTo(1.88f, 20.5f)
            reflectiveQuadTo(1.74f, 20.01f)
            reflectiveQuadTo(1.88f, 19.5f)
            lineToRelative(9.25f, -16f)
            quadTo(11.28f, 3.25f, 11.51f, 3.13f)
            reflectiveQuadTo(12f, 3f)
            reflectiveQuadToRelative(0.49f, 0.13f)
            reflectiveQuadTo(12.88f, 3.5f)
            lineToRelative(9.25f, 16f)
            quadToRelative(0.15f, 0.25f, 0.14f, 0.51f)
            reflectiveQuadTo(22.13f, 20.5f)
            reflectiveQuadToRelative(-0.35f, 0.36f)
            reflectiveQuadTo(21.28f, 21f)
            horizontalLineTo(2.73f)
            close()
            moveTo(4.45f, 19f)
            horizontalLineToRelative(15.1f)
            lineTo(12f, 6f)
            lineTo(4.45f, 19f)
            close()
            moveToRelative(8.26f, -1.29f)
            quadTo(13f, 17.43f, 13f, 17f)
            reflectiveQuadTo(12.71f, 16.29f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(11f, 17f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(0f, -3f)
            quadTo(13f, 14.43f, 13f, 14f)
            verticalLineTo(11f)
            quadToRelative(0f, -0.43f, -0.29f, -0.71f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(11f, 11f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveTo(12f, 12.5f)
            close()
          }
        }
        .build()
    return _Warning!!
  }

private var _Warning: ImageVector? = null
