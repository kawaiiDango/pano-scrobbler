package com.arn.scrobble.themes

import android.content.Context
import android.content.Intent
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import com.arn.scrobble.main.App
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.utils.Stuff.getOrDefaultKey
import com.google.android.material.color.DynamicColors

object ColorPatchUtils {

    const val primaryDefault = "Sakurapink"
    const val secondaryDefault = "Deeporange"

    // before Activity.onCreate
    fun setDarkMode(proStatus: Boolean) {
        val prefs = App.prefs

        var dayNightConstant = prefs.themeDayNight
        if (dayNightConstant !in arrayOf(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                AppCompatDelegate.MODE_NIGHT_YES,
                AppCompatDelegate.MODE_NIGHT_NO
            ) || !proStatus
        ) {
            dayNightConstant = AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(dayNightConstant)
    }

    // after Activity.onCreate
    fun setTheme(context: Context, proStatus: Boolean) {
        val prefs = App.prefs

//        var dayNightConstant = prefs.themeDayNight
//        if (dayNightConstant !in arrayOf(
//                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
//                AppCompatDelegate.MODE_NIGHT_YES,
//                AppCompatDelegate.MODE_NIGHT_NO
//            ) || !proStatus
//        ) {
//            dayNightConstant = AppCompatDelegate.MODE_NIGHT_YES
//        }
//        AppCompatDelegate.setDefaultNightMode(dayNightConstant)

        if (prefs.themeDynamic && DynamicColors.isDynamicColorAvailable() && proStatus) {
            if (prefs.themeTintBackground)
                context.theme.applyStyle(R.style.ColorPatchManual_DarkerLightBackground, true)
            else
                context.theme.applyStyle(R.style.ColorPatchManual_Pure_Background, true)

            return
        }

        val isRandom = prefs.themeRandom
        val primaryStyle: String
        val secondaryStyle: String

        if (!proStatus) {
            primaryStyle = primaryDefault
            secondaryStyle = secondaryDefault
        } else if (isRandom) {
            val primaryIdx = (0 until ColorPatchMap.primaryStyles.size).random()
            val secondaryIndex = (0 until ColorPatchMap.secondaryStyles.size).random()
            primaryStyle = ColorPatchMap.primaryStyles.keys.elementAt(primaryIdx)
            secondaryStyle = ColorPatchMap.secondaryStyles.keys.elementAt(secondaryIndex)

            prefs.themePrimary = primaryStyle
            prefs.themeSecondary = secondaryStyle
            context.sendBroadcast(
                Intent(NLService.iTHEME_CHANGED_S)
                    .setPackage(context.packageName),
                NLService.BROADCAST_PERMISSION
            )
        } else {
            primaryStyle = prefs.themePrimary
            secondaryStyle = prefs.themeSecondary
        }

        context.theme.applyStyle(
            ColorPatchMap.primaryStyles
                .getOrDefaultKey(primaryStyle, primaryDefault), true
        )
        context.theme.applyStyle(
            ColorPatchMap.secondaryStyles
                .getOrDefaultKey(secondaryStyle, secondaryDefault), true
        )

        if (prefs.themeTintBackground || !proStatus)
            context.theme.applyStyle(
                ColorPatchMap.backgroundStyles
                    .getOrDefaultKey(primaryStyle, primaryDefault), true
            )
        else
            context.theme.applyStyle(R.style.ColorPatchManual_Pure_Background, true)
    }

    fun getNotiColor(context: Context): Int? {
        val prefs = App.prefs

        if (prefs.proStatus && prefs.themeDynamic && DynamicColors.isDynamicColorAvailable())
            return null

        val primaryStyle = if (prefs.proStatus)
            prefs.themePrimary
        else
            primaryDefault
        return context.getStyledColor(
            ColorPatchMap.primaryStyles
                .getOrDefaultKey(primaryStyle, primaryDefault),
            com.google.android.material.R.attr.colorPrimary
        )
    }

    fun Context.getStyledColor(@StyleRes style: Int, @AttrRes attr: Int): Int {
        val ta = obtainStyledAttributes(style, intArrayOf(attr))
        val color = ta.getColor(0, 0)
        ta.recycle()
        return color
    }
}