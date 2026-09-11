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
val Icons.Mic: ImageVector
  get() {
    if (_Mic != null) {
      return _Mic!!
    }
    _Mic =
      ImageVector.Builder(
          name = "Mic",
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
            moveTo(9.88f, 13.13f)
            quadTo(9f, 12.25f, 9f, 11f)
            verticalLineTo(5f)
            quadTo(9f, 3.75f, 9.88f, 2.88f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(2.13f, 0.88f)
            reflectiveQuadTo(15f, 5f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 1.25f, -0.88f, 2.13f)
            reflectiveQuadTo(12f, 14f)
            reflectiveQuadTo(9.88f, 13.13f)
            close()
            moveTo(12f, 8f)
            close()
            moveTo(11f, 20f)
            verticalLineTo(17.93f)
            quadTo(8.7f, 17.6f, 7.06f, 15.98f)
            quadTo(5.43f, 14.35f, 5.08f, 12.02f)
            quadTo(5.03f, 11.6f, 5.3f, 11.3f)
            reflectiveQuadTo(6f, 11f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(7.1f, 12f)
            quadToRelative(0.35f, 1.75f, 1.74f, 2.88f)
            reflectiveQuadTo(12f, 16f)
            quadToRelative(1.8f, 0f, 3.18f, -1.14f)
            quadTo(16.55f, 13.73f, 16.9f, 12f)
            quadTo(17f, 11.58f, 17.29f, 11.29f)
            reflectiveQuadTo(18f, 11f)
            reflectiveQuadToRelative(0.7f, 0.3f)
            quadToRelative(0.28f, 0.3f, 0.23f, 0.72f)
            quadToRelative(-0.35f, 2.28f, -1.98f, 3.93f)
            reflectiveQuadTo(13f, 17.93f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 21f)
            reflectiveQuadTo(11.29f, 20.71f)
            quadTo(11f, 20.43f, 11f, 20f)
            close()
            moveToRelative(1.71f, -8.29f)
            quadTo(13f, 11.43f, 13f, 11f)
            verticalLineTo(5f)
            quadTo(13f, 4.57f, 12.71f, 4.29f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(11.29f, 4.29f)
            reflectiveQuadTo(11f, 5f)
            verticalLineToRelative(6f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 12f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
          }
        }
        .build()
    return _Mic!!
  }

private var _Mic: ImageVector? = null
