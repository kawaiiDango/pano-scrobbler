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
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.arn.scrobble.pref.AppListFragment


/**
 * Created by arn on 06/09/2017.
 */
class FirstThingsFragment: Fragment(), SharedPreferences.OnSharedPreferenceChangeListener  {
    var doneCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vg = activity.findViewById<AppBarLayout>(R.id.app_bar)
        vg.setExpanded(false, true)
        setHasOptionsMenu(false)
        return inflater.inflate(R.layout.content_first_things, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ab = (activity as AppCompatActivity).supportActionBar ?: return
        ab.setDisplayHomeAsUpEnabled(false)
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

        val pass = view.findViewById<EditText>(R.id.testing_pass)
        pass.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {

            }

            override fun afterTextChanged(arg0: Editable) {
                //            val pass = view.findViewById<TextView>(R.id.testing_pass)
                val splits = pass.text.split('_')
                Stuff.log("splits: $splits " + splits.size)
                if (splits.size == 3) {
                    PreferenceManager.getDefaultSharedPreferences(activity).edit()
                            .putString(Stuff.USERNAME, splits[0])
                            .putString(Stuff.SESS_KEY, splits[1])
                            .apply()
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame, RecentsFragment(), Stuff.GET_RECENTS)
                            .commit()
                } else
                    Stuff.log("bad pass")
            }

        })
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
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, RecentsFragment(), Stuff.GET_RECENTS)
                    .commit()
            val ab = activity.findViewById<AppBarLayout>(R.id.app_bar)
            ab?.setExpanded(true, true)
            return
        }
        LFMRequester(activity).execute(Stuff.CHECK_AUTH_SILENT)
    }

    override fun onResume() {
        super.onResume()
        checkAll()
        PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this)
        Stuff.setTitle(activity, R.string.first_things)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun markAsDone(resId:Int){
        val v= activity.findViewById<ViewGroup>(resId)
        v.isEnabled = false
        v.alpha = 0.4f
        val tv = v.getChildAt(0) as TextView
        tv.text = "✅ "
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