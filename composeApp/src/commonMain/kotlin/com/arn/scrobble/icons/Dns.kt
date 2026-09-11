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
val Icons.Dns: ImageVector
  get() {
    if (_Dns != null) {
      return _Dns!!
    }
    _Dns =
      ImageVector.Builder(
          name = "Dns",
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
            moveTo(7.5f, 6f)
            quadTo(6.88f, 6f, 6.44f, 6.44f)
            reflectiveQuadTo(6f, 7.5f)
            reflectiveQuadTo(6.44f, 8.56f)
            reflectiveQuadTo(7.5f, 9f)
            reflectiveQuadTo(8.56f, 8.56f)
            reflectiveQuadTo(9f, 7.5f)
            reflectiveQuadTo(8.56f, 6.44f)
            reflectiveQuadTo(7.5f, 6f)
            close()
            moveToRelative(0f, 10f)
            quadTo(6.88f, 16f, 6.44f, 16.44f)
            reflectiveQuadTo(6f, 17.5f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadTo(7.5f, 19f)
            reflectiveQuadTo(8.56f, 18.56f)
            reflectiveQuadTo(9f, 17.5f)
            reflectiveQuadTo(8.56f, 16.44f)
            reflectiveQuadTo(7.5f, 16f)
            close()
            moveTo(4f, 3f)
            horizontalLineTo(20f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 4f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 12f)
            horizontalLineTo(4f)
            quadTo(3.58f, 12f, 3.29f, 11.71f)
            quadTo(3f, 11.43f, 3f, 11f)
            verticalLineTo(4f)
            quadTo(3f, 3.57f, 3.29f, 3.29f)
            reflectiveQuadTo(4f, 3f)
            close()
            moveTo(5f, 5f)
            verticalLineToRelative(5f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            close()
            moveTo(4f, 13f)
            horizontalLineTo(20f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 14f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 22f)
            horizontalLineTo(4f)
            quadTo(3.58f, 22f, 3.29f, 21.71f)
            quadTo(3f, 21.43f, 3f, 21f)
            verticalLineTo(14f)
            quadTo(3f, 13.58f, 3.29f, 13.29f)
            reflectiveQuadTo(4f, 13f)
            close()
            moveToRelative(1f, 2f)
            verticalLineToRelative(5f)
            horizontalLineTo(19f)
            verticalLineTo(15f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 5f)
            verticalLineToRelative(5f)
            verticalLineTo(5f)
            close()
            moveTo(5f, 15f)
            verticalLineToRelative(5f)
            verticalLineTo(15f)
            close()
          }
        }
        .build()
    return _Dns!!
  }

private var _Dns: ImageVector? = null
