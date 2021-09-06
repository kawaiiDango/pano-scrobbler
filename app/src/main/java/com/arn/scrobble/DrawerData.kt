package com.arn.scrobble

import android.content.Context
import com.arn.scrobble.pref.MainPrefs

class DrawerData (
    val scrobblesToday: Int,
    val scrobblesTotal: Int,
    val registeredDate: Long,
    val profilePicUrl: String,
) {
    fun saveToPref(context: Context) {
        MainPrefs(context).apply {
            scrobblesTodayCached = scrobblesToday
            scrobblesTotalCached = scrobblesTotal
            scrobblingSince = registeredDate
            profilePicUrlCached = profilePicUrl
        }
    }

    companion object {
        fun loadFromPref(context: Context): DrawerData {
            val prefs = MainPrefs(context)

            return DrawerData(
                prefs.scrobblesTodayCached,
                prefs.scrobblesTotalCached,
                prefs.scrobblingSince,
                prefs.profilePicUrlCached ?: "",
            )
        }
    }
}