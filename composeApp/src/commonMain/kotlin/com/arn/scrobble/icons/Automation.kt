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
val Icons.Automation: ImageVector
  get() {
    if (_Automation != null) {
      return _Automation!!
    }
    _Automation =
      ImageVector.Builder(
          name = "Automation",
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
            moveTo(7.4f, 17.25f)
            quadToRelative(-1.05f, 0.88f, -2.19f, 0.8f)
            reflectiveQuadTo(3.23f, 17.27f)
            quadTo(2.38f, 16.58f, 2.06f, 15.44f)
            reflectiveQuadTo(2.48f, 13.1f)
            lineTo(4.35f, 10f)
            quadTo(3.73f, 9.45f, 3.36f, 8.67f)
            reflectiveQuadTo(3f, 7f)
            quadTo(3f, 5.35f, 4.18f, 4.17f)
            reflectiveQuadTo(7f, 3f)
            reflectiveQuadTo(9.83f, 4.17f)
            reflectiveQuadTo(11f, 7f)
            reflectiveQuadTo(9.83f, 9.82f)
            reflectiveQuadTo(7f, 11f)
            quadTo(6.78f, 11f, 6.55f, 10.98f)
            reflectiveQuadTo(6.13f, 10.9f)
            lineTo(4.2f, 14.15f)
            quadTo(3.93f, 14.6f, 4.03f, 15.04f)
            reflectiveQuadToRelative(0.42f, 0.71f)
            reflectiveQuadToRelative(0.78f, 0.31f)
            reflectiveQuadTo(6.1f, 15.75f)
            lineTo(16.6f, 6.72f)
            quadTo(17.65f, 5.85f, 18.8f, 5.94f)
            quadToRelative(1.15f, 0.09f, 2f, 0.79f)
            quadToRelative(0.85f, 0.7f, 1.15f, 1.84f)
            reflectiveQuadTo(21.53f, 10.9f)
            lineTo(19.65f, 14f)
            quadToRelative(0.63f, 0.55f, 0.99f, 1.32f)
            reflectiveQuadTo(21f, 17f)
            quadToRelative(0f, 1.65f, -1.17f, 2.82f)
            reflectiveQuadTo(17f, 21f)
            reflectiveQuadTo(14.18f, 19.83f)
            reflectiveQuadTo(13f, 17f)
            reflectiveQuadToRelative(1.18f, -2.83f)
            reflectiveQuadTo(17f, 13f)
            quadToRelative(0.23f, 0f, 0.44f, 0.02f)
            reflectiveQuadToRelative(0.41f, 0.08f)
            lineTo(19.8f, 9.85f)
            quadTo(20.08f, 9.4f, 19.98f, 8.96f)
            reflectiveQuadTo(19.55f, 8.25f)
            quadTo(19.23f, 7.97f, 18.78f, 7.94f)
            reflectiveQuadTo(17.9f, 8.25f)
            lineToRelative(-10.5f, 9f)
            close()
            moveTo(8.41f, 8.41f)
            quadTo(9f, 7.82f, 9f, 7f)
            reflectiveQuadTo(8.41f, 5.59f)
            quadTo(7.83f, 5f, 7f, 5f)
            reflectiveQuadTo(5.59f, 5.59f)
            quadTo(5f, 6.18f, 5f, 7f)
            reflectiveQuadTo(5.59f, 8.41f)
            reflectiveQuadTo(7f, 9f)
            quadTo(7.83f, 9f, 8.41f, 8.41f)
            close()
            moveToRelative(10f, 10f)
            quadTo(19f, 17.83f, 19f, 17f)
            reflectiveQuadTo(18.41f, 15.59f)
            reflectiveQuadTo(17f, 15f)
            reflectiveQuadToRelative(-1.41f, 0.59f)
            reflectiveQuadTo(15f, 17f)
            reflectiveQuadToRelative(0.59f, 1.41f)
            reflectiveQuadTo(17f, 19f)
            reflectiveQuadToRelative(1.41f, -0.59f)
            close()
            moveTo(7f, 7f)
            close()
            moveTo(17f, 17f)
            close()
          }
        }
        .build()
    return _Automation!!
  }

private var _Automation: ImageVector? = null
