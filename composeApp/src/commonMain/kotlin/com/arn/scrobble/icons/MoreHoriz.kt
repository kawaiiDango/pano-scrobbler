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
val Icons.MoreHoriz: ImageVector
  get() {
    if (_MoreHoriz != null) {
      return _MoreHoriz!!
    }
    _MoreHoriz =
      ImageVector.Builder(
          name = "MoreHoriz",
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
            moveTo(6f, 14f)
            quadTo(5.18f, 14f, 4.59f, 13.41f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadTo(4.59f, 10.59f)
            reflectiveQuadTo(6f, 10f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(8f, 11.18f, 8f, 12f)
            reflectiveQuadTo(7.41f, 13.41f)
            reflectiveQuadTo(6f, 14f)
            close()
            moveToRelative(6f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 11.18f, 14f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 14f)
            close()
            moveToRelative(6f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(16f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(18f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(20f, 11.18f, 20f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(18f, 14f)
            close()
          }
        }
        .build()
    return _MoreHoriz!!
  }

private var _MoreHoriz: ImageVector? = null
