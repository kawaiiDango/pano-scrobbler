package com.arn.scrobble

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.NotificationManagerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by arn on 06/09/2017.
 */
class FirstThingsFragment: Fragment(), SharedPreferences.OnSharedPreferenceChangeListener  {
    var doneCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = getString(R.string.first_things)
        val vg = activity.findViewById<AppBarLayout>(R.id.app_bar)
        vg.setExpanded(false, true)
        setHasOptionsMenu(false)
        return inflater.inflate(R.layout.content_first_things, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view == null)
            return
        view.findViewById<ViewGroup>(R.id.first_things_1).setOnClickListener {
            Stuff.toast(activity, "Check "+ getString(R.string.app_name))
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
        view.findViewById<ViewGroup>(R.id.first_things_2).setOnClickListener {
            LFMRequester(activity).execute(Stuff.CHECK_AUTH)
        }
        view.findViewById<ViewGroup>(R.id.first_things_3).setOnClickListener {
            fragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
        }

    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        checkAll()
    }

    private fun checkAll(){
        doneCount = 0
        if (checkNLAccess(activity))
            markAsDone(R.id.first_things_1)
        if (checkAuthTokenExists(activity))
            markAsDone(R.id.first_things_2)
        if (checkAppListExists(activity))
            markAsDone(R.id.first_things_3)
        if(doneCount==3) {
            fragmentManager.popBackStack()
            return
        }
        LFMRequester(activity).execute(Stuff.CHECK_AUTH_SILENT)
    }

    override fun onResume() {
        super.onResume()
        checkAll()
        PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun markAsDone(resId:Int){
        val v= activity.findViewById<ViewGroup>(resId)
        v.isEnabled = false
        v.getChildAt(0).visibility = View.VISIBLE
        doneCount++
    }

    companion object {
        fun checkNLAccess(c:Context): Boolean {
            val packages = NotificationManagerCompat.getEnabledListenerPackages(c)
            return packages.any { it == c.packageName }
        }
        fun checkAuthTokenExists(c:Context): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(c)
            return !( pref.getString(Stuff.SESS_KEY, null)== null ||
                    pref.getString(Stuff.USERNAME, null)== null)
        }
        fun checkAppListExists(c:Context): Boolean {
            val set = PreferenceManager.getDefaultSharedPreferences(c)
                    .getStringSet(Stuff.APP_WHITELIST, setOf())
            return set.size > 0
        }
    }
}