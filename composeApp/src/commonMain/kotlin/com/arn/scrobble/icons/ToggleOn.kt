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
val Icons.ToggleOn: ImageVector
  get() {
    if (_ToggleOn != null) {
      return _ToggleOn!!
    }
    _ToggleOn =
      ImageVector.Builder(
          name = "ToggleOn",
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
            moveTo(7f, 18f)
            quadTo(4.5f, 18f, 2.75f, 16.25f)
            reflectiveQuadTo(1f, 12f)
            reflectiveQuadTo(2.75f, 7.75f)
            reflectiveQuadTo(7f, 6f)
            horizontalLineTo(17f)
            quadToRelative(2.5f, 0f, 4.25f, 1.75f)
            reflectiveQuadTo(23f, 12f)
            reflectiveQuadToRelative(-1.75f, 4.25f)
            reflectiveQuadTo(17f, 18f)
            horizontalLineTo(7f)
            close()
            moveTo(7f, 16f)
            horizontalLineTo(17f)
            quadToRelative(1.65f, 0f, 2.83f, -1.18f)
            reflectiveQuadTo(21f, 12f)
            reflectiveQuadTo(19.83f, 9.17f)
            reflectiveQuadTo(17f, 8f)
            horizontalLineTo(7f)
            quadTo(5.35f, 8f, 4.18f, 9.17f)
            reflectiveQuadTo(3f, 12f)
            reflectiveQuadToRelative(1.17f, 2.82f)
            reflectiveQuadTo(7f, 16f)
            close()
            moveTo(19.13f, 14.13f)
            quadTo(20f, 13.25f, 20f, 12f)
            reflectiveQuadTo(19.13f, 9.88f)
            reflectiveQuadTo(17f, 9f)
            reflectiveQuadTo(14.88f, 9.88f)
            reflectiveQuadTo(14f, 12f)
            reflectiveQuadToRelative(0.88f, 2.13f)
            reflectiveQuadTo(17f, 15f)
            reflectiveQuadToRelative(2.13f, -0.88f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build()
    return _ToggleOn!!
  }

private var _ToggleOn: ImageVector? = null
