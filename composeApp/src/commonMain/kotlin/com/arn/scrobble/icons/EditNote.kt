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
val Icons.EditNote: ImageVector
  get() {
    if (_EditNote != null) {
      return _EditNote!!
    }
    _EditNote =
      ImageVector.Builder(
          name = "EditNote",
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
            moveTo(5f, 14f)
            quadTo(4.58f, 14f, 4.29f, 13.71f)
            quadTo(4f, 13.43f, 4f, 13f)
            reflectiveQuadTo(4.29f, 12.29f)
            reflectiveQuadTo(5f, 12f)
            horizontalLineToRelative(5f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(11f, 13f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(10f, 14f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 10f)
            quadTo(4.58f, 10f, 4.29f, 9.71f)
            reflectiveQuadTo(4f, 9f)
            quadTo(4f, 8.57f, 4.29f, 8.29f)
            reflectiveQuadTo(5f, 8f)
            horizontalLineToRelative(9f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(14f, 10f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 6f)
            quadTo(4.58f, 6f, 4.29f, 5.71f)
            quadTo(4f, 5.43f, 4f, 5f)
            reflectiveQuadTo(4.29f, 4.29f)
            reflectiveQuadTo(5f, 4f)
            horizontalLineToRelative(9f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 5f)
            reflectiveQuadTo(14.71f, 5.71f)
            reflectiveQuadTo(14f, 6f)
            horizontalLineTo(5f)
            close()
            moveToRelative(8f, 13f)
            verticalLineTo(17.35f)
            quadToRelative(0f, -0.2f, 0.08f, -0.39f)
            reflectiveQuadTo(13.3f, 16.63f)
            lineToRelative(5.23f, -5.2f)
            quadToRelative(0.22f, -0.22f, 0.5f, -0.32f)
            reflectiveQuadTo(19.58f, 11f)
            quadToRelative(0.3f, 0f, 0.57f, 0.11f)
            quadToRelative(0.27f, 0.11f, 0.5f, 0.34f)
            lineToRelative(0.93f, 0.93f)
            quadToRelative(0.2f, 0.22f, 0.31f, 0.5f)
            reflectiveQuadTo(22f, 13.43f)
            reflectiveQuadToRelative(-0.1f, 0.56f)
            reflectiveQuadTo(21.58f, 14.5f)
            lineToRelative(-5.2f, 5.2f)
            quadToRelative(-0.15f, 0.15f, -0.34f, 0.23f)
            reflectiveQuadTo(15.65f, 20f)
            horizontalLineTo(14f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(13f, 19.43f, 13f, 19f)
            close()
            moveToRelative(7.5f, -5.58f)
            lineTo(19.58f, 12.5f)
            lineToRelative(0.92f, 0.92f)
            close()
            moveToRelative(-6f, 5.08f)
            horizontalLineToRelative(0.95f)
            lineToRelative(3.03f, -3.05f)
            lineTo(18.03f, 14.98f)
            lineTo(17.55f, 14.53f)
            lineTo(14.5f, 17.55f)
            verticalLineTo(18.5f)
            close()
            moveToRelative(3.53f, -3.53f)
            lineTo(17.55f, 14.53f)
            lineToRelative(0.93f, 0.92f)
            lineTo(18.03f, 14.98f)
            close()
          }
        }
        .build()
    return _EditNote!!
  }

private var _EditNote: ImageVector? = null
