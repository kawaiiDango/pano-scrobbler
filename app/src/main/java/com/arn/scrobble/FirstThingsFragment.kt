package com.arn.scrobble

import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.MultiPreferences
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_first_things.*
import kotlinx.android.synthetic.main.content_first_things.view.*
import kotlinx.android.synthetic.main.coordinator_main.*


/**
 * Created by arn on 06/09/2017.
 */
class FirstThingsFragment: Fragment() {
    private var stepsNeeded = 4
    private lateinit var pref: MultiPreferences
    private var startupMgrIntent:Intent? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_first_things, container, false)
        pref = MultiPreferences(context!!)
        startupMgrIntent = Stuff.getStartupIntent(context!!)

        if (startupMgrIntent != null) {
            view.first_things_0.setOnClickListener {
                openStartupMgr(startupMgrIntent!!, context!!)
                Stuff.toast(activity, getString(R.string.check_nls, getString(R.string.app_name)))
            }
            view.first_things_0.first_things_0_desc.text =
                    getString(R.string.grant_autostart_desc, Build.MANUFACTURER)
            view.first_things_0.visibility = View.VISIBLE
        }
        try {
            context?.packageManager?.getPackageInfo(Stuff.PACKAGE_PIXEL_NP, 0)
            view.first_things_1_desc.text = getString(R.string.grant_notification_access_desc, getString(R.string.except_pixel_np))
        } catch (e: PackageManager.NameNotFoundException) {
            view.first_things_1_desc.text = getString(R.string.grant_notification_access_desc, "")
        }

        view.first_things_1.setOnClickListener {
            val intent = if (Main.isTV && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                Intent().setComponent(ComponentName("com.android.tv.settings","com.android.tv.settings.device.apps.AppsActivity"))
            else
                Intent(Stuff.NLS_SETTINGS)
            if (context!!.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent)
                if (Main.isTV)
                    Stuff.toast(activity, getString(R.string.check_nls_tv, getString(R.string.app_name)))
                else
                    Stuff.toast(activity, getString(R.string.check_nls, getString(R.string.app_name)))
            } else {
                val wf = WebViewFragment()
                val b = Bundle()
                b.putString(Stuff.ARG_URL, getString(R.string.tv_link))
                wf.arguments = b
                parentFragmentManager.beginTransaction()
                        .hide(this)
                        .add(R.id.frame, wf)
                        .addToBackStack(null)
                        .commit()
            }

        }
        view.first_things_2.setOnClickListener {
            val wf = WebViewFragment()
            val b = Bundle()
            b.putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
            b.putBoolean(Stuff.ARG_SAVE_COOKIES, true)
            wf.arguments = b
            parentFragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, wf)
                    .addToBackStack(null)
                    .commit()
//            Stuff.openInBrowser(Stuff.LASTFM_AUTH_CB_URL, activity)
        }
        view.first_things_3.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
        }

        if (arguments?.getBoolean(Stuff.ARG_NOPASS) == true) {
            view.testing_pass.visibility = View.GONE
        } else {
            if (Main.isTV)
                view.testing_pass.isFocusable = false
            view.testing_pass.showSoftInputOnFocus = false
            view.testing_pass.addTextChangedListener(object : TextWatcher {

                override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                }

                override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                }

                override fun afterTextChanged(editable: Editable) {
                    val splits = editable.split('_')
                    if (splits.size == 3) {
                        pref.putString(Stuff.PREF_LASTFM_USERNAME, splits[0])
                        pref.putString(Stuff.PREF_LASTFM_SESS_KEY, splits[1])
                        val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                        imm?.hideSoftInputFromWindow(view!!.windowToken, 0)
                        checkAll(true)
                    } else
                        Stuff.log("bad pass")
                }

            })

            view.testing_pass.setOnTouchListener { v, event ->
                if (v != null) {
                    if (Main.isTV)
                        v.isFocusable = true
                    v.onTouchEvent(event)
                    v.alpha = 0.2f
                }
                true
            }
        }

        return view
    }

    private fun checkAll(skipChecks:Boolean = false){
        val activity = activity ?: return
        stepsNeeded = 4
        if (checkNLAccess(activity)) {
            markAsDone(R.id.first_things_1)
            if(startupMgrIntent != null && KeepNLSAliveJob.ensureServiceRunning(activity))
                // needed for cases when a miui user enables autostart AFTER granting NLS permission
                markAsDone(R.id.first_things_0)
            else
                stepsNeeded --
        }
        if (checkAuthTokenExists(pref))
            markAsDone(R.id.first_things_2)
        if (checkAppListExists(pref))
            markAsDone(R.id.first_things_3)

        if(stepsNeeded == 0 || skipChecks) {
            (activity as Main).showPager()
            if (activity.coordinatorPadding == 0)
                activity.drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    override fun onStart() {
        super.onStart()
        val iF = IntentFilter()
        iF.addAction(NLService.iSESS_CHANGED)
        iF.addAction(NLService.iNLS_STARTED)
        activity!!.registerReceiver(receiver, iF)
        Stuff.setTitle(activity, R.string.first_things)
        (activity as AppCompatActivity?)!!.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        if (arguments?.getBoolean(Stuff.ARG_NOPASS) != true)
        //prevent keyboard from showing up on start
            testing_pass.postDelayed({
                testing_pass?.visibility = View.VISIBLE
            }, 200)

    }

    override fun onResume() {
        super.onResume()
        checkAll()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            checkAll()
            activity?.toolbar?.title = getString(R.string.first_things)
        }
        }

    override fun onDestroyView() {
        activity!!.unregisterReceiver(receiver)
        (activity as AppCompatActivity?)!!.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        super.onDestroyView()
    }

    private fun markAsDone(resId:Int){
        val v= activity!!.findViewById<ViewGroup>(resId)
        v.isEnabled = false
        v.isFocusable = false
        v.alpha = 0.4f
        val tv = v.getChildAt(0) as TextView
        tv.text = "✅ "
        stepsNeeded --
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NLService.iSESS_CHANGED -> checkAll()
                NLService.iNLS_STARTED -> {
                    val i = Intent(context, Main::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(i)
                }
            }
        }
    }

    companion object {
        fun checkNLAccess(c:Context): Boolean {
            val packages = NotificationManagerCompat.getEnabledListenerPackages(c)
            return packages.any { it == c.packageName }
        }

        fun checkAuthTokenExists(pref: MultiPreferences): Boolean {
            return !( pref.getString(Stuff.PREF_LASTFM_SESS_KEY, null)== null ||
                    pref.getString(Stuff.PREF_LASTFM_USERNAME, null)== null)
        }

        fun checkAuthTokenExists(pref: SharedPreferences): Boolean {
            return !( pref.getString(Stuff.PREF_LASTFM_SESS_KEY, null)== null ||
                    pref.getString(Stuff.PREF_LASTFM_USERNAME, null)== null)
        }

        fun checkAppListExists(pref: MultiPreferences): Boolean {
            return !pref.getBoolean(Stuff.PREF_ACTIVITY_FIRST_RUN, true)
        }

        fun openStartupMgr(startupMgrIntent: Intent?, context: Context){
            if (startupMgrIntent == null)
                Stuff.openInBrowser("https://dontkillmyapp.com", context)
            else {
                try {
                    context.startActivity(startupMgrIntent)
                } catch (e: SecurityException) {
                    Stuff.openInBrowser("https://dontkillmyapp.com", context)
                }
            }
        }
    }
}