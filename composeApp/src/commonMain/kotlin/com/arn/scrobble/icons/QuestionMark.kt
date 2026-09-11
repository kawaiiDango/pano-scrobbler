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
val Icons.QuestionMark: ImageVector
  get() {
    if (_QuestionMark != null) {
      return _QuestionMark!!
    }
    _QuestionMark =
      ImageVector.Builder(
          name = "QuestionMark",
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
            moveTo(14.6f, 8.07f)
            quadTo(14.6f, 7f, 13.89f, 6.35f)
            reflectiveQuadTo(12f, 5.7f)
            quadToRelative(-0.72f, 0f, -1.31f, 0.31f)
            reflectiveQuadTo(9.68f, 6.93f)
            quadTo(9.28f, 7.5f, 8.59f, 7.59f)
            quadTo(7.9f, 7.68f, 7.4f, 7.22f)
            quadTo(7.05f, 6.9f, 7.01f, 6.43f)
            reflectiveQuadTo(7.25f, 5.52f)
            quadTo(8.05f, 4.32f, 9.29f, 3.66f)
            quadTo(10.53f, 3f, 12f, 3f)
            quadToRelative(2.43f, 0f, 3.94f, 1.38f)
            reflectiveQuadToRelative(1.51f, 3.6f)
            quadToRelative(0f, 1.13f, -0.47f, 2.03f)
            reflectiveQuadToRelative(-1.75f, 2.13f)
            quadTo(14.3f, 13f, 13.98f, 13.49f)
            quadToRelative(-0.33f, 0.49f, -0.43f, 1.11f)
            quadToRelative(-0.1f, 0.6f, -0.51f, 1f)
            reflectiveQuadTo(12.05f, 16f)
            reflectiveQuadTo(11.06f, 15.61f)
            quadTo(10.65f, 15.23f, 10.65f, 14.65f)
            quadToRelative(0f, -0.98f, 0.43f, -1.79f)
            reflectiveQuadTo(12.5f, 11.15f)
            quadToRelative(1.28f, -1.13f, 1.69f, -1.74f)
            reflectiveQuadTo(14.6f, 8.07f)
            close()
            moveTo(12f, 22f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 20f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 19.18f, 14f, 20f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 22f)
            close()
          }
        }
        .build()
    return _QuestionMark!!
  }

private var _QuestionMark: ImageVector? = null
