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
val Icons.Description: ImageVector
  get() {
    if (_Description != null) {
      return _Description!!
    }
    _Description =
      ImageVector.Builder(
          name = "Description",
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
            moveTo(9f, 18f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(16f, 17.43f, 16f, 17f)
            reflectiveQuadTo(15.71f, 16.29f)
            reflectiveQuadTo(15f, 16f)
            horizontalLineTo(9f)
            quadTo(8.58f, 16f, 8.29f, 16.29f)
            reflectiveQuadTo(8f, 17f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            quadTo(8.58f, 18f, 9f, 18f)
            close()
            moveTo(9f, 14f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, -0.29f)
            quadTo(16f, 13.43f, 16f, 13f)
            reflectiveQuadTo(15.71f, 12.29f)
            reflectiveQuadTo(15f, 12f)
            horizontalLineTo(9f)
            quadTo(8.58f, 12f, 8.29f, 12.29f)
            reflectiveQuadTo(8f, 13f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            quadTo(8.58f, 14f, 9f, 14f)
            close()
            moveTo(6f, 22f)
            quadTo(5.18f, 22f, 4.59f, 21.41f)
            reflectiveQuadTo(4f, 20f)
            verticalLineTo(4f)
            quadTo(4f, 3.17f, 4.59f, 2.59f)
            reflectiveQuadTo(6f, 2f)
            horizontalLineToRelative(7.18f)
            quadToRelative(0.4f, 0f, 0.76f, 0.15f)
            reflectiveQuadToRelative(0.64f, 0.43f)
            lineToRelative(4.85f, 4.85f)
            quadTo(19.7f, 7.7f, 19.85f, 8.06f)
            quadTo(20f, 8.42f, 20f, 8.82f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 22f)
            horizontalLineTo(6f)
            close()
            moveTo(13f, 8f)
            verticalLineTo(4f)
            horizontalLineTo(6f)
            verticalLineTo(20f)
            horizontalLineTo(18f)
            verticalLineTo(9f)
            horizontalLineTo(14f)
            quadTo(13.58f, 9f, 13.29f, 8.71f)
            reflectiveQuadTo(13f, 8f)
            close()
            moveTo(6f, 4f)
            verticalLineTo(8f)
            quadTo(6f, 8.42f, 6f, 8.71f)
            reflectiveQuadTo(6f, 9f)
            verticalLineTo(4f)
            verticalLineTo(8f)
            quadTo(6f, 8.42f, 6f, 8.71f)
            reflectiveQuadTo(6f, 9f)
            verticalLineTo(20f)
            verticalLineTo(4f)
            close()
          }
        }
        .build()
    return _Description!!
  }

private var _Description: ImageVector? = null
