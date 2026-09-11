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
val Icons.ArrowRightAltAutoMirrored: ImageVector
  get() {
    if (_ArrowRightAltAutoMirrored != null) {
      return _ArrowRightAltAutoMirrored!!
    }
    _ArrowRightAltAutoMirrored =
      ImageVector.Builder(
          name = "ArrowRightAltAutoMirrored",
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
            moveTo(16.15f, 13f)
            horizontalLineTo(5f)
            quadTo(4.58f, 13f, 4.29f, 12.71f)
            quadTo(4f, 12.43f, 4f, 12f)
            reflectiveQuadTo(4.29f, 11.29f)
            reflectiveQuadTo(5f, 11f)
            horizontalLineTo(16.15f)
            lineTo(13.3f, 8.15f)
            quadTo(13f, 7.85f, 13.01f, 7.45f)
            reflectiveQuadTo(13.3f, 6.75f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.31f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            lineTo(19.3f, 11.3f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
            reflectiveQuadTo(19.58f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.38f)
            reflectiveQuadTo(19.3f, 12.7f)
            lineToRelative(-4.57f, 4.57f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.29f)
            reflectiveQuadTo(13.3f, 17.25f)
            quadToRelative(-0.28f, -0.3f, -0.29f, -0.7f)
            reflectiveQuadToRelative(0.29f, -0.7f)
            lineTo(16.15f, 13f)
            close()
          }
        }
        .build()
    return _ArrowRightAltAutoMirrored!!
  }

private var _ArrowRightAltAutoMirrored: ImageVector? = null
