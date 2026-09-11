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
val Icons.Cake: ImageVector
  get() {
    if (_Cake != null) {
      return _Cake!!
    }
    _Cake =
      ImageVector.Builder(
          name = "Cake",
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
            moveTo(4f, 22f)
            quadTo(3.58f, 22f, 3.29f, 21.71f)
            quadTo(3f, 21.43f, 3f, 21f)
            verticalLineTo(16f)
            quadTo(3f, 15.18f, 3.59f, 14.59f)
            reflectiveQuadTo(5f, 14f)
            verticalLineTo(10f)
            quadTo(5f, 9.17f, 5.59f, 8.59f)
            reflectiveQuadTo(7f, 8f)
            horizontalLineToRelative(4f)
            verticalLineTo(6.55f)
            quadTo(10.55f, 6.25f, 10.28f, 5.82f)
            reflectiveQuadTo(10f, 4.8f)
            quadTo(10f, 4.42f, 10.15f, 4.06f)
            reflectiveQuadTo(10.6f, 3.4f)
            lineTo(11.65f, 2.35f)
            quadTo(11.7f, 2.3f, 12f, 2.2f)
            quadToRelative(0.05f, 0f, 0.35f, 0.15f)
            lineTo(13.4f, 3.4f)
            quadToRelative(0.3f, 0.3f, 0.45f, 0.66f)
            quadTo(14f, 4.42f, 14f, 4.8f)
            quadToRelative(0f, 0.6f, -0.27f, 1.03f)
            reflectiveQuadTo(13f, 6.55f)
            verticalLineTo(8f)
            horizontalLineToRelative(4f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(19f, 10f)
            verticalLineToRelative(4f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 16f)
            verticalLineToRelative(5f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 22f)
            horizontalLineTo(4f)
            close()
            moveTo(7f, 14f)
            horizontalLineTo(17f)
            verticalLineTo(10f)
            horizontalLineTo(7f)
            verticalLineToRelative(4f)
            close()
            moveTo(5f, 20f)
            horizontalLineTo(19f)
            verticalLineTo(16f)
            horizontalLineTo(5f)
            verticalLineToRelative(4f)
            close()
            moveTo(7f, 14f)
            horizontalLineTo(17f)
            horizontalLineTo(7f)
            close()
            moveTo(5f, 20f)
            horizontalLineTo(19f)
            horizontalLineTo(5f)
            close()
            moveTo(19f, 14f)
            horizontalLineTo(5f)
            horizontalLineTo(19f)
            close()
          }
        }
        .build()
    return _Cake!!
  }

private var _Cake: ImageVector? = null
