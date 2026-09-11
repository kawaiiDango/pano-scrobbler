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
val Icons.ArrowUpward: ImageVector
  get() {
    if (_ArrowUpward != null) {
      return _ArrowUpward!!
    }
    _ArrowUpward =
      ImageVector.Builder(
          name = "ArrowUpward",
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
            moveTo(11f, 7.82f)
            lineToRelative(-4.9f, 4.9f)
            quadToRelative(-0.3f, 0.3f, -0.7f, 0.29f)
            reflectiveQuadTo(4.7f, 12.7f)
            quadTo(4.43f, 12.4f, 4.41f, 12f)
            reflectiveQuadTo(4.7f, 11.3f)
            lineTo(11.3f, 4.7f)
            quadTo(11.45f, 4.55f, 11.63f, 4.49f)
            reflectiveQuadTo(12f, 4.42f)
            reflectiveQuadToRelative(0.38f, 0.06f)
            reflectiveQuadTo(12.7f, 4.7f)
            lineToRelative(6.6f, 6.6f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.69f)
            reflectiveQuadTo(19.3f, 12.7f)
            quadTo(19f, 13f, 18.59f, 13f)
            reflectiveQuadTo(17.88f, 12.7f)
            lineTo(13f, 7.82f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 20f)
            reflectiveQuadTo(11.29f, 19.71f)
            quadTo(11f, 19.43f, 11f, 19f)
            verticalLineTo(7.82f)
            close()
          }
        }
        .build()
    return _ArrowUpward!!
  }

private var _ArrowUpward: ImageVector? = null
