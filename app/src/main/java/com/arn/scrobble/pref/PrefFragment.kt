package com.arn.scrobble.pref

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.*
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.transition.Fade
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.*
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogImportBinding
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.ui.MyClickableSpan
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*


/**
 * Created by arn on 09/07/2017.
 */

class PrefFragment : PreferenceFragmentCompat(){

    private val restoreHandler by lazy { Handler(Looper.getMainLooper()) }
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

        val pixelNp = findPreference<SwitchPreference>(Stuff.PREF_PIXEL_NP)!!
        hideOnTV.add(pixelNp)
        try {
            if (Build.VERSION.SDK_INT >= 30)
                context?.packageManager?.getPackageInfo(Stuff.PACKAGE_PIXEL_NP_R, 0)
            else
                context?.packageManager?.getPackageInfo(Stuff.PACKAGE_PIXEL_NP, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            pixelNp.summary = getString(R.string.pref_pixel_np_nope)
            pixelNp.isEnabled = false
            pixelNp.isPersistent = false
            pixelNp.isChecked = false
        }
        val autoDetect = findPreference<SwitchPreference>(Stuff.PREF_AUTO_DETECT)!!
        hideOnTV.add(autoDetect)

        val delaySecs = findPreference<SeekBarPreference>(Stuff.PREF_DELAY_SECS)!!
        delaySecs.min = 20

        val delayPer = findPreference<SeekBarPreference>(Stuff.PREF_DELAY_PER)!!
        delayPer.min = 30

        val edits = findPreference<Preference>("edits")!!
        edits.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                    .remove(this)
                    .add(R.id.frame, EditsFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }

        findPreference<Preference>(Stuff.PREF_EXPORT)
                ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/json"
            val cal = Calendar.getInstance()
            intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.export_file_name,
                    "" + cal[Calendar.YEAR] + "_" + cal[Calendar.MONTH] + "_" + cal[Calendar.DATE]
                    ))
            startActivityForResult(intent, Stuff.REQUEST_CODE_EXPORT)
            true
        }

        findPreference<Preference>(Stuff.PREF_IMPORT)
                ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/*"
            startActivityForResult(intent, Stuff.REQUEST_CODE_IMPORT)
            true
        }
        hideOnTV.add(findPreference("imexport")!!)


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

                val dialog = MaterialAlertDialogBuilder(context!!)
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
                            } else
                                Stuff.toast(activity!!, getString(R.string.failed_encode_url))
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
                b.putString(LoginFragment.TEXTFL, getString(R.string.pref_token_label))

                val loginFragment = LoginFragment()
                loginFragment.arguments = b
                activity!!.supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, loginFragment)
                        .addToBackStack(null)
                        .commit()
            },
                Stuff.PREF_LISTENBRAINZ_USERNAME, Stuff.PREF_LISTENBRAINZ_TOKEN
        )

        initAuthConfirmation("lb", {
                val b = Bundle()
                b.putString(LoginFragment.HEADING, getString(R.string.custom_listenbrainz))
                b.putString(LoginFragment.TEXTF1, getString(R.string.pref_user_label))
                b.putString(LoginFragment.TEXTF2, "API URL")
                b.putString(LoginFragment.TEXTFL, getString(R.string.pref_token_label))

                val loginFragment = LoginFragment()
                loginFragment.arguments = b
                parentFragmentManager.beginTransaction()
                        .replace(R.id.frame, loginFragment)
                        .addToBackStack(null)
                        .commit()
            },
                Stuff.PREF_LB_CUSTOM_USERNAME, Stuff.PREF_LB_CUSTOM_TOKEN, Stuff.PREF_LB_CUSTOM_ROOT
        )

        findPreference<Preference>(Stuff.PREF_INTENTS)
                ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val ss = SpannableString(getString(R.string.pref_intents_dialog_desc))
            val newlinePosList = mutableListOf(-1)
            ss.forEachIndexed { index, c ->
                if (c == '\n')
                    newlinePosList += index
            }
            newlinePosList.forEachIndexed { index, i ->
                if (index in 1..3) {
                    val start = newlinePosList[index - 1] + 1
                    val end = newlinePosList[index]
                    ss.setSpan(MyClickableSpan(start, end), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
            MaterialAlertDialogBuilder(context!!)
                    .setTitle(R.string.pref_intents_dialog_title)
                    .setMessage(ss)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
                    .findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()

            true
        }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)
        rv.layoutAnimation = AnimationUtils.loadLayoutAnimation(context!!, R.anim.layout_animation_slide_up)
        rv.scheduleLayoutAnimation()
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

    private fun setAuthLabel(elemKey: String) = setAuthLabel(findPreference(elemKey)!!)

    private fun initAuthConfirmation(key:String, login: () -> Unit, vararg keysToClear: String,
                                     logout: (() -> Unit)? = null) {
        val elem = findPreference<Preference>(key)!!
        setAuthLabel(elem)
        if (elem.key == "listenbrainz" || elem.key == "lb")
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
                        span.setSpan(ForegroundColorSpan(MaterialColors.getColor(context!!, R.attr.colorPrimary, null)),
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == Stuff.REQUEST_CODE_EXPORT) {
                if (data != null) {
                    val currentUri = data.data ?: return
                    AsyncTask.THREAD_POOL_EXECUTOR.execute {
                        val exporter = ImExporter()
                        exporter.setOutputUri(context!!, currentUri)
                        if (!exporter.export())
                            activity?.runOnUiThread {
                                Stuff.toast(context!!, getString(R.string.export_failed), Toast.LENGTH_LONG)
                            }
                        else
                            Stuff.log("Exported")
                        exporter.close()
                    }
                }
            } else if (requestCode == Stuff.REQUEST_CODE_IMPORT) {
                if (data != null) {
                    val currentUri = data.data ?: return
                    val binding = DialogImportBinding.inflate(layoutInflater)
                    MaterialAlertDialogBuilder(context!!)
                            .setView(binding.root)
                            .setTitle(R.string.import_options)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val editsModeMap = mapOf(
                                        R.id.import_edits_nope to Stuff.EDITS_NOPE,
                                        R.id.import_edits_replace_all to Stuff.EDITS_REPLACE_ALL,
                                        R.id.import_edits_replace_existing to Stuff.EDITS_REPLACE_EXISTING,
                                        R.id.import_edits_keep to Stuff.EDITS_KEEP_EXISTING
                                )
                                val editsMode = editsModeMap[binding.importRadioGroup.checkedRadioButtonId]!!
                                val settingsMode = binding.importSettings.isChecked
                                if (editsMode == Stuff.EDITS_NOPE && !settingsMode)
                                    return@setPositiveButton
                                AsyncTask.THREAD_POOL_EXECUTOR.execute {
                                    val importer = ImExporter()
                                    importer.setInputUri(context!!, currentUri)
                                    if (!importer.import(editsMode, settingsMode))
                                        activity?.runOnUiThread {
                                            Stuff.toast(context!!, getString(R.string.import_hey_wtf), Toast.LENGTH_LONG)
                                        }
                                    else
                                        activity?.runOnUiThread {
                                            Stuff.toast(context!!, getString(R.string.imported))
                                            parentFragmentManager.popBackStack()
                                        }
                                    importer.close()
                                }
                            }
                            .show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.action_settings)

        listView.isNestedScrollingEnabled = false

        setAuthLabel("listenbrainz")
        setAuthLabel("lb")

        val edits = findPreference<Preference>("edits")!!
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            activity?.let {
                val numEdits = PendingScrobblesDb.getDb(it).getEditsDao().count
                it.runOnUiThread {
                    if (context != null)
                        edits.title = getString(R.string.n_edits, numEdits)
                }
            }
        }

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