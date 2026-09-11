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
val Icons.HeartBroken: ImageVector
  get() {
    if (_HeartBroken != null) {
      return _HeartBroken!!
    }
    _HeartBroken =
      ImageVector.Builder(
          name = "HeartBroken",
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
            moveTo(4f, 8.5f)
            quadTo(4f, 9.25f, 4.26f, 9.92f)
            reflectiveQuadTo(5.2f, 11.46f)
            quadToRelative(0.67f, 0.86f, 1.83f, 2.06f)
            quadToRelative(1.15f, 1.2f, 2.93f, 3f)
            quadToRelative(0.1f, 0.1f, 0.21f, 0.06f)
            reflectiveQuadTo(10.3f, 16.43f)
            lineTo(10.78f, 12f)
            quadTo(9.48f, 12f, 8.7f, 10.99f)
            quadTo(7.93f, 9.98f, 8.28f, 8.75f)
            lineTo(9.23f, 5.43f)
            quadTo(8.83f, 5.22f, 8.39f, 5.11f)
            reflectiveQuadTo(7.5f, 5f)
            quadTo(6.05f, 5f, 5.03f, 6.02f)
            reflectiveQuadTo(4f, 8.5f)
            close()
            moveToRelative(16f, 0f)
            quadTo(20f, 7.05f, 18.98f, 6.02f)
            reflectiveQuadTo(16.5f, 5f)
            quadTo(16.23f, 5f, 15.95f, 5.04f)
            reflectiveQuadTo(15.4f, 5.18f)
            lineTo(14.8f, 7f)
            quadToRelative(1.13f, 0.3f, 1.66f, 1.34f)
            quadTo(17f, 9.38f, 16.65f, 10.5f)
            lineToRelative(-1.3f, 4.38f)
            quadTo(15.3f, 15f, 15.44f, 15.09f)
            reflectiveQuadTo(15.7f, 15.05f)
            quadToRelative(1.37f, -1.4f, 2.22f, -2.34f)
            reflectiveQuadToRelative(1.31f, -1.65f)
            reflectiveQuadTo(19.85f, 9.77f)
            reflectiveQuadTo(20f, 8.5f)
            close()
            moveToRelative(-3.35f, 2f)
            close()
            moveTo(8.28f, 8.75f)
            close()
            moveToRelative(3.06f, 11.48f)
            quadTo(11f, 20.1f, 10.73f, 19.85f)
            quadTo(8f, 17.45f, 6.3f, 15.81f)
            reflectiveQuadTo(3.64f, 12.94f)
            reflectiveQuadTo(2.34f, 10.68f)
            reflectiveQuadTo(2f, 8.5f)
            quadTo(2f, 6.2f, 3.6f, 4.6f)
            reflectiveQuadTo(7.5f, 3f)
            quadToRelative(1.65f, 0f, 2.9f, 0.64f)
            reflectiveQuadToRelative(0.9f, 1.84f)
            lineTo(10.38f, 8.73f)
            quadToRelative(-0.13f, 0.5f, 0.16f, 0.89f)
            reflectiveQuadTo(11.33f, 10f)
            horizontalLineTo(13f)
            lineToRelative(-0.65f, 6.35f)
            quadToRelative(-0.03f, 0.2f, 0.16f, 0.22f)
            reflectiveQuadToRelative(0.24f, -0.15f)
            lineTo(14.6f, 10.3f)
            quadTo(14.75f, 9.8f, 14.45f, 9.4f)
            reflectiveQuadTo(13.65f, 9f)
            horizontalLineTo(12f)
            lineTo(13.53f, 4.47f)
            quadTo(13.8f, 3.6f, 14.68f, 3.3f)
            reflectiveQuadTo(16.5f, 3f)
            quadToRelative(2.3f, 0f, 3.9f, 1.6f)
            reflectiveQuadTo(22f, 8.5f)
            quadToRelative(0f, 1.1f, -0.4f, 2.17f)
            reflectiveQuadToRelative(-1.39f, 2.38f)
            reflectiveQuadToRelative(-2.65f, 2.94f)
            reflectiveQuadToRelative(-4.21f, 3.86f)
            quadToRelative(-0.28f, 0.25f, -0.63f, 0.38f)
            reflectiveQuadToRelative(-0.7f, 0.13f)
            reflectiveQuadTo(11.34f, 20.23f)
            close()
          }
        }
        .build()
    return _HeartBroken!!
  }

private var _HeartBroken: ImageVector? = null
