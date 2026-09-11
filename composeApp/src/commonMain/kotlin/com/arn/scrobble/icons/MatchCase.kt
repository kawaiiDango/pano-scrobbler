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
val Icons.MatchCase: ImageVector
  get() {
    if (_MatchCase != null) {
      return _MatchCase!!
    }
    _MatchCase =
      ImageVector.Builder(
          name = "MatchCase",
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
            moveTo(6.18f, 14.9f)
            lineToRelative(-0.8f, 2.22f)
            quadTo(5.28f, 17.4f, 5.05f, 17.55f)
            reflectiveQuadTo(4.55f, 17.7f)
            quadToRelative(-0.47f, 0f, -0.74f, -0.39f)
            reflectiveQuadTo(3.73f, 16.5f)
            lineTo(7.18f, 7.3f)
            quadTo(7.28f, 7.02f, 7.51f, 6.86f)
            reflectiveQuadTo(8.03f, 6.7f)
            horizontalLineToRelative(0.7f)
            quadTo(9f, 6.7f, 9.25f, 6.86f)
            quadTo(9.5f, 7.02f, 9.6f, 7.3f)
            lineToRelative(3.45f, 9.22f)
            quadToRelative(0.18f, 0.43f, -0.1f, 0.8f)
            reflectiveQuadTo(12.23f, 17.7f)
            quadToRelative(-0.28f, 0f, -0.5f, -0.16f)
            reflectiveQuadTo(11.4f, 17.1f)
            lineTo(10.63f, 14.9f)
            horizontalLineTo(6.18f)
            close()
            moveTo(6.75f, 13.3f)
            horizontalLineToRelative(3.28f)
            lineTo(8.43f, 8.75f)
            horizontalLineTo(8.33f)
            lineTo(6.75f, 13.3f)
            close()
            moveToRelative(9.88f, 4.65f)
            quadToRelative(-1.27f, 0f, -2.02f, -0.69f)
            reflectiveQuadTo(13.85f, 15.45f)
            quadToRelative(0f, -1.1f, 0.86f, -1.81f)
            reflectiveQuadToRelative(2.21f, -0.71f)
            quadToRelative(0.57f, 0f, 1.13f, 0.1f)
            quadTo(18.6f, 13.13f, 19f, 13.3f)
            verticalLineTo(13f)
            quadToRelative(0f, -0.73f, -0.51f, -1.18f)
            reflectiveQuadTo(17.13f, 11.38f)
            quadToRelative(-0.38f, 0f, -0.74f, 0.11f)
            quadToRelative(-0.36f, 0.11f, -0.66f, 0.34f)
            quadTo(15.4f, 12.08f, 15.11f, 12f)
            reflectiveQuadTo(14.65f, 11.73f)
            quadToRelative(-0.17f, -0.2f, -0.17f, -0.47f)
            reflectiveQuadToRelative(0.27f, -0.48f)
            quadToRelative(0.5f, -0.4f, 1.13f, -0.59f)
            reflectiveQuadTo(17.15f, 10f)
            quadToRelative(1.72f, 0f, 2.57f, 0.81f)
            reflectiveQuadToRelative(0.85f, 2.44f)
            verticalLineToRelative(3.68f)
            quadToRelative(0f, 0.32f, -0.24f, 0.55f)
            reflectiveQuadTo(19.78f, 17.7f)
            reflectiveQuadTo(19.23f, 17.46f)
            quadTo(19f, 17.23f, 19f, 16.9f)
            verticalLineTo(16.77f)
            horizontalLineTo(18.9f)
            quadToRelative(-0.35f, 0.58f, -0.95f, 0.88f)
            reflectiveQuadToRelative(-1.32f, 0.3f)
            close()
            moveToRelative(0.3f, -1.35f)
            quadToRelative(0.88f, 0f, 1.49f, -0.6f)
            reflectiveQuadToRelative(0.61f, -1.4f)
            quadTo(18.68f, 14.4f, 18.19f, 14.29f)
            reflectiveQuadTo(17.23f, 14.18f)
            quadToRelative(-0.8f, 0f, -1.25f, 0.35f)
            reflectiveQuadToRelative(-0.45f, 0.92f)
            quadToRelative(0f, 0.5f, 0.4f, 0.82f)
            reflectiveQuadToRelative(1f, 0.33f)
            close()
          }
        }
        .build()
    return _MatchCase!!
  }

private var _MatchCase: ImageVector? = null
