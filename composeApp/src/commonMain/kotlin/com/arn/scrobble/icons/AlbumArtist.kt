package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PanoIcons.AlbumArtist: ImageVector
    get() {
        if (_AlbumArtist != null) {
            return _AlbumArtist!!
        }
        _AlbumArtist = ImageVector.Builder(
            name = "AlbumArtist",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 16.043f, 4.043f, 19f, 8.156f, 19.813f)
                curveTo(9.333f, 20.045f, 9.794f, 19.751f, 9.885f, 19.14f)
                curveTo(9.992f, 18.415f, 8.86f, 17.986f, 8.156f, 17.781f)
                curveTo(7.19f, 18.915f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(15.326f, 4f, 17.644f, 6.016f, 18.759f, 7.766f)
                curveTo(19.874f, 9.516f, 20.549f, 9.099f, 20.756f, 8.966f)
                curveTo(21.07f, 8.766f, 21.493f, 8.446f, 20.538f, 6.873f)
                curveTo(19.562f, 5.266f, 16.858f, 2f, 12f, 2f)
                close()
                moveTo(12f, 7.5f)
                curveTo(9.51f, 7.5f, 7.5f, 9.51f, 7.5f, 12f)
                curveTo(7.5f, 14.295f, 9.214f, 16.162f, 11.432f, 16.443f)
                curveTo(11.366f, 16.073f, 11.332f, 15.697f, 11.332f, 15.32f)
                curveTo(11.332f, 14.511f, 11.482f, 13.71f, 11.775f, 12.955f)
                curveTo(11.335f, 12.85f, 11f, 12.471f, 11f, 12f)
                curveTo(11f, 11.45f, 11.45f, 11f, 12f, 11f)
                curveTo(12.278f, 11f, 12.53f, 11.116f, 12.71f, 11.3f)
                curveTo(13.438f, 10.364f, 14.405f, 9.642f, 15.51f, 9.211f)
                curveTo(14.686f, 8.175f, 13.43f, 7.5f, 12f, 7.5f)
                close()
                moveTo(17.576f, 17.574f)
                curveTo(18.638f, 17.574f, 19.495f, 16.717f, 19.495f, 15.655f)
                verticalLineTo(11.817f)
                curveTo(19.495f, 10.755f, 18.638f, 9.898f, 17.576f, 9.898f)
                curveTo(16.515f, 9.898f, 15.658f, 10.755f, 15.658f, 11.817f)
                verticalLineTo(15.655f)
                curveTo(15.658f, 16.717f, 16.515f, 17.574f, 17.576f, 17.574f)
                close()
                moveTo(16.936f, 11.817f)
                curveTo(16.936f, 11.465f, 17.224f, 11.177f, 17.576f, 11.177f)
                curveTo(17.928f, 11.177f, 18.216f, 11.465f, 18.216f, 11.817f)
                verticalLineTo(15.655f)
                curveTo(18.216f, 16.007f, 17.928f, 16.295f, 17.576f, 16.295f)
                curveTo(17.224f, 16.295f, 16.936f, 16.007f, 16.936f, 15.655f)
                close()
                moveTo(21.356f, 15.655f)
                curveTo(21.043f, 15.655f, 20.781f, 15.885f, 20.73f, 16.199f)
                curveTo(20.467f, 17.702f, 19.156f, 18.854f, 17.576f, 18.854f)
                curveTo(15.996f, 18.854f, 14.684f, 17.702f, 14.421f, 16.199f)
                curveTo(14.37f, 15.885f, 14.108f, 15.655f, 13.794f, 15.655f)
                curveTo(13.404f, 15.655f, 13.097f, 16.001f, 13.154f, 16.385f)
                curveTo(13.468f, 18.304f, 15.004f, 19.807f, 16.935f, 20.082f)
                verticalLineTo(21.412f)
                curveTo(16.935f, 21.764f, 17.223f, 22.052f, 17.575f, 22.052f)
                curveTo(17.927f, 22.052f, 18.215f, 21.764f, 18.215f, 21.412f)
                verticalLineTo(20.082f)
                curveTo(20.147f, 19.807f, 21.682f, 18.304f, 21.996f, 16.385f)
                curveTo(22.06f, 16.001f, 21.746f, 15.655f, 21.356f, 15.655f)
                close()
            }
        }.build()
        return _AlbumArtist!!
    }

private var _AlbumArtist: ImageVector? = null
