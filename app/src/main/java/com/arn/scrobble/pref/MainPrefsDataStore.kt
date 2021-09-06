package com.arn.scrobble.pref

import android.content.Context
import androidx.preference.PreferenceDataStore

class MainPrefsDataStore(context: Context): PreferenceDataStore() {

    private val prefs = MainPrefs(context.applicationContext).sharedPreferences

    override fun getString(key: String, defValue: String?) = prefs.getString(key, defValue)
    override fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    override fun getInt(key: String, defValue: Int) = prefs.getInt(key, defValue)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    override fun getLong(key: String, defValue: Long) = prefs.getLong(key, defValue)
    override fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()

    override fun getBoolean(key: String, defValue: Boolean) = prefs.getBoolean(key, defValue)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    override fun getFloat(key: String, defValue: Float) = prefs.getFloat(key, defValue)
    override fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()

    override fun getStringSet(key: String, defValue: Set<String>?) = prefs.getStringSet(key, defValue)
    override fun putStringSet(key: String, value: Set<String>?) = prefs.edit().putStringSet(key, value).apply()

}