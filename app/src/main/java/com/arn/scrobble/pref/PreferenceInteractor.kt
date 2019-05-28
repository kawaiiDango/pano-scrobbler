package com.arn.scrobble.pref

import android.content.Context
import android.content.SharedPreferences

/**
 * Preference Interactor class
 *
 * - Accesses Shared Preferences and returns a (Matrix) Cursor Object
 */
class PreferenceInteractor(mContext: Context, mPreferenceName: String) {
    private val mSharedPreferences: SharedPreferences = mContext.getSharedPreferences(mPreferenceName, Context.MODE_PRIVATE)

    fun getString(key: String, defaultVal: String?): String? = mSharedPreferences.getString(key, defaultVal)
    fun setString(key: String, value: String) = mSharedPreferences.edit().putString(key, value).apply()

    fun getInt(key: String, defaultVal: Int) = mSharedPreferences.getInt(key, defaultVal)
    fun setInt(key: String, value: Int) = mSharedPreferences.edit().putInt(key, value).apply()

    fun getLong(key: String, defaultVal: Long) = mSharedPreferences.getLong(key, defaultVal)
    fun setLong(key: String, value: Long) = mSharedPreferences.edit().putLong(key, value).apply()

    fun getBoolean(key: String, defaultVal: Boolean) = mSharedPreferences.getBoolean(key, defaultVal)
    fun setBoolean(key: String, value: Boolean) = mSharedPreferences.edit().putBoolean(key, value).apply()

    fun getFloat(key: String, defaultVal: Float) = mSharedPreferences.getFloat(key, defaultVal)
    fun setFloat(key: String, value: Float) = mSharedPreferences.edit().putFloat(key, value).apply()

    fun getStringSet(key: String, defaultVal: Set<String>): Set<String>? = mSharedPreferences.getStringSet(key, defaultVal)
    fun setStringSet(key: String, value: Set<String>) = mSharedPreferences.edit().putStringSet(key, value).apply()

    fun size() = mSharedPreferences.all.size

    fun removePref(key: String) {
        mSharedPreferences.edit().remove(key).apply()
    }

    fun clearPreference() {
        mSharedPreferences.edit().clear().apply()
    }
}
