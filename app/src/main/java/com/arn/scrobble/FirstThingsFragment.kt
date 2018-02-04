package com.arn.scrobble

import android.app.Fragment
import android.content.Context
import android.content.*
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.NotificationManagerCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.arn.scrobble.pref.AppListFragment
import kotlinx.android.synthetic.main.content_first_things.*
import kotlinx.android.synthetic.main.content_first_things.view.*


/**
 * Created by arn on 06/09/2017.
 */
class FirstThingsFragment: Fragment(), SharedPreferences.OnSharedPreferenceChangeListener  {
    private var stepsNeeded = 4

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_first_things, container, false)

        if (Stuff.isMiui(activity)) {
            view.first_things_0.setOnClickListener {
                Stuff.toast(activity, getString(R.string.check_nls, getString(R.string.app_name)))
                val intent = Intent()
                intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException){
                    Stuff.log("ActivityNotFoundException")
                }
            }
            view.first_things_0.visibility = View.VISIBLE
        }
        view.first_things_1.setOnClickListener {
            Stuff.toast(activity, getString(R.string.check_nls, getString(R.string.app_name)))
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
        view.first_things_2.setOnClickListener {
            Stuff.openInBrowser(Stuff.AUTH_CB_URL, activity)
        }
        view.first_things_3.setOnClickListener {
            fragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
        }

        view.testing_pass.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {

            }

            override fun afterTextChanged(arg0: Editable) {
                val splits = testing_pass.text.split('_')
                Stuff.log("splits: $splits " + splits.size)
                if (splits.size == 3) {
                    PreferenceManager.getDefaultSharedPreferences(activity).edit()
                            .putString(Stuff.USERNAME, splits[0])
                            .putString(Stuff.SESS_KEY, splits[1])
                            .apply()
                    checkAll(true)
                } else
                    Stuff.log("bad pass")
            }

        })
        return view
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        checkAll()
    }

    private fun checkAll(skipChecks:Boolean = false){
        stepsNeeded = 4
        if (checkNLAccess(activity)) {
            markAsDone(R.id.first_things_1)
            if(Stuff.isMiui(activity) && NLService.ensureServiceRunning(activity))
                // needed for cases when a miui user enables autostart AFTER granting NLS permission
                markAsDone(R.id.first_things_0)
            else
                stepsNeeded --
        }
        if (checkAuthTokenExists(activity))
            markAsDone(R.id.first_things_2)
        if (checkAppListExists(activity))
            markAsDone(R.id.first_things_3)

        if(stepsNeeded == 0 || skipChecks) {
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, RecentsFragment(), Stuff.GET_RECENTS)
                    .commit()
            fragmentManager.addOnBackStackChangedListener(activity as Main)
            Main.checkBackStack(activity as Main)
            return
        }
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this)
        Stuff.setTitle(activity, R.string.first_things)
    }

    override fun onResume() {
        super.onResume()
        checkAll()
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    private fun markAsDone(resId:Int){
        val v= activity.findViewById<ViewGroup>(resId)
        v.isEnabled = false
        v.alpha = 0.4f
        val tv = v.getChildAt(0) as TextView
        tv.text = "✅ "
        stepsNeeded --
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
                    .getStringSet(Stuff.PREF_WHITELIST, setOf())
            return set.size > 0
        }
    }
}