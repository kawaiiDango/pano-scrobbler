package com.arn.scrobble.recents

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.google.android.material.color.MaterialColors

class PaletteColors {
    var primDark = Color.WHITE
    var lightWhite = Color.WHITE
    var mutedDark = Color.BLACK
    var mutedBlack = Color.BLACK

    constructor()

    constructor(context: Context, palette: Palette) {
        primDark = palette.getDominantColor(Color.WHITE)
        if (!Stuff.isDark(primDark))
            primDark = palette.getDarkVibrantColor(MaterialColors.getColor(context, R.attr.colorPrimary, null))

        lightWhite = palette.getLightMutedColor(ContextCompat.getColor(context, android.R.color.primary_text_dark))
        mutedDark = palette.getDarkMutedColor(MaterialColors.getColor(context, R.attr.colorPrimary, null))
        mutedBlack = palette.getDarkMutedColor(MaterialColors.getColor(context, android.R.attr.colorBackground, null))
    }
}