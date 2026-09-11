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
val Icons.Apps: ImageVector
  get() {
    if (_Apps != null) {
      return _Apps!!
    }
    _Apps =
      ImageVector.Builder(
          name = "Apps",
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
            moveTo(4.59f, 19.41f)
            quadTo(4f, 18.83f, 4f, 18f)
            reflectiveQuadTo(4.59f, 16.59f)
            reflectiveQuadTo(6f, 16f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(8f, 17.18f, 8f, 18f)
            reflectiveQuadTo(7.41f, 19.41f)
            reflectiveQuadTo(6f, 20f)
            reflectiveQuadTo(4.59f, 19.41f)
            close()
            moveToRelative(6f, 0f)
            quadTo(10f, 18.83f, 10f, 18f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 17.18f, 14f, 18f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 20f)
            reflectiveQuadTo(10.59f, 19.41f)
            close()
            moveToRelative(6f, 0f)
            quadTo(16f, 18.83f, 16f, 18f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(18f, 16f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(20f, 17.18f, 20f, 18f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(18f, 20f)
            reflectiveQuadTo(16.59f, 19.41f)
            close()
            moveToRelative(-12f, -6f)
            quadTo(4f, 12.83f, 4f, 12f)
            reflectiveQuadTo(4.59f, 10.59f)
            reflectiveQuadTo(6f, 10f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(8f, 11.18f, 8f, 12f)
            reflectiveQuadTo(7.41f, 13.41f)
            reflectiveQuadTo(6f, 14f)
            reflectiveQuadTo(4.59f, 13.41f)
            close()
            moveToRelative(6f, 0f)
            quadTo(10f, 12.83f, 10f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 11.18f, 14f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 14f)
            reflectiveQuadTo(10.59f, 13.41f)
            close()
            moveToRelative(6f, 0f)
            quadTo(16f, 12.83f, 16f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(18f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(20f, 11.18f, 20f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(18f, 14f)
            reflectiveQuadTo(16.59f, 13.41f)
            close()
            moveToRelative(-12f, -6f)
            quadTo(4f, 6.82f, 4f, 6f)
            reflectiveQuadTo(4.59f, 4.59f)
            reflectiveQuadTo(6f, 4f)
            quadTo(6.83f, 4f, 7.41f, 4.59f)
            quadTo(8f, 5.18f, 8f, 6f)
            reflectiveQuadTo(7.41f, 7.41f)
            reflectiveQuadTo(6f, 8f)
            reflectiveQuadTo(4.59f, 7.41f)
            close()
            moveToRelative(6f, 0f)
            quadTo(10f, 6.82f, 10f, 6f)
            reflectiveQuadTo(10.59f, 4.59f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 5.18f, 14f, 6f)
            reflectiveQuadTo(13.41f, 7.41f)
            reflectiveQuadTo(12f, 8f)
            reflectiveQuadTo(10.59f, 7.41f)
            close()
            moveToRelative(6f, 0f)
            quadTo(16f, 6.82f, 16f, 6f)
            reflectiveQuadTo(16.59f, 4.59f)
            reflectiveQuadTo(18f, 4f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(20f, 5.18f, 20f, 6f)
            reflectiveQuadTo(19.41f, 7.41f)
            reflectiveQuadTo(18f, 8f)
            reflectiveQuadTo(16.59f, 7.41f)
            close()
          }
        }
        .build()
    return _Apps!!
  }

private var _Apps: ImageVector? = null
