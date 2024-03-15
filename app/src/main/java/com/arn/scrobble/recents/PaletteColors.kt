package com.arn.scrobble.recents

import android.content.Context
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.arn.scrobble.R
import com.arn.scrobble.utils.UiUtils
import com.google.android.material.color.MaterialColors

class PaletteColors {
    private val primDark: Int
    private val lightWhite: Int
    private val mutedDark: Int
    private val mutedBgDark: Int

    private val primLight: Int
    private val darkBlack: Int
    private val mutedLight: Int
    private val mutedBgLight: Int

    private var isDark = false

    val primary
        get() = if (isDark) primDark else primLight
    val foreground
        get() = if (isDark) lightWhite else darkBlack
    val muted
        get() = if (isDark) mutedDark else mutedLight
    val background
        get() = if (isDark) mutedBgDark else mutedBgLight


    constructor(context: Context) {
        primDark = Color.BLACK
        lightWhite = Color.WHITE
        mutedDark = Color.BLACK
        mutedBgDark = Color.BLACK

        primLight = Color.WHITE
        darkBlack = Color.BLACK
        mutedLight = Color.WHITE
        mutedBgLight = Color.WHITE
        setDarkModeFrom(context)
    }

    constructor(context: Context, palette: Palette) {
        fun Int.harmonize() = MaterialColors.harmonizeWithPrimary(context, this)

        var dom = palette.getDominantColor(Color.WHITE)
        if (!UiUtils.isDark(dom))
            dom = palette.getDarkVibrantColor(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorPrimary,
                    null
                )
            )
        primDark = dom.harmonize()

        var fg = palette.getLightMutedColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOutline,
                null
            ) or 0xFF000000.toInt()
        )

        lightWhite = UiUtils.capMinSatLum(fg, 0.45f, 0.7f, 0.85f).harmonize()

        fg = palette.getDarkMutedColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorPrimary,
                null
            )
        )

        mutedDark = UiUtils.capMaxSatLum(fg, 0.2f, 0.2f).harmonize()

        var bg = palette.getDarkMutedColor(
            MaterialColors.getColor(
                context,
                android.R.attr.colorBackground,
                null
            )
        )

        bg = UiUtils.capMaxSatLum(bg, 0.4f, 0.2f)
        mutedBgDark = bg.harmonize()


        dom = palette.getDominantColor(Color.BLACK)
        if (UiUtils.isDark(dom))
            dom = palette.getLightVibrantColor(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorPrimary,
                    null
                )
            )
        primLight = dom.harmonize()

        darkBlack = palette.getDarkVibrantColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorPrimary,
                null
            ) or 0xFF000000.toInt()
        ).harmonize()
        mutedLight =
            palette.getLightMutedColor(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorPrimary,
                    null
                )
            ).harmonize()
        bg = palette.getLightMutedColor(
            MaterialColors.getColor(
                context,
                android.R.attr.colorBackground,
                null
            )
        )

        bg = UiUtils.capMinSatLum(bg, 0.45f, 0.7f, 0.9f)
        mutedBgLight = bg.harmonize()

        setDarkModeFrom(context)
    }

    fun setDarkModeFrom(context: Context) {
        isDark = context.resources.getBoolean(R.bool.is_dark)
    }
}