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
val Icons.Api: ImageVector
  get() {
    if (_Api != null) {
      return _Api!!
    }
    _Api =
      ImageVector.Builder(
          name = "Api",
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
            moveTo(12f, 14f)
            lineTo(10f, 12f)
            lineToRelative(2f, -2f)
            lineToRelative(2f, 2f)
            lineToRelative(-2f, 2f)
            close()
            moveTo(9.88f, 8.13f)
            lineTo(7.38f, 5.63f)
            lineToRelative(3.2f, -3.2f)
            quadToRelative(0.3f, -0.3f, 0.67f, -0.45f)
            reflectiveQuadTo(12f, 1.82f)
            reflectiveQuadToRelative(0.75f, 0.15f)
            quadToRelative(0.38f, 0.15f, 0.68f, 0.45f)
            lineToRelative(3.2f, 3.2f)
            lineToRelative(-2.5f, 2.5f)
            lineTo(12f, 6f)
            lineTo(9.88f, 8.13f)
            close()
            moveToRelative(-4.25f, 8.5f)
            lineToRelative(-3.2f, -3.2f)
            quadTo(2.13f, 13.13f, 1.98f, 12.75f)
            reflectiveQuadTo(1.83f, 12f)
            reflectiveQuadTo(1.98f, 11.25f)
            reflectiveQuadTo(2.43f, 10.58f)
            lineToRelative(3.2f, -3.2f)
            lineToRelative(2.5f, 2.5f)
            lineTo(6f, 12f)
            lineToRelative(2.13f, 2.13f)
            lineToRelative(-2.5f, 2.5f)
            close()
            moveToRelative(12.75f, 0f)
            lineToRelative(-2.5f, -2.5f)
            lineTo(18f, 12f)
            lineTo(15.88f, 9.88f)
            lineToRelative(2.5f, -2.5f)
            lineToRelative(3.2f, 3.2f)
            quadToRelative(0.3f, 0.3f, 0.45f, 0.68f)
            reflectiveQuadTo(22.18f, 12f)
            reflectiveQuadToRelative(-0.15f, 0.75f)
            reflectiveQuadToRelative(-0.45f, 0.67f)
            lineToRelative(-3.2f, 3.2f)
            close()
            moveToRelative(-7.8f, 4.95f)
            lineToRelative(-3.2f, -3.2f)
            lineToRelative(2.5f, -2.5f)
            lineTo(12f, 18f)
            lineToRelative(2.13f, -2.13f)
            lineToRelative(2.5f, 2.5f)
            lineToRelative(-3.2f, 3.2f)
            quadToRelative(-0.3f, 0.3f, -0.68f, 0.45f)
            quadTo(12.38f, 22.18f, 12f, 22.18f)
            reflectiveQuadTo(11.25f, 22.03f)
            quadTo(10.88f, 21.88f, 10.58f, 21.58f)
            close()
          }
        }
        .build()
    return _Api!!
  }

private var _Api: ImageVector? = null
