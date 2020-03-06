package com.arn.scrobble.pref

import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.transition.Fade
import android.webkit.URLUtil
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.arn.scrobble.*
import com.arn.scrobble.pending.db.PendingScrobblesDb


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

        val hideOnTV = mutableListOf<Preference>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !Main.isTV) {
            val master = findPreference<SwitchPreference>(Stuff.PREF_MASTER)!!
            master.summary = getString(R.string.pref_master_qs_hint)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Main.isTV){
            val notif = findPreference<SwitchPreference>(Stuff.PREF_NOTIFICATIONS)!!
            notif.summaryOn = getString(R.string.pref_noti_q)
        }

        val appList = findPreference<Preference>(Stuff.PREF_WHITELIST)!!
        appList.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .add(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }

        val spotifyNotice = findPreference<Preference>("spotify_notice")!!
        spotifyNotice.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Stuff.openInBrowser("https://www.last.fm/settings/applications", activity)
            true
        }

        val pixelNp = findPreference<SwitchPreference>(Stuff.PREF_PIXEL_NP)!!
        hideOnTV.add(pixelNp)
        try {
            context?.packageManager?.getPackageInfo(Stuff.PACKAGE_PIXEL_NP, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            pixelNp.summary = getString(R.string.pref_pixel_np_nope)
            pixelNp.isEnabled = false
            pixelNp.isPersistent = false
            pixelNp.isChecked = false
        }
        val autoDetect = findPreference<SwitchPreference>(Stuff.PREF_AUTO_DETECT)!!
        hideOnTV.add(autoDetect)
/*
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
*/
        val delaySecs = findPreference<SeekBarPreference>(Stuff.PREF_DELAY_SECS)!!
        delaySecs.min = 20

        val delayPer = findPreference<SeekBarPreference>(Stuff.PREF_DELAY_PER)!!
        delayPer.min = 30

        val shareSig = findPreference<Preference>(Stuff.PREF_ACTIVITY_SHARE_SIG)!!

        shareSig.onPreferenceClickListener = Preference.OnPreferenceClickListener {

            val shareSigVal = appPrefs.getString(Stuff.PREF_ACTIVITY_SHARE_SIG,
                getString(R.string.share_sig, getString(R.string.share_link)))
            val et = EditText(context)
            et.setText(shareSigVal)
            val padding = resources.getDimensionPixelSize(R.dimen.fab_margin)

            val dialog = AlertDialog.Builder(context!!, R.style.AppTheme_Transparent)
                    .setTitle(R.string.pref_share_sig)
                    .setPositiveButton(android.R.string.ok) { dialog, id ->
                        appPrefs.edit()
                                .putString(Stuff.PREF_ACTIVITY_SHARE_SIG, et.text.toString().take(50))
                                .apply()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, id ->
                    }
                    .create()
            dialog.setView(et,padding,padding/3,padding,0)
            dialog.show()
            true

        }

        val edits = findPreference<Preference>("edits")!!
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val context = context
            if (context != null) {
                val numEdits = PendingScrobblesDb.getDb(context).getEditsDao().count
                activity!!.runOnUiThread { edits.title = getString(R.string.n_edits, numEdits) }
            }
        }
        edits.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .add(R.id.frame, EditsFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }

        initAuthConfirmation("lastfm", {
            val wf = WebViewFragment()
            val b = Bundle()
            b.putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
            b.putBoolean(Stuff.ARG_SAVE_COOKIES, true)
            wf.arguments = b
            parentFragmentManager.beginTransaction()
                    .remove(this)
                    .add(R.id.frame, wf)
                    .addToBackStack(null)
                    .commit()
//                Stuff.openInBrowser(Stuff.LASTFM_AUTH_CB_URL, context)
            },
                Stuff.PREF_LASTFM_USERNAME, Stuff.PREF_LASTFM_SESS_KEY,
                logout = {LastfmUnscrobbler(context!!).clearCookies()}
        )

        initAuthConfirmation("librefm", {
            val wf = WebViewFragment()
            val b = Bundle()
            b.putString(Stuff.ARG_URL, Stuff.LIBREFM_AUTH_CB_URL)
            wf.arguments = b
            parentFragmentManager.beginTransaction()
                    .remove(this)
                    .add(R.id.frame, wf)
                    .addToBackStack(null)
                    .commit()
//                Stuff.openInBrowser(Stuff.LIBREFM_AUTH_CB_URL, context)
            },
                Stuff.PREF_LIBREFM_USERNAME, Stuff.PREF_LIBREFM_SESS_KEY
        )

        initAuthConfirmation("gnufm", {
            val nixtapeUrl =
                    preferenceManager.preferenceDataStore?.getString(Stuff.PREF_GNUFM_ROOT, "https://")!!
                val et = EditText(context)
                et.setText(nixtapeUrl)
                val padding = resources.getDimensionPixelSize(R.dimen.fab_margin)

                val dialog = AlertDialog.Builder(context!!, R.style.AppTheme_Transparent)
                        .setTitle(R.string.pref_gnufm_title)
                        .setPositiveButton(android.R.string.ok) { dialog, id ->
                            var newUrl = et.text.toString()
                            if (URLUtil.isValidUrl(newUrl)) {
                                if (!newUrl.endsWith('/'))
                                    newUrl += '/'
                                preferenceManager.preferenceDataStore?.putString(Stuff.PREF_GNUFM_ROOT, newUrl)
                                val wf = WebViewFragment()
                                val b = Bundle()
                                b.putString(Stuff.ARG_URL, newUrl+"api/auth?api_key="+Stuff.LIBREFM_KEY+"&cb=pscrobble://auth/gnufm")
                                wf.arguments = b
                                parentFragmentManager.beginTransaction()
                                        .remove(this)
                                        .add(R.id.frame, wf)
                                        .addToBackStack(null)
                                        .commit()
                            }
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, id ->
                        }
                        .create()
                dialog.setView(et,padding,padding/3,padding,0)
                dialog.show()
            },
                Stuff.PREF_GNUFM_USERNAME, Stuff.PREF_GNUFM_SESS_KEY, Stuff.PREF_GNUFM_ROOT
        )


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
                parentFragmentManager.beginTransaction()
                        .replace(R.id.frame, loginFragment)
                        .addToBackStack(null)
                        .commit()
            },
                Stuff.PREF_LB_CUSTOM_USERNAME, Stuff.PREF_LB_CUSTOM_TOKEN, Stuff.PREF_LB_CUSTOM_ROOT
        )

        val about = findPreference<Preference>("about")!!
        try {
            about.title = "v " + BuildConfig.VERSION_NAME
            about.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                Stuff.openInBrowser(about.summary.toString(), activity)
                true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        if (Main.isTV)
            hideOnTV.forEach {
                it.isVisible = false
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

    private fun setAuthLabel(elemKey: String) = setAuthLabel(findPreference<Preference>(elemKey)!!)

    private fun initAuthConfirmation(key:String, login: () -> Unit, vararg keysToClear: String,
                                     logout: (() -> Unit)? = null) {
        val elem = findPreference<Preference>(key)!!
        setAuthLabel(elem)
        if (elem.key == "librefm" || elem.key == "gnufm")
            elem.title = elem.title.toString()+ " " + getString(R.string.pref_scrobble_love_only)
        else if (elem.key == "listenbrainz" || elem.key == "lb_custom")
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
                setAuthLabel("gnufm")
            }
        }
    }

}

private const val STATE_LOGIN = 0
private const val STATE_LOGOUT = 2
private const val STATE_CONFIRM = 1
private const val CONFIRM_TIME = 3000L