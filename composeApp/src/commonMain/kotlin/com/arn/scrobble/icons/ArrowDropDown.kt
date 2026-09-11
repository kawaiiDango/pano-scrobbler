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
val Icons.ArrowDropDown: ImageVector
  get() {
    if (_ArrowDropDown != null) {
      return _ArrowDropDown!!
    }
    _ArrowDropDown =
      ImageVector.Builder(
          name = "ArrowDropDown",
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
            moveTo(11.48f, 14.48f)
            lineTo(7.85f, 10.85f)
            quadTo(7.78f, 10.77f, 7.74f, 10.69f)
            reflectiveQuadTo(7.7f, 10.5f)
            quadToRelative(0f, -0.2f, 0.14f, -0.35f)
            reflectiveQuadTo(8.2f, 10f)
            horizontalLineToRelative(7.6f)
            quadToRelative(0.23f, 0f, 0.36f, 0.15f)
            reflectiveQuadTo(16.3f, 10.5f)
            quadToRelative(0f, 0.05f, -0.15f, 0.35f)
            lineToRelative(-3.63f, 3.63f)
            quadTo(12.4f, 14.6f, 12.28f, 14.65f)
            reflectiveQuadTo(12f, 14.7f)
            reflectiveQuadTo(11.73f, 14.65f)
            reflectiveQuadTo(11.48f, 14.48f)
            close()
          }
        }
        .build()
    return _ArrowDropDown!!
  }

private var _ArrowDropDown: ImageVector? = null
