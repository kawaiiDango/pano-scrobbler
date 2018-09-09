package com.arn.scrobble.pref

import android.content.Context
import androidx.preference.PreferenceDataStore
import com.arn.scrobble.Stuff

class MultiPrefsDataStore(context: Context): PreferenceDataStore() {

    val pref = MultiPreferences(context.applicationContext)

    //workaround for bug in preferenceDataStore with PreferenceCompat. Views dont change to specified default values.
    private val defaults = mapOf<String, Any?>(
            Stuff.PREF_MASTER to true,
            Stuff.PREF_NOTIFICATIONS to true,
            Stuff.PREF_AUTO_DETECT to true,
            Stuff.PREF_DELAY_SECS to 90,
            Stuff.PREF_DELAY_PER to 50,
            Stuff.PREF_LB_CUSTOM_ROOT to Stuff.LISTENBRAINZ_API_ROOT,
            Stuff.PREF_LASTFM_DISABLE to false
    )

    override fun getString(key: String, defValue: String?) =
            pref.getString(key, defaults[key] as String?)
    override fun putString(key: String, value: String?) {
        if (value != null)
            pref.putString(key, value)
        else
            pref.remove(key)
    }

    override fun getInt(key: String, defValue: Int) =
            pref.getInt(key, defaults[key] as Int)
    override fun putInt(key: String, value: Int) = pref.putInt(key, value)

    override fun getLong(key: String, defValue: Long) =
            pref.getLong(key, defaults[key] as Long)
    override fun putLong(key: String, value: Long) = pref.putLong(key, value)

    override fun getBoolean(key: String, defValue: Boolean) =
            pref.getBoolean(key, defaults[key] as Boolean)
    override fun putBoolean(key: String, value: Boolean) = pref.putBoolean(key, value)

    override fun getFloat(key: String, defValue: Float) =
            pref.getFloat(key, defaults[key] as Float)
    override fun putFloat(key: String, value: Float) = pref.putFloat(key, value)

    override fun getStringSet(key: String, defValue: Set<String>?) =
            pref.getStringSet(key, defValue) //no defaults for this
    override fun putStringSet(key: String, value: Set<String>?) = pref.putStringSet(key, value)

}