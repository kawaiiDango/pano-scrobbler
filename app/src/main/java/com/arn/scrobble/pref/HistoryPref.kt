package com.arn.scrobble.pref

import android.content.SharedPreferences

class HistoryPref(
    private val pref: SharedPreferences,
    private val prefName: String,
    private val maxItems: Int
) {

    val history = mutableListOf<String>()

    fun load() {
        val historySet = pref.getStringSet(prefName, setOf())!!
        val historyList = mutableListOf<Pair<Int,String>>()
        historySet.forEach {
            val parts = it.split('\n')
            historyList += parts[0].toInt() to parts[1]
        }
        historyList.sortBy { it.first }
        history.clear()
        historyList.forEach {
            history += it.second
        }
    }

    fun save() {
        val historyPrefsSet = mutableSetOf<String>()
        history.takeLast(maxItems).forEachIndexed { i, it ->
            historyPrefsSet += "" + i + "\n" + it.replace(',', ' ')
        }
        pref.edit().putStringSet(prefName, historyPrefsSet).apply()
    }

    fun add(term: String) {
        history.remove(term)
        history.add(0, term)
    }
}