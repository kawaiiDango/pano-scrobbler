package com.arn.scrobble.pref

import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.transition.Fade
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.Stuff

/**
 * Created by arn on 09/07/2017.
 */

class PrefFragment : PreferenceFragment(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reenterTransition = Fade()
        addPreferencesFromResource(R.xml.preferences)

        val appList = findPreference("app_whitelist")
        appList.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            fragmentManager.beginTransaction()
                    .remove(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }
//                appList.onPreferenceClickListener

        val reauth = findPreference("reauth")
        reauth.title = reauth.title.toString() +": " + preferenceManager.sharedPreferences.getString("username", "nobody")
        reauth.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            //open browser or intent here
            preferenceManager.sharedPreferences
                    .edit()
                    .remove("sesskey")
                    .apply()
            Stuff.openInBrowser(Stuff.AUTH_CB_URL, activity)
            true
        }

        val about = findPreference("about")
        try {
            about.title = "v " + BuildConfig.VERSION_NAME
            about.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                Stuff.openInBrowser(about.summary.toString(), activity)
                true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

    }

    override fun onResume() {
        super.onResume()
        Stuff.setTitle(activity, R.string.action_settings)
    }

}
