package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PanoIcons.MatchCase: ImageVector
    get() {
        if (_MatchCase != null) {
            return _MatchCase!!
        }
        _MatchCase = ImageVector.Builder(
            name = "MatchCase",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(131f, 708f)
                lineTo(296f, 268f)
                lineTo(375f, 268f)
                lineTo(540f, 708f)
                lineTo(464f, 708f)
                lineTo(425f, 596f)
                lineTo(247f, 596f)
                lineTo(207f, 708f)
                lineTo(131f, 708f)
                close()
                moveTo(270f, 532f)
                lineTo(401f, 532f)
                lineTo(337f, 350f)
                lineTo(333f, 350f)
                lineTo(270f, 532f)
                close()
                moveTo(665f, 718f)
                quadTo(614f, 718f, 584f, 690.5f)
                quadTo(554f, 663f, 554f, 618f)
                quadTo(554f, 574f, 588.5f, 545.5f)
                quadTo(623f, 517f, 677f, 517f)
                quadTo(700f, 517f, 722f, 521f)
                quadTo(744f, 525f, 760f, 532f)
                lineTo(760f, 520f)
                quadTo(760f, 491f, 739.5f, 473f)
                quadTo(719f, 455f, 685f, 455f)
                quadTo(662f, 455f, 643f, 464.5f)
                quadTo(624f, 474f, 610f, 492f)
                lineTo(563f, 457f)
                quadTo(587f, 428f, 617.5f, 414f)
                quadTo(648f, 400f, 686f, 400f)
                quadTo(755f, 400f, 789f, 432.5f)
                quadTo(823f, 465f, 823f, 530f)
                lineTo(823f, 708f)
                lineTo(760f, 708f)
                lineTo(760f, 671f)
                lineTo(756f, 671f)
                quadTo(742f, 694f, 718f, 706f)
                quadTo(694f, 718f, 665f, 718f)
                close()
                moveTo(677f, 664f)
                quadTo(712f, 664f, 736.5f, 640f)
                quadTo(761f, 616f, 761f, 584f)
                quadTo(747f, 576f, 727.5f, 571.5f)
                quadTo(708f, 567f, 689f, 567f)
                quadTo(657f, 567f, 639f, 581f)
                quadTo(621f, 595f, 621f, 618f)
                quadTo(621f, 638f, 637f, 651f)
                quadTo(653f, 664f, 677f, 664f)
                close()
            }
        }.build()

        return _MatchCase!!
    }

@Suppress("ObjectPropertyName")
private var _MatchCase: ImageVector? = null
