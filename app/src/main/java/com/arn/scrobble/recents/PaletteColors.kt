package com.arn.scrobble.recents

import android.content.Context
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.google.android.material.color.MaterialColors

class PaletteColors {
    val primDark: Int
    val lightWhite: Int
    val mutedDark: Int
    val mutedBg: Int

    constructor() {
        primDark = Color.WHITE
        lightWhite = Color.WHITE
        mutedDark = Color.BLACK
        mutedBg = Color.BLACK
    }

    constructor(context: Context, palette: Palette) {
        var prim = palette.getDominantColor(Color.WHITE)
        if (!Stuff.isDark(prim))
            prim = palette.getDarkVibrantColor(
                MaterialColors.getColor(
                    context,
                    R.attr.colorPrimary,
                    null
                )
            )
        primDark = prim

        lightWhite = palette.getLightMutedColor(
            MaterialColors.getColor(
                context,
                R.attr.colorOutline,
                null
            ) or 0xFF000000.toInt()
        )
        mutedDark =
            palette.getDarkMutedColor(MaterialColors.getColor(context, R.attr.colorPrimary, null))
        var bg = palette.getDarkMutedColor(
            MaterialColors.getColor(
                context,
                android.R.attr.colorBackground,
                null
            )
        )

        bg = Stuff.capSatLum(bg, 0.3f, 0.2f)
        mutedBg = bg
    }
}