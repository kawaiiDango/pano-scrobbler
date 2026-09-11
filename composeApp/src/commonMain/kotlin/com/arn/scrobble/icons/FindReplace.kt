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
val Icons.FindReplace: ImageVector
  get() {
    if (_FindReplace != null) {
      return _FindReplace!!
    }
    _FindReplace =
      ImageVector.Builder(
          name = "FindReplace",
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
            moveTo(11f, 6f)
            quadTo(9.43f, 6f, 8.16f, 6.86f)
            quadTo(6.9f, 7.72f, 6.35f, 9.13f)
            quadTo(6.2f, 9.5f, 5.86f, 9.69f)
            reflectiveQuadTo(5.13f, 9.8f)
            quadTo(4.73f, 9.73f, 4.49f, 9.41f)
            reflectiveQuadTo(4.38f, 8.75f)
            quadTo(5.05f, 6.65f, 6.85f, 5.32f)
            reflectiveQuadTo(11f, 4f)
            quadToRelative(1.48f, 0f, 2.76f, 0.56f)
            reflectiveQuadTo(16f, 6.1f)
            verticalLineTo(5f)
            quadTo(16f, 4.57f, 16.29f, 4.29f)
            reflectiveQuadTo(17f, 4f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 5f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 10f)
            horizontalLineTo(13f)
            quadTo(12.58f, 10f, 12.29f, 9.71f)
            reflectiveQuadTo(12f, 9f)
            quadTo(12f, 8.57f, 12.29f, 8.29f)
            reflectiveQuadTo(13f, 8f)
            horizontalLineToRelative(2f)
            quadTo(14.28f, 7.1f, 13.26f, 6.55f)
            reflectiveQuadTo(11f, 6f)
            close()
            moveToRelative(0f, 12f)
            quadTo(9.53f, 18f, 8.24f, 17.44f)
            reflectiveQuadTo(6f, 15.9f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(5f, 18f)
            quadTo(4.58f, 18f, 4.29f, 17.71f)
            quadTo(4f, 17.43f, 4f, 17f)
            verticalLineTo(13f)
            quadTo(4f, 12.58f, 4.29f, 12.29f)
            reflectiveQuadTo(5f, 12f)
            horizontalLineTo(9f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(10f, 13f)
            reflectiveQuadTo(9.71f, 13.71f)
            reflectiveQuadTo(9f, 14f)
            horizontalLineTo(7f)
            quadToRelative(0.73f, 0.9f, 1.74f, 1.45f)
            reflectiveQuadTo(11f, 16f)
            quadToRelative(1.55f, 0f, 2.79f, -0.83f)
            reflectiveQuadTo(15.6f, 13f)
            quadToRelative(0.17f, -0.4f, 0.51f, -0.64f)
            quadToRelative(0.34f, -0.24f, 0.76f, -0.16f)
            quadToRelative(0.43f, 0.1f, 0.63f, 0.45f)
            reflectiveQuadToRelative(0.07f, 0.75f)
            quadToRelative(-0.17f, 0.5f, -0.41f, 0.94f)
            quadTo(16.93f, 14.78f, 16.6f, 15.2f)
            lineToRelative(3.7f, 3.7f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
            quadToRelative(0f, 0.42f, -0.28f, 0.7f)
            quadToRelative(-0.27f, 0.27f, -0.7f, 0.27f)
            reflectiveQuadTo(18.9f, 20.3f)
            lineTo(15.2f, 16.6f)
            quadToRelative(-0.9f, 0.67f, -1.96f, 1.04f)
            reflectiveQuadTo(11f, 18f)
            close()
          }
        }
        .build()
    return _FindReplace!!
  }

private var _FindReplace: ImageVector? = null
