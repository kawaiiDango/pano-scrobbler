package com.arn.scrobble.pref

import android.app.Activity
import android.app.LocaleManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.DividerItemDecoration
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.ForceLogException
import com.arn.scrobble.LocaleUtils
import com.arn.scrobble.LocaleUtils.setLocaleCompat
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.copyToClipboard
import com.arn.scrobble.Stuff.isChannelEnabled
import com.arn.scrobble.databinding.DialogImportBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.LoginFlows
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.toast
import com.arn.scrobble.widget.ChartsWidgetActivity
import com.arn.scrobble.widget.ChartsWidgetProvider
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale


/**
 * Created by arn on 09/07/2017.
 */

class PrefFragment : PreferenceFragmentCompat() {

    private lateinit var exportRequest: ActivityResultLauncher<Intent>
    private lateinit var exportPrivateDataRequest: ActivityResultLauncher<Intent>
    private lateinit var importRequest: ActivityResultLauncher<Intent>
    private val prefs by lazy { MainPrefs(requireContext()) }
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        exportRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null)
                    export(result.data!!)
            }

        exportPrivateDataRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null)
                    export(result.data!!, privateData = true)
            }

        importRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null)
                    import(result.data!!)
            }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val field = preferenceManager::class.java.getDeclaredField("mSharedPreferences")
        field.isAccessible = true
        field[preferenceManager] = prefs.sharedPreferences

        addPreferencesFromResource(R.xml.preferences)

        val hideOnTV = mutableListOf<Preference>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            !Stuff.isTv
        ) {
            val master = findPreference<SwitchPreference>(MainPrefs.PREF_MASTER)!!
            master.summary = getString(R.string.pref_master_qs_hint)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            findPreference<Preference>("master_qs_add")!!.apply {
                isVisible = true
                setOnPreferenceClickListener {
                    val statusBarManager = getSystemService(context, StatusBarManager::class.java)
                        ?: return@setOnPreferenceClickListener false
                    statusBarManager.requestAddTileService(
                        ComponentName(context, MasterSwitchQS::class.java),
                        getString(
                            if (prefs.scrobblerEnabled)
                                R.string.scrobbler_on
                            else
                                R.string.scrobbler_off
                        ),
                        Icon.createWithResource(context, R.drawable.vd_noti),
                        context.mainExecutor
                    ) { result ->
                        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED)
                            context.toast(R.string.pref_master_qs_already_addded)
                    }
                    true
                }
            }
        }

        val notiCategories = findPreference<Preference>("noti_categories")!!

        hideOnTV += notiCategories

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Stuff.isTv) {
            notiCategories.summary = getString(R.string.pref_noti_q)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Stuff.isWindows11) {
            arrayOf(
                MainPrefs.CHANNEL_NOTI_SCROBBLING,
                MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY,
                MainPrefs.CHANNEL_NOTI_DIGEST_MONTHLY
            ).forEach {
                findPreference<Preference>(it)?.isVisible = false
            }
            notiCategories.setOnPreferenceClickListener {
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                }
                startActivity(intent)
                true
            }
        } else {
            notiCategories.isVisible = false
        }

        val nlsEnabled = Stuff.isNotificationListenerEnabled()
        if (!nlsEnabled) {
            findPreference<Preference>(MainPrefs.PREF_MASTER)!!
                .isEnabled = false
        }

        val changeLocalePref = findPreference<Preference>(MainPrefs.PREF_LOCALE)!!

        val _entryValues = LocaleUtils.localesSet.toTypedArray()
        var prevLang = ""
        val _entries = _entryValues.map {
            val locale = Locale.forLanguageTag(it)

            val displayStr = when {
                locale.language in LocaleUtils.showScriptSet ->
                    locale.displayLanguage + " (${locale.displayScript})"

                prevLang == locale.language ->
                    locale.displayLanguage + " (${locale.displayCountry})"

                else ->
                    locale.displayLanguage
            }
            prevLang = locale.language
            displayStr
        }.toTypedArray()

        val entries = arrayOf(getString(R.string.auto)) + _entries
        val entryValues = arrayOf("auto") + _entryValues
        var currentLocale = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            prefs.locale
        else {
            val ll = requireContext().getSystemService(LocaleManager::class.java).applicationLocales
            if (ll.size() == 0)
                null
            else
                ll.get(0).toLanguageTag()
        }

        currentLocale = currentLocale ?: "auto"

        val checkedIndex = entryValues.indexOf(currentLocale)

        changeLocalePref.setSummaryProvider { preference ->
            var idx = entryValues.indexOf(currentLocale)
            if (idx == -1)
                idx = 0 // choose "Auto"
            entries[idx]
        }
        changeLocalePref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_change_locale)
                    .setSingleChoiceItems(entries, checkedIndex) { dialogInterface, idx ->
                        dialogInterface.dismiss()
                        val newLocale = entryValues[idx]
                        if (currentLocale != newLocale) {
                            prefs.locale = newLocale
                            requireContext().setLocaleCompat(force = true)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }

        val appList = findPreference<Preference>(MainPrefs.PREF_ALLOWED_PACKAGES)!!
        appList.setOnPreferenceClickListener {
            findNavController().navigate(R.id.appListFragment)
            true
        }

        val autoDetect = findPreference<SwitchPreference>(MainPrefs.PREF_AUTO_DETECT)!!
        hideOnTV.add(autoDetect)
        val nm = getSystemService(requireContext(), NotificationManager::class.java)!!
        if (!nm.isChannelEnabled(prefs.sharedPreferences, MainPrefs.CHANNEL_NOTI_NEW_APP)) {
            autoDetect.apply {
                summary = getString(R.string.notification_channel_blocked)
                isEnabled = false
                isPersistent = false
                isChecked = false
            }
        }

        findPreference<Preference>(MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY)!!
            .title = getString(R.string.s_top_scrobbles, getString(R.string.weekly))

        findPreference<Preference>(MainPrefs.CHANNEL_NOTI_DIGEST_MONTHLY)!!
            .title = getString(R.string.s_top_scrobbles, getString(R.string.monthly))

        findPreference<Preference>("charts_widget")!!
            .setOnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val appWidgetManager =
                        getSystemService(
                            requireContext(),
                            AppWidgetManager::class.java
                        ) as AppWidgetManager
                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                        val pi = PendingIntent.getActivity(
                            context,
                            30,
                            Intent(context, ChartsWidgetActivity::class.java)
                                .apply { putExtra(Stuff.EXTRA_PINNED, true) },
                            Stuff.updateCurrentOrMutable
                        )

                        val myProvider =
                            ComponentName(requireContext(), ChartsWidgetProvider::class.java)
                        appWidgetManager.requestPinAppWidget(myProvider, null, pi)
                    }
                }
                true
            }

        findPreference<Preference>("themes")!!
            .setOnPreferenceClickListener {
                findNavController().navigate(R.id.themesFragment)
                true
            }

        findPreference<SwitchPreference>(MainPrefs.PREF_CRASHLYTICS_ENABLED)!!
            .setOnPreferenceChangeListener { preference, newValue ->
                kotlin.runCatching {
                    FirebaseCrashlytics.getInstance()
                        .setCrashlyticsCollectionEnabled(newValue as Boolean)
                }
                true
            }

        val showScrobbleSources = findPreference<SwitchPreference>("show_scrobble_sources")!!
        val fakeShowScrobbleSources =
            findPreference<SwitchPreference>("fake_show_scrobble_sources")!!
        val searchInSource = findPreference<SwitchPreference>("search_in_source")!!

        fakeShowScrobbleSources.isVisible = !prefs.proStatus
        showScrobbleSources.isVisible = prefs.proStatus
        if (!prefs.proStatus) {
            searchInSource.isPersistent = false
            searchInSource.isChecked = false
            searchInSource.setOnPreferenceClickListener {
                findNavController().navigate(R.id.billingFragment)
                true
            }

        }

        if (prefs.checkForUpdates == null) {
            findPreference<SwitchPreference>("check_for_updates")!!
                .isVisible = false
        }

        findPreference<Preference>(MainPrefs.PREF_EXPORT)
            ?.setOnPreferenceClickListener {
                val cal = Calendar.getInstance()

                if (prefs.proStatus && prefs.showScrobbleSources) {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(
                            Intent.EXTRA_TITLE, getString(
                                R.string.export_file_name,
                                "private_" + cal[Calendar.YEAR] + "_" + (cal[Calendar.MONTH] + 1) + "_" + cal[Calendar.DATE]
                            )
                        )
                    }

                    exportPrivateDataRequest.launch(intent)
                }

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(
                        Intent.EXTRA_TITLE, getString(
                            R.string.export_file_name,
                            "" + cal[Calendar.YEAR] + "_" + (cal[Calendar.MONTH] + 1) + "_" + cal[Calendar.DATE]
                        )
                    )
                }

                exportRequest.launch(intent)
                true
            }

        findPreference<Preference>(MainPrefs.PREF_IMPORT)
            ?.setOnPreferenceClickListener {
                // On Android 11 TV:
                // Permission Denial: opening provider com.android.externalstorage.ExternalStorageProvider
                // from ProcessRecord{a608cee 5039:com.google.android.documentsui/u0a21}
                // (pid=5039, uid=10021) requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/*"
                }
                importRequest.launch(intent)
                true
            }
        hideOnTV.add(findPreference("imexport")!!)


        initAuthConfirmation("lastfm", AccountType.LASTFM)

        initAuthConfirmation("librefm", AccountType.LIBREFM)

        initAuthConfirmation("gnufm", AccountType.GNUFM)

        initAuthConfirmation("listenbrainz", AccountType.LISTENBRAINZ)

        initAuthConfirmation("lb", AccountType.CUSTOM_LISTENBRAINZ)

        findPreference<Preference>(MainPrefs.PREF_INTENTS)!!
            .setOnPreferenceClickListener {

                val itemsRaw = listOf(
                    NLService.iSCROBBLER_ON,
                    NLService.iSCROBBLER_OFF,
                    null,
                    NLService.iLOVE,
                    NLService.iUNLOVE,
                    NLService.iCANCEL,
                )

                val itemsDisplay = itemsRaw.map {
                    if (it == null)
                        getString(R.string.current_track)
                    else
                        "\t\t$it"
                }.toTypedArray()

                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_intents_dialog_title)
                    .setItems(itemsDisplay, null)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()

                dialog.listView.setOnItemClickListener { _, _, selectedIdx, _ ->
                    val item = itemsRaw[selectedIdx]
                    requireContext().copyToClipboard(item ?: return@setOnItemClickListener)
                }

                true
            }

        findPreference<Preference>("delete_account")!!
            .setOnPreferenceClickListener {
                findNavController().navigate(R.id.deleteAccountFragment)
                true
            }

        findPreference<Preference>("translate")!!
            .setOnPreferenceClickListener {
                Stuff.openInBrowser(getString(R.string.crowdin_link))
                true
            }

        findPreference<Preference>("translate_credits")!!
            .setOnPreferenceClickListener {
                Stuff.openInBrowser(getString(R.string.crowdin_link) + "/members")
                true
            }

        findPreference<Preference>("privacy")!!
            .setOnPreferenceClickListener {
                Stuff.openInBrowser(getString(R.string.privacy_policy_link))
                true
            }

        val about = findPreference<Preference>("about")!!
        try {
            about.title = "v " + BuildConfig.VERSION_NAME
            about.setOnPreferenceClickListener {
                Stuff.openInBrowser(about.summary.toString())
                true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        if (BuildConfig.DEBUG) {
            findPreference<PreferenceCategory>("debug")!!.isVisible = true
        }

        findPreference<EditTextPreference>("force_exception")!!.apply {
            setOnBindEditTextListener { editText ->
                editText.setText("Unspecified")
            }
            setOnPreferenceChangeListener { preference, newValue ->
                Timber.tag(Stuff.TAG).e(ForceLogException(newValue.toString()))
                preference as EditTextPreference
                preference.text = ""
                true
            }
        }

        findPreference<EditTextPreference>(MainPrefs.CHANNEL_TEST_SCROBBLE_FROM_NOTI)!!.apply {
            setOnBindEditTextListener { editText ->
                editText.setText("Back Door by nachi")
            }
            setOnPreferenceChangeListener { preference, newValue ->
                newValue as String
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channel = NotificationChannel(
                        MainPrefs.CHANNEL_TEST_SCROBBLE_FROM_NOTI,
                        getString(R.string.test_scrobble_from_noti),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)

                    if (newValue.isNotEmpty()) {
                        val notification = NotificationCompat.Builder(
                            context,
                            MainPrefs.CHANNEL_TEST_SCROBBLE_FROM_NOTI
                        )
                            .setContentTitle(newValue)
                            .setSmallIcon(R.drawable.vd_noti_persistent)
                            .build()
                        notificationManager.notify(55, notification)
                    } else {
                        notificationManager.cancel(55)
                    }
                }
                true
            }
        }

        val libraries = findPreference<Preference>("libraries")!!
        libraries.setOnPreferenceClickListener {
            findNavController().navigate(R.id.licensesFragment)
            true
        }

        if (Stuff.isTv)
            hideOnTV.forEach {
                it.isVisible = false
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listView.clipToPadding = false
        listView.setupInsets()

        listView.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_slide_up)
        listView.scheduleLayoutAnimation()

        super.onViewCreated(view, savedInstanceState)

        val decoration =
            MaterialDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        decoration.setDividerInsetStartResource(requireContext(), R.dimen.divider_inset)
        decoration.setDividerInsetEndResource(requireContext(), R.dimen.divider_inset)
        val colorDrawable = ColorDrawable(decoration.dividerColor)
        val insetDrawable = InsetDrawable(
            colorDrawable,
            decoration.dividerInsetStart,
            0,
            decoration.dividerInsetEnd,
            0
        )
        setDivider(insetDrawable)
    }

    private fun setAuthLabel(elem: Preference, type: AccountType) {

        val username = Scrobblables.byType(type)?.userAccount?.user?.name
        elem.extras.putInt("state", STATE_CONFIRM)
        if (username != null) {
            elem.summary = getString(R.string.pref_logout) + ": [$username]"
            elem.extras.putInt("state", STATE_LOGOUT)
        } else {
            elem.summary = getString(R.string.pref_login)
            elem.extras.putInt("state", STATE_LOGIN)
        }
    }

    private fun setAuthLabel(elemKey: String, type: AccountType) =
        setAuthLabel(findPreference(elemKey)!!, type)

    private fun initAuthConfirmation(
        key: String,
        type: AccountType
    ) {
        val elem = findPreference<Preference>(key)!!
        setAuthLabel(elem, type)
        elem.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val state = it.extras.getInt("state", STATE_LOGIN)
                when (state) {
                    STATE_LOGOUT -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(CONFIRM_TIME)
                            setAuthLabel(it, type)
                        }
                        val span = SpannableString(getString(R.string.pref_confirm_logout))
                        span.setSpan(
                            ForegroundColorSpan(
                                MaterialColors.getColor(
                                    requireContext(),
                                    com.google.android.material.R.attr.colorPrimary,
                                    null
                                )
                            ),
                            0, span.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                        )
                        span.setSpan(
                            StyleSpan(Typeface.BOLD_ITALIC),
                            0,
                            span.length,
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                        )

                        it.summary = span
                        it.extras.putInt("state", STATE_CONFIRM)
                    }

                    STATE_CONFIRM -> {
                        val prevAccount = Scrobblables.current?.userAccount
                        Scrobblables.deleteAllByType(type)
                        Scrobblables.updateScrobblables()

                        val currentAccount = Scrobblables.current?.userAccount

                        setAuthLabel(it, type)

                        if (currentAccount == null) {
                            findNavController().apply {
                                popBackStack(R.id.myHomePagerFragment, true)
                                navigate(R.id.onboardingFragment)
                            }
                        } else if(currentAccount != prevAccount) {
                            findNavController().apply {
                                popBackStack(R.id.myHomePagerFragment, true)
                                navigate(R.id.myHomePagerFragment)
                            }
                        }
                    }

                    STATE_LOGIN -> LoginFlows(findNavController()).go(type)
                }
                true
            }
    }

    private fun export(data: Intent, privateData: Boolean = false) {
        val currentUri = data.data ?: return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val exported = ImExporter().use {
                it.setOutputUri(currentUri)
                if (privateData)
                    it.exportPrivateData()
                else
                    it.export()
            }
            if (!exported)
                withContext(Dispatchers.Main) {
                    requireContext().toast(R.string.export_failed, Toast.LENGTH_LONG)
                }
            else
                Stuff.log("Exported")
        }
    }

    private fun import(data: Intent) {
        val currentUri = data.data ?: return
        val binding = DialogImportBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
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
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val imported = ImExporter().use {
                        it.setInputUri(currentUri)
                        it.import(editsMode, settingsMode)
                    }
                    withContext(Dispatchers.Main) {
                        if (!imported)
                            requireContext().toast(R.string.import_hey_wtf, Toast.LENGTH_LONG)
                        else {
                            requireContext().toast(R.string.imported)
                            parentFragmentManager
                                .beginTransaction()
                                .detach(this@PrefFragment)
                                .commit()
                            parentFragmentManager
                                .beginTransaction()
                                .attach(this@PrefFragment)
                                .commit()
                        }
                    }
                }
            }
            .show()
    }

    override fun onStart() {
        super.onStart()

        setAuthLabel("listenbrainz", AccountType.LISTENBRAINZ)
        setAuthLabel("lb", AccountType.CUSTOM_LISTENBRAINZ)

        val simpleEdits = findPreference<Preference>("simple_edits")!!
        simpleEdits.setOnPreferenceClickListener {
            findNavController().navigate(R.id.simpleEditsFragment)
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val numEdits = withContext(Dispatchers.IO) {
                PanoDb.db.getSimpleEditsDao().count
            }
            withContext(Dispatchers.Main) {
                simpleEdits.title =
                    resources.getQuantityString(
                        R.plurals.num_simple_edits,
                        numEdits,
                        NumberFormat.getInstance().format(numEdits)
                    )
            }
        }

        val regexEdits = findPreference<Preference>("regex_edits")!!
        regexEdits.setOnPreferenceClickListener {
            findNavController().navigate(R.id.regexEditsFragment)
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val numEdits = withContext(Dispatchers.IO) {
                PanoDb.db.getRegexEditsDao().count
            }
            withContext(Dispatchers.Main) {
                regexEdits.title =
                    resources.getQuantityString(
                        R.plurals.num_regex_edits,
                        numEdits,
                        NumberFormat.getInstance().format(numEdits)
                    )
            }
        }

        val blockedMetadata = findPreference<Preference>("blocked_metadata")!!
        blockedMetadata.setOnPreferenceClickListener {
            findNavController().navigate(R.id.blockedMetadataFragment)
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val numEdits = withContext(Dispatchers.IO) {
                PanoDb.db.getBlockedMetadataDao().count
            }
            withContext(Dispatchers.Main) {
                blockedMetadata.title =
                    resources.getQuantityString(
                        R.plurals.num_blocked_metadata,
                        numEdits,
                        NumberFormat.getInstance().format(numEdits)
                    )
            }
        }

        findPreference<SwitchPreference>("show_scrobble_sources")!!.isVisible = prefs.proStatus
        findPreference<SwitchPreference>("fake_show_scrobble_sources")!!.isVisible =
            !prefs.proStatus

        if (!prefs.proStatus) {
            val fakeShowScrobbleSources =
                findPreference<SwitchPreference>("fake_show_scrobble_sources")!!
            fakeShowScrobbleSources.isChecked = false
            fakeShowScrobbleSources.setOnPreferenceClickListener {
                findNavController().navigate(R.id.billingFragment)
                true
            }
        }
    }

    companion object {
        private const val STATE_LOGIN = 0
        private const val STATE_CONFIRM = 1
        private const val STATE_LOGOUT = 2
        private const val CONFIRM_TIME = 3000L
    }
}