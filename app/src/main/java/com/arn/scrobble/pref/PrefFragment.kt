package com.arn.scrobble.pref

import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.transition.Fade
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.arn.scrobble.*
import com.arn.scrobble.R

/**
 * Created by arn on 09/07/2017.
 */

class PrefFragment : PreferenceFragmentCompat(){

    private val restoreHandler = Handler()
    private lateinit var appPrefs: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = MultiPrefsDataStore(context!!)
        appPrefs = context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)

        reenterTransition = Fade()
        exitTransition = Fade()
        addPreferencesFromResource(R.xml.preferences)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val master = findPreference(Stuff.PREF_MASTER) as SwitchPreference
            master.summary = getString(R.string.pref_master_qs_hint)
        }

        val appList = findPreference(Stuff.PREF_WHITELIST)
        appList.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity!!.supportFragmentManager.beginTransaction()
                    .remove(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }

        val searchSite = findPreference(Stuff.PREF_ACTIVITY_SEARCH_URL) as ListPreference
        val searchSiteVal = appPrefs.getString(Stuff.PREF_ACTIVITY_SEARCH_URL,
                Stuff.PREF_ACTIVITY_SEARCH_URL_DEFAULT)
        val idx = searchSite.findIndexOfValue(searchSiteVal)
        searchSite.summary = searchSite.entries[idx]
        searchSite.value = searchSiteVal

        searchSite.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ pref: Preference, newVal: Any ->
            newVal as String
            val idx = searchSite.findIndexOfValue(newVal)
            searchSite.summary = searchSite.entries[idx]
            appPrefs.edit()
                    .putString(Stuff.PREF_ACTIVITY_SEARCH_URL, newVal)
                    .apply()
            true
        }

        val delaySecs = findPreference(Stuff.PREF_DELAY_SECS) as SeekBarPreference
        delaySecs.min = 20

        val delayPer = findPreference(Stuff.PREF_DELAY_PER) as SeekBarPreference
        delayPer.min = 30

        val shareSig = findPreference(Stuff.PREF_ACTIVITY_SHARE_SIG) as EditTextPreference
        val shareSigVal = appPrefs.getString(Stuff.PREF_ACTIVITY_SHARE_SIG,
                getString(R.string.share_sig, getString(R.string.share_link)))
        shareSig.text = shareSigVal
        shareSig.onPreferenceChangeListener = Preference.OnPreferenceChangeListener{ pref: Preference, newVal ->
            appPrefs.edit()
                    .putString(Stuff.PREF_ACTIVITY_SHARE_SIG, newVal.toString().take(50))
                    .apply()
            true
        }

        initAuthConfirmation("lastfm", {
                LFMRequester.reAuth(context!!)
            },
                Stuff.PREF_LASTFM_USERNAME, Stuff.PREF_LASTFM_SESS_KEY,
                logout = {LastfmUnscrobbler(context!!).clearCookies()}
        )

        initAuthConfirmation("librefm", {
                Stuff.openInBrowser(Stuff.LIBREFM_AUTH_CB_URL, context)
            },
                Stuff.PREF_LIBREFM_USERNAME, Stuff.PREF_LIBREFM_SESS_KEY
        )
/*
        initAuthConfirmation("gnufm_nixtape", {
                val b = Bundle()
                b.putString(LoginFragment.HEADING, "GnuFM")
                b.putString(LoginFragment.TEXTF1, getString(R.string.pref_user_label))
                b.putString(LoginFragment.TEXTF2, "GnuFM nixtape URL")

                val loginFragment = LoginFragment()
                loginFragment.arguments = b
                fragmentManager!!.beginTransaction()
                        .replace(R.id.frame, loginFragment)
                        .addToBackStack(null)
                        .commit()
            },
                Stuff.PREF_GNUFM_USERNAME, Stuff.PREF_GNUFM_SESS_KEY, Stuff.PREF_GNUFM_NIXTAPE
        )
        */

        initAuthConfirmation("listenbrainz", {
                val b = Bundle()
                b.putString(LoginFragment.HEADING, getString(R.string.listenbrainz))
                b.putString(LoginFragment.TEXTF1, getString(R.string.pref_user_label))

                val loginFragment = LoginFragment()
                loginFragment.arguments = b
                activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, loginFragment)
                        .addToBackStack(null)
                        .commit()
            },
                Stuff.PREF_LISTENBRAINZ_USERNAME, Stuff.PREF_LISTENBRAINZ_TOKEN
        )

        initAuthConfirmation("lb_custom", {
                val b = Bundle()
                b.putString(LoginFragment.HEADING, getString(R.string.custom_listenbrainz))
                b.putString(LoginFragment.TEXTF1, getString(R.string.pref_user_label))
                b.putString(LoginFragment.TEXTF2, "API URL")

                val loginFragment = LoginFragment()
                loginFragment.arguments = b
                activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, loginFragment)
                        .addToBackStack(null)
                        .commit()
            },
                Stuff.PREF_LB_CUSTOM_USERNAME, Stuff.PREF_LB_CUSTOM_TOKEN, Stuff.PREF_LB_CUSTOM_ROOT
        )

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

    private fun setAuthLabel(elem: Preference) {
        val username =
                preferenceManager.preferenceDataStore?.getString(elem.key + "_username", null)
        elem.extras.putInt("state", 1)
        if (username != null) {
            elem.summary = getString(R.string.pref_logout) + ": [$username]"
            elem.extras.putInt("state", STATE_LOGOUT)
        } else {
            elem.summary = getString(R.string.pref_login)
            elem.extras.putInt("state", STATE_LOGIN)
        }
    }

    private fun setAuthLabel(elemKey: String) = setAuthLabel(findPreference(elemKey))

    private fun initAuthConfirmation(key:String, login: () -> Unit, vararg keysToClear: String,
                                     logout: (() -> Unit)? = null) {
        val elem = findPreference(key)
        setAuthLabel(elem)
        if (elem.key != "lastfm")
            elem.title = elem.title.toString()+ " " + getString(R.string.pref_scrobble_only)
        elem.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val state = it.extras.getInt("state", STATE_LOGIN)
                when (state){
                    STATE_LOGOUT -> {
                        restoreHandler.postDelayed({
                            setAuthLabel(it)
                        }, CONFIRM_TIME)
                        val span = SpannableString(getString(R.string.pref_confirm_logout))
                        span.setSpan(ForegroundColorSpan(ContextCompat.getColor(context!!, R.color.colorAccent)),
                                0, span.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        span.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, span.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

                        it.summary = span
                        it.extras.putInt("state", STATE_CONFIRM)
                    }
                    STATE_CONFIRM -> {
                        keysToClear.forEach {
                            preferenceManager.preferenceDataStore?.putString(it, null)
                        }
                        logout?.invoke()
                        setAuthLabel(it)
                    }
                    STATE_LOGIN -> login()
                }
                true
            }
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.action_settings)

        listView.isNestedScrollingEnabled = false

        setAuthLabel("listenbrainz")
        setAuthLabel("lb_custom")

        val iF = IntentFilter()
        iF.addAction(NLService.iSESS_CHANGED)
        activity!!.registerReceiver(sessChangeReceiver, iF)
    }

    override fun onStop() {
        activity!!.unregisterReceiver(sessChangeReceiver)
        restoreHandler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    private val sessChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == NLService.iSESS_CHANGED) {
                setAuthLabel("lastfm")
                setAuthLabel("librefm")
            }
        }
    }

}

private const val STATE_LOGIN = 0
private const val STATE_LOGOUT = 2
private const val STATE_CONFIRM = 1
private const val CONFIRM_TIME = 3000L