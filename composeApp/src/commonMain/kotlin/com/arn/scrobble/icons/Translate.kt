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
val Icons.Translate: ImageVector
  get() {
    if (_Translate != null) {
      return _Translate!!
    }
    _Translate =
      ImageVector.Builder(
          name = "Translate",
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
            moveTo(15.08f, 18.95f)
            lineToRelative(-0.85f, 2.43f)
            quadToRelative(-0.1f, 0.27f, -0.35f, 0.45f)
            reflectiveQuadTo(13.33f, 22f)
            quadToRelative(-0.5f, 0f, -0.81f, -0.41f)
            quadTo(12.2f, 21.18f, 12.4f, 20.68f)
            lineTo(16.2f, 10.63f)
            quadToRelative(0.13f, -0.28f, 0.38f, -0.45f)
            reflectiveQuadTo(17.13f, 10f)
            horizontalLineToRelative(0.75f)
            quadToRelative(0.3f, 0f, 0.55f, 0.17f)
            reflectiveQuadToRelative(0.38f, 0.45f)
            lineTo(22.6f, 20.7f)
            quadToRelative(0.2f, 0.48f, -0.1f, 0.89f)
            reflectiveQuadTo(21.7f, 22f)
            quadToRelative(-0.33f, 0f, -0.56f, -0.18f)
            reflectiveQuadTo(20.78f, 21.35f)
            lineToRelative(-0.85f, -2.4f)
            horizontalLineTo(15.08f)
            close()
            moveTo(9.05f, 13.98f)
            lineTo(4.7f, 18.3f)
            quadTo(4.43f, 18.58f, 4.01f, 18.59f)
            reflectiveQuadTo(3.3f, 18.3f)
            quadTo(3.03f, 18.02f, 3.03f, 17.6f)
            reflectiveQuadTo(3.3f, 16.9f)
            lineTo(7.65f, 12.55f)
            quadToRelative(-0.88f, -0.88f, -1.59f, -2f)
            reflectiveQuadTo(4.75f, 8f)
            horizontalLineToRelative(2.1f)
            quadToRelative(0.5f, 0.97f, 1f, 1.7f)
            reflectiveQuadToRelative(1.2f, 1.45f)
            quadTo(9.88f, 10.33f, 10.76f, 8.84f)
            reflectiveQuadTo(12.1f, 6f)
            horizontalLineTo(2f)
            quadTo(1.58f, 6f, 1.29f, 5.71f)
            quadTo(1f, 5.43f, 1f, 5f)
            reflectiveQuadTo(1.29f, 4.29f)
            reflectiveQuadTo(2f, 4f)
            horizontalLineTo(8f)
            verticalLineTo(3f)
            quadTo(8f, 2.57f, 8.29f, 2.29f)
            quadTo(8.58f, 2f, 9f, 2f)
            quadTo(9.43f, 2f, 9.71f, 2.29f)
            reflectiveQuadTo(10f, 3f)
            verticalLineTo(4f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 5f)
            reflectiveQuadTo(16.71f, 5.71f)
            reflectiveQuadTo(16f, 6f)
            horizontalLineTo(14.1f)
            quadTo(13.58f, 7.8f, 12.53f, 9.7f)
            reflectiveQuadToRelative(-2.07f, 2.9f)
            lineToRelative(2.4f, 2.45f)
            lineTo(12.1f, 17.1f)
            lineTo(9.05f, 13.98f)
            close()
            moveTo(15.7f, 17.2f)
            horizontalLineToRelative(3.6f)
            lineTo(17.5f, 12.1f)
            lineToRelative(-1.8f, 5.1f)
            close()
          }
        }
        .build()
    return _Translate!!
  }

private var _Translate: ImageVector? = null
