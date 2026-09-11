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
val Icons.ArrowLeftAutoMirrored: ImageVector
  get() {
    if (_ArrowLeftAutoMirrored != null) {
      return _ArrowLeftAutoMirrored!!
    }
    _ArrowLeftAutoMirrored =
      ImageVector.Builder(
          name = "ArrowLeftAutoMirrored",
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
            moveTo(13.15f, 16.15f)
            lineTo(9.53f, 12.52f)
            quadTo(9.4f, 12.4f, 9.35f, 12.27f)
            reflectiveQuadTo(9.3f, 12f)
            reflectiveQuadTo(9.35f, 11.73f)
            reflectiveQuadTo(9.53f, 11.48f)
            lineTo(13.15f, 7.85f)
            quadTo(13.23f, 7.77f, 13.31f, 7.74f)
            reflectiveQuadTo(13.5f, 7.7f)
            quadToRelative(0.2f, 0f, 0.35f, 0.14f)
            reflectiveQuadTo(14f, 8.2f)
            verticalLineToRelative(7.6f)
            quadToRelative(0f, 0.22f, -0.15f, 0.36f)
            reflectiveQuadTo(13.5f, 16.3f)
            quadToRelative(-0.05f, 0f, -0.35f, -0.15f)
            close()
          }
        }
        .build()
    return _ArrowLeftAutoMirrored!!
  }

private var _ArrowLeftAutoMirrored: ImageVector? = null
