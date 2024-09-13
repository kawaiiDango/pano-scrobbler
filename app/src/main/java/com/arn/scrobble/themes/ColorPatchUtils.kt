package com.arn.scrobble.themes

import android.content.Context
import android.content.Intent
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import com.arn.scrobble.NLService
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getOrDefaultKey
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object ColorPatchUtils {

    const val primaryDefault = "Sakurapink"
    const val secondaryDefault = "Deeporange"

    // before Activity.onCreate
    fun setDarkMode() {
        val proStatus = Stuff.billingRepository.isLicenseValid
        val themeDayNight =
            runBlocking { PlatformStuff.mainPrefs.data.map { it.themeDayNight }.first() }

        var dayNightConstant = when (themeDayNight) {
            DayNightMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            DayNightMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (!proStatus) {
            dayNightConstant = AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(dayNightConstant)
    }

    // after Activity.onCreate
    fun setTheme(context: Context) {
        val proStatus = Stuff.billingRepository.isLicenseValid
        val prefs = runBlocking { PlatformStuff.mainPrefs.data.first() }

        if (prefs.themeDynamic && DynamicColors.isDynamicColorAvailable() && proStatus) {
            context.theme.applyStyle(R.style.ColorPatchManual_DarkerLightBackground, true)
            return
        }

        val isRandom = false
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

//            runBlocking {
//                PlatformStuff.mainPrefs.updateData {
//                    it.copy(themePrimary = primaryStyle, themeSecondary = secondaryStyle)
//                }
//            }

            context.sendBroadcast(
                Intent(NLService.iTHEME_CHANGED_S)
                    .setPackage(context.packageName),
                NLService.BROADCAST_PERMISSION
            )
        } else {
            primaryStyle = primaryDefault
            secondaryStyle = secondaryDefault
        }

        context.theme.applyStyle(
            ColorPatchMap.primaryStyles
                .getOrDefaultKey(primaryStyle, primaryDefault), true
        )
        context.theme.applyStyle(
            ColorPatchMap.secondaryStyles
                .getOrDefaultKey(secondaryStyle, secondaryDefault), true
        )

        context.theme.applyStyle(
            ColorPatchMap.backgroundStyles
                .getOrDefaultKey(primaryStyle, primaryDefault), true
        )
    }

    fun getNotiColor(context: Context): Int? {
        val proStatus = Stuff.billingRepository.isLicenseValid
        val themeDynamic =
            runBlocking { PlatformStuff.mainPrefs.data.map { it.themeDynamic }.first() }

        if (proStatus && themeDynamic && DynamicColors.isDynamicColorAvailable())
            return null

        val primaryStyle = primaryDefault
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