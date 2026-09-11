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
val Icons.VisibilityOff: ImageVector
  get() {
    if (_VisibilityOff != null) {
      return _VisibilityOff!!
    }
    _VisibilityOff =
      ImageVector.Builder(
          name = "VisibilityOff",
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
            moveTo(15.18f, 8.32f)
            quadToRelative(0.72f, 0.72f, 1.06f, 1.65f)
            reflectiveQuadToRelative(0.24f, 1.9f)
            quadToRelative(0f, 0.38f, -0.28f, 0.64f)
            quadToRelative(-0.27f, 0.26f, -0.65f, 0.26f)
            reflectiveQuadTo(14.91f, 12.51f)
            reflectiveQuadTo(14.65f, 11.88f)
            quadToRelative(0.13f, -0.65f, -0.07f, -1.25f)
            quadTo(14.38f, 10.02f, 13.95f, 9.6f)
            quadTo(13.53f, 9.17f, 12.93f, 8.95f)
            reflectiveQuadTo(11.65f, 8.85f)
            quadToRelative(-0.38f, 0f, -0.64f, -0.28f)
            reflectiveQuadTo(10.75f, 7.93f)
            reflectiveQuadTo(11.01f, 7.29f)
            reflectiveQuadTo(11.65f, 7.02f)
            quadToRelative(0.95f, -0.1f, 1.88f, 0.24f)
            reflectiveQuadToRelative(1.65f, 1.06f)
            close()
            moveTo(12f, 6f)
            quadTo(11.53f, 6f, 11.08f, 6.04f)
            reflectiveQuadToRelative(-0.9f, 0.14f)
            quadTo(9.75f, 6.25f, 9.41f, 6.05f)
            quadTo(9.08f, 5.85f, 8.95f, 5.45f)
            reflectiveQuadTo(9.04f, 4.67f)
            reflectiveQuadTo(9.65f, 4.22f)
            quadTo(10.23f, 4.1f, 10.81f, 4.05f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(3.43f, 0f, 6.26f, 1.8f)
            reflectiveQuadToRelative(4.34f, 4.85f)
            quadToRelative(0.1f, 0.2f, 0.15f, 0.41f)
            quadToRelative(0.05f, 0.21f, 0.05f, 0.44f)
            reflectiveQuadToRelative(-0.04f, 0.44f)
            reflectiveQuadToRelative(-0.14f, 0.41f)
            quadToRelative(-0.45f, 1f, -1.11f, 1.88f)
            reflectiveQuadToRelative(-1.46f, 1.6f)
            quadToRelative(-0.3f, 0.28f, -0.7f, 0.23f)
            reflectiveQuadTo(18.7f, 15.65f)
            reflectiveQuadTo(18.49f, 14.89f)
            reflectiveQuadTo(18.83f, 14.2f)
            quadToRelative(0.6f, -0.57f, 1.1f, -1.25f)
            reflectiveQuadTo(20.8f, 11.5f)
            quadTo(19.55f, 8.98f, 17.19f, 7.49f)
            reflectiveQuadTo(12f, 6f)
            close()
            moveToRelative(0f, 13f)
            quadTo(8.65f, 19f, 5.88f, 17.19f)
            reflectiveQuadTo(1.5f, 12.43f)
            quadTo(1.38f, 12.23f, 1.31f, 11.99f)
            reflectiveQuadTo(1.25f, 11.5f)
            reflectiveQuadTo(1.3f, 11.02f)
            reflectiveQuadTo(1.48f, 10.58f)
            quadToRelative(0.5f, -1f, 1.16f, -1.91f)
            reflectiveQuadTo(4.15f, 7f)
            lineTo(2.08f, 4.9f)
            quadTo(1.8f, 4.6f, 1.81f, 4.19f)
            quadTo(1.83f, 3.77f, 2.1f, 3.5f)
            quadTo(2.38f, 3.22f, 2.8f, 3.22f)
            reflectiveQuadTo(3.5f, 3.5f)
            lineToRelative(17f, 17f)
            quadToRelative(0.28f, 0.27f, 0.29f, 0.69f)
            reflectiveQuadTo(20.5f, 21.9f)
            quadToRelative(-0.27f, 0.28f, -0.7f, 0.28f)
            reflectiveQuadTo(19.1f, 21.9f)
            lineTo(15.6f, 18.45f)
            quadToRelative(-0.88f, 0.28f, -1.78f, 0.41f)
            reflectiveQuadTo(12f, 19f)
            close()
            moveTo(5.55f, 8.4f)
            quadTo(4.83f, 9.05f, 4.23f, 9.82f)
            reflectiveQuadTo(3.2f, 11.5f)
            quadToRelative(1.25f, 2.52f, 3.61f, 4.01f)
            reflectiveQuadTo(12f, 17f)
            quadToRelative(0.5f, 0f, 0.98f, -0.06f)
            reflectiveQuadTo(13.95f, 16.8f)
            lineToRelative(-0.9f, -0.95f)
            quadToRelative(-0.28f, 0.07f, -0.53f, 0.11f)
            reflectiveQuadTo(12f, 16f)
            quadTo(10.13f, 16f, 8.81f, 14.69f)
            reflectiveQuadTo(7.5f, 11.5f)
            quadToRelative(0f, -0.28f, 0.04f, -0.53f)
            reflectiveQuadTo(7.65f, 10.45f)
            lineTo(5.55f, 8.4f)
            close()
            moveToRelative(7.98f, 2.33f)
            close()
            moveTo(9.75f, 12.6f)
            close()
          }
        }
        .build()
    return _VisibilityOff!!
  }

private var _VisibilityOff: ImageVector? = null
