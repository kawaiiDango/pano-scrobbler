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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.Stuff.hideKeyboard
import com.arn.scrobble.databinding.ContentFirstThingsBinding
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.MainPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat


/**
 * Created by arn on 06/09/2017.
 */
class FirstThingsFragment: Fragment() {
    private var stepsNeeded = 4
    private lateinit var prefs: MainPrefs
    private var startupMgrIntent:Intent? = null
    private var isOnTop = false
    private var _binding: ContentFirstThingsBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentFirstThingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = MainPrefs(context!!)
        startupMgrIntent = Stuff.getStartupIntent(context!!)

        if (startupMgrIntent != null) {
            binding.firstThings0.setOnClickListener {
                openStartupMgr(startupMgrIntent!!, context!!)
                Stuff.toast(activity, getString(R.string.check_nls, getString(R.string.app_name)))
            }
            binding.firstThings0Desc.text =
                    getString(R.string.grant_autostart_desc, Build.MANUFACTURER)
            binding.firstThings0.visibility = View.VISIBLE
        }

        binding.firstThings1.setOnClickListener {
            val intent = if (MainActivity.isTV && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                Intent().setComponent(ComponentName("com.android.tv.settings","com.android.tv.settings.device.apps.AppsActivity"))
            else
                Intent(Stuff.NLS_SETTINGS)
            if (context!!.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent)
                if (MainActivity.isTV)
                    Stuff.toast(activity, getString(R.string.check_nls_tv, getString(R.string.app_name)))
                else
                    Stuff.toast(activity, getString(R.string.check_nls, getString(R.string.app_name)))
            } else {
                val wf = WebViewFragment()
                wf.arguments = Bundle().apply {
                    putString(Stuff.ARG_URL, getString(R.string.tv_link))
                }
                parentFragmentManager.beginTransaction()
                        .hide(this)
                        .add(R.id.frame, wf)
                        .addToBackStack(null)
                        .commit()
            }

        }
        binding.firstThings2.setOnClickListener {
            val wf = WebViewFragment()
            wf.arguments = Bundle().apply {
                putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
                putBoolean(Stuff.ARG_SAVE_COOKIES, true)
            }
            parentFragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, wf)
                    .addToBackStack(null)
                    .commit()
//            Stuff.openInBrowser(Stuff.LASTFM_AUTH_CB_URL, activity)
        }
        binding.firstThings3.setOnClickListener {
            parentFragmentManager.beginTransaction()
                    .hide(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
        }

        if (arguments?.getBoolean(Stuff.ARG_NOPASS) == true) {
            binding.testingPass.visibility = View.GONE
            binding.firstThings2.visibility = View.GONE
        } else {
            if (MainActivity.isTV)
                binding.testingPass.isFocusable = false
            binding.testingPass.showSoftInputOnFocus = false
            binding.testingPass.addTextChangedListener(object : TextWatcher {

                override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                }

                override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
                }

                override fun afterTextChanged(editable: Editable) {
                    val splits = editable.split('_')
                    if (splits.size == 3) {
                        prefs.lastfmUsername = splits[0]
                        prefs.lastfmSessKey = splits[1]
                        hideKeyboard()
                        checkAll(true)
                    } else
                        Stuff.log("bad pass")
                }

            })

            binding.testingPass.setOnTouchListener { v, event ->
                if (v != null) {
                    if (MainActivity.isTV)
                        v.isFocusable = true
                    v.onTouchEvent(event)
                    v.alpha = 0.2f
                }
                true
            }
        }
        if (startupMgrIntent != null)
            putNumbers(binding.firstThings0, binding.firstThings1, binding.firstThings2, binding.firstThings3)
        else
            putNumbers(binding.firstThings1, binding.firstThings2, binding.firstThings3)
    }

    private fun checkAll(skipChecks:Boolean = false){
        val activity = activity ?: return
        stepsNeeded = 4
        if (checkNLAccess(activity)) {
            markAsDone(binding.firstThings1)
            if(startupMgrIntent != null && Stuff.isScrobblerRunning(activity))
                // needed for cases when a miui user enables autostart AFTER granting NLS permission
                markAsDone(binding.firstThings0)
            else
                stepsNeeded--
        }
        if (checkAuthTokenExists(prefs))
            markAsDone(binding.firstThings2)
        if (!prefs.firstRun)
            markAsDone(binding.firstThings3)

        if(stepsNeeded == 0 || skipChecks) {
            (activity as MainActivity).showHomePager()
            if (activity.coordinatorPadding == 0)
                activity.binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onStart() {
        super.onStart()
        val iF = IntentFilter().apply {
            addAction(NLService.iSESS_CHANGED_S)
            addAction(NLService.iNLS_STARTED_S)
        }
        activity!!.registerReceiver(receiver, iF, NLService.BROADCAST_PERMISSION, null)
        Stuff.setTitle(activity, R.string.almost_there)
        (activity as AppCompatActivity?)!!.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        if (arguments?.getBoolean(Stuff.ARG_NOPASS) != true)
            //prevent keyboard from showing up on start
            viewLifecycleOwner.lifecycleScope.launch {
                delay(200)
                binding.testingPass.visibility = View.VISIBLE
            }
    }

    override fun onResume() {
        super.onResume()
        isOnTop = true
        checkAll()
    }

    override fun onPause() {
        super.onPause()
        isOnTop = false
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            checkAll()
            (activity as MainActivity?)?.binding?.coordinatorMain?.toolbar?.title = getString(R.string.almost_there)
        }
    }

    override fun onDestroyView() {
        activity!!.unregisterReceiver(receiver)
        (activity as AppCompatActivity?)!!.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        _binding = null
        super.onDestroyView()
    }

    private fun markAsDone(vg: ViewGroup){
        vg.isEnabled = false
        vg.isFocusable = false
        vg.alpha = 0.4f
        val tv = vg.getChildAt(0) as TextView
        tv.text = "✔ "
        stepsNeeded --
    }

    private fun putNumbers(vararg vgs: ViewGroup){
        vgs.forEachIndexed { i, vg ->
            val tv = vg.getChildAt(0) as TextView
            tv.text = NumberFormat.getInstance().format(i + 1L)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NLService.iSESS_CHANGED_S -> checkAll()
                NLService.iNLS_STARTED_S -> {
                    if (isOnTop)
                        checkAll()
                }
            }
        }
    }

    companion object {
        fun checkNLAccess(c: Context): Boolean {
            val packages = NotificationManagerCompat.getEnabledListenerPackages(c)
            return packages.any { it == c.packageName }
        }

        fun checkAuthTokenExists(prefs: MainPrefs): Boolean {
            return !( prefs.lastfmSessKey == null || prefs.lastfmUsername == null)
        }

        fun openStartupMgr(startupMgrIntent: Intent?, context: Context){
            if (startupMgrIntent == null)
                Stuff.openInBrowser(context, "https://dontkillmyapp.com")
            else {
                try {
                    context.startActivity(startupMgrIntent)
                } catch (e: SecurityException) {
                    Stuff.openInBrowser(context, "https://dontkillmyapp.com")
                }
            }
        }
    }
}