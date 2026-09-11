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
val Icons.WorkspacePremium: ImageVector
  get() {
    if (_WorkspacePremium != null) {
      return _WorkspacePremium!!
    }
    _WorkspacePremium =
      ImageVector.Builder(
          name = "WorkspacePremium",
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
            moveTo(12f, 11.93f)
            lineToRelative(-1.7f, 1.3f)
            quadTo(10.15f, 13.35f, 10f, 13.24f)
            quadTo(9.85f, 13.13f, 9.9f, 12.95f)
            lineToRelative(0.65f, -2.1f)
            lineTo(8.8f, 9.45f)
            quadTo(8.68f, 9.32f, 8.73f, 9.16f)
            quadTo(8.78f, 9f, 8.95f, 9f)
            horizontalLineTo(11.1f)
            lineTo(11.75f, 6.95f)
            quadTo(11.8f, 6.77f, 12f, 6.77f)
            reflectiveQuadToRelative(0.25f, 0.18f)
            lineTo(12.9f, 9f)
            horizontalLineToRelative(2.13f)
            quadToRelative(0.18f, 0f, 0.24f, 0.16f)
            reflectiveQuadTo(15.2f, 9.45f)
            lineToRelative(-1.78f, 1.4f)
            lineToRelative(0.65f, 2.1f)
            quadToRelative(0.05f, 0.18f, -0.1f, 0.29f)
            reflectiveQuadToRelative(-0.3f, -0.01f)
            lineTo(12f, 11.93f)
            close()
            moveTo(12f, 21f)
            lineTo(7.33f, 22.55f)
            quadTo(6.83f, 22.73f, 6.41f, 22.43f)
            reflectiveQuadTo(6f, 21.63f)
            verticalLineTo(15.28f)
            quadTo(5.05f, 14.23f, 4.53f, 12.88f)
            quadTo(4f, 11.52f, 4f, 10f)
            quadTo(4f, 6.65f, 6.33f, 4.32f)
            reflectiveQuadTo(12f, 2f)
            reflectiveQuadToRelative(5.68f, 2.32f)
            reflectiveQuadTo(20f, 10f)
            quadToRelative(0f, 1.52f, -0.52f, 2.88f)
            reflectiveQuadTo(18f, 15.28f)
            verticalLineToRelative(6.35f)
            quadToRelative(0f, 0.5f, -0.41f, 0.8f)
            reflectiveQuadToRelative(-0.91f, 0.13f)
            lineTo(12f, 21f)
            close()
            moveToRelative(4.25f, -6.75f)
            quadTo(18f, 12.5f, 18f, 10f)
            reflectiveQuadTo(16.25f, 5.75f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(7.75f, 5.75f)
            reflectiveQuadTo(6f, 10f)
            reflectiveQuadToRelative(1.75f, 4.25f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(4.25f, -1.75f)
            close()
            moveTo(8f, 20.02f)
            lineTo(12f, 19f)
            lineToRelative(4f, 1.02f)
            verticalLineToRelative(-3.1f)
            quadToRelative(-0.88f, 0.5f, -1.89f, 0.79f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadTo(9.89f, 17.71f)
            quadTo(8.88f, 17.43f, 8f, 16.93f)
            verticalLineToRelative(3.1f)
            close()
            moveToRelative(4f, -1.55f)
            close()
          }
        }
        .build()
    return _WorkspacePremium!!
  }

private var _WorkspacePremium: ImageVector? = null
