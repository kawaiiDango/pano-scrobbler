package com.arn.scrobble.pref

import android.content.Context
import com.frybits.harmony.getHarmonySharedPreferences
import hu.autsoft.krate.*

class WidgetPrefs(context: Context) {

    val sharedPreferences = context.getHarmonySharedPreferences(NAME)

    operator fun get(widgetId: Int)  = SpecificWidgetPrefs(widgetId)

    fun chartsData(tab: Int, period: Int) = ChartsData(tab, period)

    inner class ChartsData(tab: Int, period: Int): Krate {
        override val sharedPreferences = this@WidgetPrefs.sharedPreferences

        var data by stringPref("${tab}_$period")
    }

    inner class SpecificWidgetPrefs(private val widgetId: Int): Krate {
        override val sharedPreferences = this@WidgetPrefs.sharedPreferences

        var tab by intPref(getWidgetPrefName(PREF_WIDGET_TAB))
        var bgAlpha by floatPref(getWidgetPrefName(PREF_WIDGET_BG_ALPHA), 0.4f)
        var isDark by booleanPref(getWidgetPrefName(PREF_WIDGET_DARK), true)
        var period by intPref(getWidgetPrefName(PREF_WIDGET_PERIOD))
        var shadow by booleanPref(getWidgetPrefName(PREF_WIDGET_SHADOW), true)
        var lastUpdated by longPref(getWidgetPrefName(PREF_WIDGET_LAST_UPDATED))

        fun clear() {
            sharedPreferences.edit().apply {
                sharedPreferences.all
                    .keys
                    .toList()
                    .forEach { key ->
                        if (key.endsWith("_$widgetId"))
                            remove(key)
                    }
                apply()
            }
        }

        private fun getWidgetPrefName(name: String) = "${name}_$widgetId"
    }

    companion object {
        const val NAME = "widget"
        const val PREF_WIDGET_TAB = "tab"
        const val PREF_WIDGET_BG_ALPHA = "bg_alpha"
        const val PREF_WIDGET_DARK = "dark"
        const val PREF_WIDGET_PERIOD = "period"
        const val PREF_WIDGET_SHADOW = "shadow"
        const val PREF_WIDGET_LAST_UPDATED = "last_updated"
    }
}