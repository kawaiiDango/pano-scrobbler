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
val Icons.Block: ImageVector
  get() {
    if (_Block != null) {
      return _Block!!
    }
    _Block =
      ImageVector.Builder(
          name = "Block",
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
            moveTo(8.1f, 21.21f)
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
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            close()
            moveTo(12f, 20f)
            quadToRelative(1.35f, 0f, 2.6f, -0.44f)
            reflectiveQuadTo(16.9f, 18.3f)
            lineTo(5.7f, 7.1f)
            quadTo(4.88f, 8.15f, 4.44f, 9.4f)
            reflectiveQuadTo(4f, 12f)
            quadToRelative(0f, 3.35f, 2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(6.3f, -3.1f)
            quadToRelative(0.82f, -1.05f, 1.26f, -2.3f)
            reflectiveQuadTo(20f, 12f)
            quadTo(20f, 8.65f, 17.68f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            quadTo(10.65f, 4f, 9.4f, 4.44f)
            reflectiveQuadTo(7.1f, 5.7f)
            lineTo(18.3f, 16.9f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build()
    return _Block!!
  }

private var _Block: ImageVector? = null
