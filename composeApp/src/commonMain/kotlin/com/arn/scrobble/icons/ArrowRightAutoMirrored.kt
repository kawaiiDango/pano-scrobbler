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
val Icons.ArrowRightAutoMirrored: ImageVector
  get() {
    if (_ArrowRightAutoMirrored != null) {
      return _ArrowRightAutoMirrored!!
    }
    _ArrowRightAutoMirrored =
      ImageVector.Builder(
          name = "ArrowRightAutoMirrored",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
          autoMirror = true,
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
            moveTo(10.5f, 16.3f)
            quadToRelative(-0.2f, 0f, -0.35f, -0.14f)
            quadTo(10f, 16.02f, 10f, 15.8f)
            verticalLineTo(8.2f)
            quadTo(10f, 7.97f, 10.15f, 7.84f)
            reflectiveQuadTo(10.5f, 7.7f)
            quadToRelative(0.05f, 0f, 0.35f, 0.15f)
            lineToRelative(3.63f, 3.63f)
            quadToRelative(0.13f, 0.13f, 0.17f, 0.25f)
            reflectiveQuadTo(14.7f, 12f)
            reflectiveQuadToRelative(-0.05f, 0.27f)
            reflectiveQuadToRelative(-0.17f, 0.25f)
            lineToRelative(-3.63f, 3.63f)
            quadToRelative(-0.08f, 0.08f, -0.16f, 0.11f)
            reflectiveQuadTo(10.5f, 16.3f)
            close()
          }
        }
        .build()
    return _ArrowRightAutoMirrored!!
  }

private var _ArrowRightAutoMirrored: ImageVector? = null
