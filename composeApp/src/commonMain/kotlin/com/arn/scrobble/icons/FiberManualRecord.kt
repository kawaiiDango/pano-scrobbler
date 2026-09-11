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
val Icons.FiberManualRecord: ImageVector
  get() {
    if (_FiberManualRecord != null) {
      return _FiberManualRecord!!
    }
    _FiberManualRecord =
      ImageVector.Builder(
          name = "FiberManualRecord",
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
            moveTo(12f, 12f)
            close()
            moveTo(7.05f, 16.95f)
            quadTo(5f, 14.9f, 5f, 12f)
            reflectiveQuadTo(7.05f, 7.05f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadToRelative(4.95f, 2.05f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadToRelative(-2.05f, 4.95f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadTo(7.05f, 16.95f)
            close()
            moveToRelative(8.49f, -1.41f)
            quadTo(17f, 14.08f, 17f, 12f)
            quadTo(17f, 9.92f, 15.54f, 8.46f)
            reflectiveQuadTo(12f, 7f)
            quadTo(9.93f, 7f, 8.46f, 8.46f)
            reflectiveQuadTo(7f, 12f)
            reflectiveQuadToRelative(1.46f, 3.54f)
            reflectiveQuadTo(12f, 17f)
            reflectiveQuadToRelative(3.54f, -1.46f)
            close()
          }
        }
        .build()
    return _FiberManualRecord!!
  }

private var _FiberManualRecord: ImageVector? = null
