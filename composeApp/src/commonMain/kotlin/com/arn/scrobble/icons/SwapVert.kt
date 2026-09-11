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
val Icons.SwapVert: ImageVector
  get() {
    if (_SwapVert != null) {
      return _SwapVert!!
    }
    _SwapVert =
      ImageVector.Builder(
          name = "SwapVert",
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
            moveTo(8.29f, 12.71f)
            quadTo(8f, 12.43f, 8f, 12f)
            verticalLineTo(5.82f)
            lineTo(6.13f, 7.7f)
            quadTo(5.85f, 7.97f, 5.44f, 7.97f)
            quadTo(5.03f, 7.97f, 4.73f, 7.7f)
            quadTo(4.43f, 7.4f, 4.43f, 6.99f)
            quadToRelative(0f, -0.41f, 0.3f, -0.71f)
            lineTo(8.3f, 2.7f)
            quadTo(8.45f, 2.55f, 8.63f, 2.49f)
            reflectiveQuadTo(9f, 2.42f)
            reflectiveQuadTo(9.38f, 2.49f)
            reflectiveQuadTo(9.7f, 2.7f)
            lineToRelative(3.6f, 3.6f)
            quadTo(13.6f, 6.6f, 13.59f, 7f)
            reflectiveQuadTo(13.28f, 7.7f)
            quadToRelative(-0.3f, 0.27f, -0.7f, 0.29f)
            reflectiveQuadTo(11.88f, 7.7f)
            lineTo(10f, 5.82f)
            verticalLineTo(12f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(9f, 13f)
            quadTo(8.58f, 13f, 8.29f, 12.71f)
            close()
            moveToRelative(6.34f, 8.8f)
            quadTo(14.45f, 21.45f, 14.3f, 21.3f)
            lineTo(10.7f, 17.7f)
            quadTo(10.4f, 17.4f, 10.41f, 17f)
            reflectiveQuadToRelative(0.31f, -0.7f)
            quadToRelative(0.3f, -0.28f, 0.7f, -0.29f)
            reflectiveQuadToRelative(0.7f, 0.29f)
            lineTo(14f, 18.18f)
            verticalLineTo(12f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(15f, 11f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(16f, 12f)
            verticalLineToRelative(6.18f)
            lineTo(17.88f, 16.3f)
            quadToRelative(0.28f, -0.28f, 0.69f, -0.28f)
            quadToRelative(0.41f, 0f, 0.71f, 0.28f)
            quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
            quadToRelative(0f, 0.41f, -0.3f, 0.71f)
            lineTo(15.7f, 21.3f)
            quadToRelative(-0.15f, 0.15f, -0.33f, 0.21f)
            reflectiveQuadTo(15f, 21.58f)
            reflectiveQuadTo(14.63f, 21.51f)
            close()
          }
        }
        .build()
    return _SwapVert!!
  }

private var _SwapVert: ImageVector? = null
