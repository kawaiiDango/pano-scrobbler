package com.arn.scrobble.themes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.getOrDefaultKey
import com.arn.scrobble.pref.MultiPreferences
import kotlin.random.Random

object ColorPatchUtils {

    const val primaryDefault = "Pink"
    const val secondaryDefault = "Purple"
    const val backgroundDefault = "Black"

    fun setTheme(context: Context) {
        val pref = MultiPreferences(context)
        val random = pref.getBoolean(Stuff.PREF_THEME_RANDOM, false)
        val sameTone = pref.getBoolean(Stuff.PREF_THEME_SAME_TONE, false)
        val primaryStyle: String
        val secondaryStyle: String
        val backgroundStyle: String
        if (random) {
            val r = Random(System.currentTimeMillis())
            val i1 = r.nextInt(0, ColorPatchMap.primaryStyles.size)
            val i2: Int
            val i3: Int
            if (sameTone) {
                i2 = i1
                i3 = i1
            } else {
                i2 = r.nextInt(0, ColorPatchMap.secondaryStyles.size)
                i3 = r.nextInt(0, ColorPatchMap.backgroundStyles.size)
            }
            primaryStyle = ColorPatchMap.primaryStyles.keys.elementAt(i1)
            secondaryStyle = ColorPatchMap.secondaryStyles.keys.elementAt(i2)
            backgroundStyle = ColorPatchMap.backgroundStyles.keys.elementAt(i3)

            pref.putString(Stuff.PREF_THEME_PRIMARY, primaryStyle)
            pref.putString(Stuff.PREF_THEME_SECONDARY, secondaryStyle)
            pref.putString(Stuff.PREF_THEME_BACKGROUND, backgroundStyle)
            context.sendBroadcast(Intent(NLService.iTHEME_CHANGED))
        } else {
            primaryStyle = pref.getString(Stuff.PREF_THEME_PRIMARY, primaryDefault)!!
            secondaryStyle = pref.getString(Stuff.PREF_THEME_SECONDARY, secondaryDefault)!!
            backgroundStyle = pref.getString(Stuff.PREF_THEME_BACKGROUND, backgroundDefault)!!
        }

        context.theme.applyStyle(ColorPatchMap.primaryStyles
            .getOrDefaultKey(primaryStyle, primaryDefault), true)
        context.theme.applyStyle(ColorPatchMap.secondaryStyles
            .getOrDefaultKey(secondaryStyle, secondaryDefault), true)
        context.theme.applyStyle(ColorPatchMap.backgroundStyles
            .getOrDefaultKey(backgroundStyle, backgroundDefault), true)
    }

    fun getNotiColor(context: Context, pref: SharedPreferences): Int {
        val primaryStyle = if (pref.getBoolean(Stuff.PREF_PRO_STATUS, false))
            pref.getString(Stuff.PREF_THEME_PRIMARY, primaryDefault)!!
        else
            primaryDefault
        return context.getStyledColor(ColorPatchMap.primaryStyles
            .getOrDefaultKey(primaryStyle, primaryDefault), R.attr.colorNoti)
    }

    fun getNotiColor(context: Context, pref: MultiPreferences): Int {
        val primaryStyle = if (pref.getBoolean(Stuff.PREF_PRO_STATUS, false))
            pref.getString(Stuff.PREF_THEME_PRIMARY, primaryDefault)!!
        else
            primaryDefault
        return context.getStyledColor(ColorPatchMap.primaryStyles
            .getOrDefaultKey(primaryStyle, primaryDefault), R.attr.colorNoti)
    }

    fun Context.getStyledColor(@StyleRes style: Int, @AttrRes attr: Int): Int {
        val ta = obtainStyledAttributes(style, intArrayOf(attr))
        val color = ta.getColor(0, 0)
        ta.recycle()
        return color
    }
}