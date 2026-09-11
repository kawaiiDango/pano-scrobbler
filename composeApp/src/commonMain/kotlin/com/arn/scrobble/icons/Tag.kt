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
val Icons.Tag: ImageVector
  get() {
    if (_Tag != null) {
      return _Tag!!
    }
    _Tag =
      ImageVector.Builder(
          name = "Tag",
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
            moveTo(9f, 16f)
            lineTo(8.18f, 19.27f)
            quadTo(8.1f, 19.6f, 7.85f, 19.8f)
            reflectiveQuadTo(7.25f, 20f)
            quadTo(6.78f, 20f, 6.48f, 19.63f)
            reflectiveQuadTo(6.3f, 18.8f)
            lineTo(7f, 16f)
            horizontalLineTo(4.28f)
            quadToRelative(-0.5f, 0f, -0.8f, -0.39f)
            quadTo(3.18f, 15.23f, 3.3f, 14.75f)
            quadTo(3.38f, 14.4f, 3.65f, 14.2f)
            reflectiveQuadTo(4.28f, 14f)
            horizontalLineTo(7.5f)
            lineToRelative(1f, -4f)
            horizontalLineTo(5.78f)
            quadTo(5.28f, 10f, 4.98f, 9.61f)
            quadTo(4.68f, 9.23f, 4.8f, 8.75f)
            quadTo(4.88f, 8.4f, 5.15f, 8.2f)
            reflectiveQuadTo(5.78f, 8f)
            horizontalLineTo(9f)
            lineTo(9.83f, 4.72f)
            quadTo(9.9f, 4.4f, 10.15f, 4.2f)
            reflectiveQuadTo(10.75f, 4f)
            quadToRelative(0.48f, 0f, 0.78f, 0.38f)
            reflectiveQuadTo(11.7f, 5.2f)
            lineTo(11f, 8f)
            horizontalLineToRelative(4f)
            lineTo(15.83f, 4.72f)
            quadTo(15.9f, 4.4f, 16.15f, 4.2f)
            reflectiveQuadTo(16.75f, 4f)
            quadToRelative(0.48f, 0f, 0.78f, 0.38f)
            reflectiveQuadTo(17.7f, 5.2f)
            lineTo(17f, 8f)
            horizontalLineToRelative(2.73f)
            quadToRelative(0.5f, 0f, 0.8f, 0.39f)
            reflectiveQuadTo(20.7f, 9.25f)
            quadTo(20.63f, 9.6f, 20.35f, 9.8f)
            reflectiveQuadTo(19.73f, 10f)
            horizontalLineTo(16.5f)
            lineToRelative(-1f, 4f)
            horizontalLineToRelative(2.73f)
            quadToRelative(0.5f, 0f, 0.8f, 0.39f)
            quadToRelative(0.3f, 0.39f, 0.18f, 0.86f)
            quadTo(19.13f, 15.6f, 18.85f, 15.8f)
            reflectiveQuadTo(18.23f, 16f)
            horizontalLineTo(15f)
            lineToRelative(-0.82f, 3.27f)
            quadTo(14.1f, 19.6f, 13.85f, 19.8f)
            reflectiveQuadTo(13.25f, 20f)
            quadToRelative(-0.47f, 0f, -0.77f, -0.38f)
            reflectiveQuadTo(12.3f, 18.8f)
            lineTo(13f, 16f)
            horizontalLineTo(9f)
            close()
            moveTo(9.5f, 14f)
            horizontalLineToRelative(4f)
            lineToRelative(1f, -4f)
            horizontalLineToRelative(-4f)
            lineToRelative(-1f, 4f)
            close()
          }
        }
        .build()
    return _Tag!!
  }

private var _Tag: ImageVector? = null
