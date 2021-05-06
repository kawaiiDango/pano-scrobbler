package com.arn.scrobble

import android.content.Context

class DrawerData (
    val todayScrobbles: Int,
    val totalScrobbles: Int,
    val registeredDate: Long,
    val profilePicUrl: String,
) {
    fun saveToPref(context: Context) {
        val actPref = context.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
        actPref.edit()
            .putInt(Stuff.PREF_ACTIVITY_TODAY_SCROBBLES, todayScrobbles)
            .putInt(Stuff.PREF_ACTIVITY_TOTAL_SCROBBLES, totalScrobbles)
            .putLong(Stuff.PREF_ACTIVITY_SCROBBLING_SINCE, registeredDate)
            .putString(Stuff.PREF_ACTIVITY_PROFILE_PIC, profilePicUrl)
            .apply()
    }

    companion object {
        fun loadFromPref(context: Context): DrawerData {
            val actPref = context.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)

            return DrawerData(
                actPref.getInt(Stuff.PREF_ACTIVITY_TODAY_SCROBBLES, 0),
                actPref.getInt(Stuff.PREF_ACTIVITY_TOTAL_SCROBBLES, 0),
                actPref.getLong(Stuff.PREF_ACTIVITY_SCROBBLING_SINCE, 0),
                actPref.getString(Stuff.PREF_ACTIVITY_PROFILE_PIC, "")!!
            )
        }
    }
}