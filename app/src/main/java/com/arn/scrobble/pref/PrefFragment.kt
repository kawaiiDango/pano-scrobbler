package com.arn.scrobble.pref

import android.app.LocaleManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.main.App
import com.arn.scrobble.onboarding.LoginFlows
import com.arn.scrobble.utils.ForceLogException
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.copyToClipboard
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.isChannelEnabled
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.toast
import com.arn.scrobble.widget.ChartsWidgetActivity
import com.arn.scrobble.widget.ChartsWidgetProvider
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Locale


/**
 * Created by arn on 09/07/2017.
 */

class PrefFragment : PreferenceFragmentCompat() {
    private val prefs = App.prefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)
        return super.onCreateView(inflater, container, savedInstanceState)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Stuff.isTv) {
            findPreference<Preference>("master_qs_add")!!.apply {
                isVisible = true
                setOnPreferenceClickListener {
                    val statusBarManager =
                        ContextCompat.getSystemService(context, StatusBarManager::class.java)
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

        var prevLang = ""
        val localeEntryValues = arrayOf("auto") + LocaleUtils.localesSet
        val localeEntries = localeEntryValues.map {
            if (it == "auto")
                return@map getString(R.string.auto)

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

        val currentLocale = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            prefs.locale ?: "auto"
        } else {
            requireContext()
                .getSystemService(LocaleManager::class.java)
                .applicationLocales
                .takeIf { it.size() == 1 }
                ?.get(0)
                ?.let {
                    if (it.toLanguageTag() in localeEntryValues)
                        it.toLanguageTag()
                    else if (it.language in localeEntryValues)
                        it.language
                    else
                        "auto"
                }
        }

        changeLocalePref.setSummaryProvider { preference ->
            var idx = localeEntryValues.indexOf(currentLocale)
            if (idx == -1)
                idx = 0 // choose "Auto"
            localeEntries[idx]
        }
        changeLocalePref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_change_locale)
                    .setSingleChoiceItems(
                        localeEntries,
                        localeEntryValues.indexOf(currentLocale)
                    ) { dialogInterface, idx ->
                        dialogInterface.dismiss()
                        val newLocale = localeEntryValues[idx]
                        if (currentLocale != newLocale) {
                            prefs.locale = newLocale
                            requireContext().setLocaleCompat(force = true)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }


        val startOfWeekPref = findPreference<Preference>(MainPrefs.PREF_FIRST_DAY_OF_WEEK)!!
        val cal = Calendar.getInstance()
        cal[Calendar.DAY_OF_WEEK] = cal.firstDayOfWeek

        val weekAutoText = getString(R.string.auto) + " - " + cal.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            Locale.getDefault()
        )
        val weeksMap = mutableMapOf(0 to weekAutoText)
        weeksMap.putAll(
            cal.getDisplayNames(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())!!
                .entries
                .associateBy({ it.value }) { it.key }
                .toSortedMap()
        )

        startOfWeekPref.setSummaryProvider {
            weeksMap[prefs.firstDayOfWeek] ?: getString(R.string.auto)
        }

        val checkedWeekIndex = prefs.firstDayOfWeek.coerceIn(0..7)

        startOfWeekPref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_first_day_of_week)
                    .setSingleChoiceItems(
                        weeksMap.values.toTypedArray(),
                        checkedWeekIndex
                    ) { dialogInterface, idx ->
                        dialogInterface.dismiss()
                        if (checkedWeekIndex != idx) {
                            prefs.firstDayOfWeek = idx
                            requireActivity().recreate()
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
        val nm = ContextCompat.getSystemService(requireContext(), NotificationManager::class.java)!!
        if (!nm.isChannelEnabled(prefs.sharedPreferences, MainPrefs.CHANNEL_NOTI_NEW_APP)) {
            autoDetect.apply {
                summary = getString(R.string.notification_channel_blocked)
                isEnabled = false
                isPersistent = false
                isChecked = false
            }
        }

        val noti = findPreference<Preference>("noti")!!

        hideOnTV.add(noti)

        findPreference<Preference>(MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY)!!
            .title = getString(R.string.s_top_scrobbles, getString(R.string.weekly))

        findPreference<Preference>(MainPrefs.CHANNEL_NOTI_DIGEST_MONTHLY)!!
            .title = getString(R.string.s_top_scrobbles, getString(R.string.monthly))

        val chartsWidget = findPreference<Preference>("charts_widget")!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            chartsWidget.setOnPreferenceClickListener {
                val appWidgetManager = AppWidgetManager.getInstance(requireContext())
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
                true
            }
        } else {
            chartsWidget.isVisible = false
        }

        hideOnTV += chartsWidget

        findPreference<Preference>("themes")!!
            .setOnPreferenceClickListener {
                findNavController().navigate(R.id.themesFragment)
                true
            }

        val crashReporter = findPreference<SwitchPreference>(MainPrefs.PREF_CRASHLYTICS_ENABLED)!!

        crashReporter.setOnPreferenceChangeListener { preference, newValue ->
            newValue as Boolean
            CrashReporter.setEnabled(newValue)

            true
        }

        if (ExtrasConsts.isFossBuild) {
            crashReporter.isVisible = false
        }

        val showScrobbleSources = findPreference<SwitchPreference>("show_scrobble_sources")!!

        if (!prefs.proStatus) {
            showScrobbleSources.isPersistent = false
            showScrobbleSources.isChecked = false
            showScrobbleSources.setOnPreferenceClickListener {
                findNavController().navigate(R.id.billingFragment)
                true
            }
        }

        val searchInSource = findPreference<SwitchPreference>("search_in_source")!!

        if (!prefs.proStatus) {
            searchInSource.isPersistent = false
            searchInSource.isChecked = false
            searchInSource.setOnPreferenceClickListener {
                findNavController().navigate(R.id.billingFragment)
                true
            }

        }


        val simpleEdits = findPreference<Preference>("simple_edits")!!
        simpleEdits.setOnPreferenceClickListener {
            findNavController().navigate(R.id.simpleEditsFragment)
            true
        }

        val regexEdits = findPreference<Preference>("regex_edits")!!
        regexEdits.setOnPreferenceClickListener {
            findNavController().navigate(R.id.regexEditsFragment)
            true
        }

        val blockedMetadata = findPreference<Preference>("blocked_metadata")!!
        blockedMetadata.setOnPreferenceClickListener {
            findNavController().navigate(R.id.blockedMetadataFragment)
            true
        }


        val linkHeartButtonToRating =
            findPreference<SwitchPreference>("link_heart_button_to_rating")!!
        if (!prefs.proStatus) {
            linkHeartButtonToRating.isPersistent = false
            linkHeartButtonToRating.isChecked = false
            linkHeartButtonToRating.setOnPreferenceClickListener {
                findNavController().navigate(R.id.billingFragment)
                true
            }
        }

        hideOnTV.add(linkHeartButtonToRating)

//        if (prefs.checkForUpdates == null) {
//            findPreference<SwitchPreference>("check_for_updates")!!
//                .isVisible = false
//        }

        findPreference<Preference>(MainPrefs.PREF_EXPORT)
            ?.setOnPreferenceClickListener {
                findNavController().navigate(R.id.exportFragment)
                true
            }

        findPreference<Preference>(MainPrefs.PREF_IMPORT)
            ?.setOnPreferenceClickListener {
                findNavController().navigate(R.id.importFragment)
                true
            }

        val intentsPref = findPreference<Preference>(MainPrefs.PREF_INTENTS)!!
        intentsPref.setOnPreferenceClickListener {

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

        hideOnTV.add(intentsPref)

        findPreference<Preference>("delete_account")!!
            .setOnPreferenceClickListener {
                findNavController().navigate(R.id.deleteAccountFragment)
                true
            }

        val translatePref = findPreference<Preference>("translate")!!
        translatePref.setOnPreferenceClickListener {
            Stuff.openInBrowser(requireContext(), getString(R.string.crowdin_link))
            true
        }

        hideOnTV.add(translatePref)


        findPreference<Preference>("translate_credits")!!
            .setOnPreferenceClickListener {
                findNavController().navigate(R.id.translatorsFragment)
                true
            }

        findPreference<Preference>("privacy")!!
            .setOnPreferenceClickListener {
                val args = Bundle().apply {
                    putString(Stuff.ARG_URL, getString(R.string.privacy_policy_link))
                }
                findNavController().navigate(R.id.webViewFragment, args)
                true
            }

        val about = findPreference<Preference>("about")!!
        try {
            about.title = "v " + BuildConfig.VERSION_NAME

            about.setOnPreferenceClickListener {
                Stuff.openInBrowser(requireContext(), about.summary.toString())
                true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        if (BuildConfig.DEBUG) {
            findPreference<PreferenceCategory>("debug")!!.isVisible = true

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
                            ContextCompat.getSystemService(
                                requireContext(),
                                NotificationManager::class.java
                            )!!
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

            findPreference<Preference>("copy_sk")!!.setOnPreferenceClickListener {
                Scrobblables.byType(AccountType.LASTFM)?.userAccount?.authKey?.let {
                    requireContext().copyToClipboard(it)
                }
                true
            }
        }

        val libraries = findPreference<Preference>("libraries")!!
        libraries.setOnPreferenceClickListener {
            findNavController().navigate(R.id.licensesFragment)
            true
        }

        val fileScrobblablePref = findPreference<Preference>("file")!!
        hideOnTV.add(fileScrobblablePref)

        val notiCategory = findPreference<Preference>("noti")!!
        hideOnTV.add(notiCategory)

        if (Stuff.isTv)
            hideOnTV.forEach {
                it.isVisible = false
            }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

        listView.clipToPadding = false
        listView.setupInsets(
            additionalSpaceSides = if (UiUtils.isTabletUi) resources.getDimensionPixelSize(R.dimen.overscan_padding_horiz) else 0
        )

        super.onViewCreated(view, savedInstanceState)

        setDivider(null)

        (view.parent as? ViewGroup)?.doOnPreDraw {
            startPostponedEnterTransition()
        }

        collectLatestLifecycleFlow(PanoDb.db.getSimpleEditsDao().count()) {
            findPreference<Preference>("simple_edits")!!.title =
                resources.getQuantityString(
                    R.plurals.num_simple_edits,
                    it,
                    it.format()
                )

        }

        collectLatestLifecycleFlow(PanoDb.db.getRegexEditsDao().count()) {
            findPreference<Preference>("regex_edits")!!.title =
                resources.getQuantityString(
                    R.plurals.num_regex_edits,
                    it,
                    it.format()
                )
        }

        collectLatestLifecycleFlow(PanoDb.db.getBlockedMetadataDao().count()) {
            findPreference<Preference>("blocked_metadata")!!.title =
                resources.getQuantityString(
                    R.plurals.num_blocked_metadata,
                    it,
                    it.format()
                )
        }

        if (arguments?.getBoolean(Stuff.ARG_SCROLL_TO_ACCOUNTS, false) == true) {
            scrollToPreference("accounts")
        }
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

    private fun setAuthLabel(elemKey: String, type: AccountType) {
        setAuthLabel(findPreference<Preference>(elemKey)!!, type)
    }

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
                        val span = SpannableString(getString(R.string.sure_tap_again))
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
                            findNavController().navigate(R.id.action_prefFragment_to_onboardingFragment)
                        } else if (currentAccount != prevAccount) {
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

    override fun onStart() {
        super.onStart()

        setAuthLabel("listenbrainz", AccountType.LISTENBRAINZ)
        setAuthLabel("lb", AccountType.CUSTOM_LISTENBRAINZ)

        initAuthConfirmation("lastfm", AccountType.LASTFM)
        initAuthConfirmation("librefm", AccountType.LIBREFM)
        initAuthConfirmation("gnufm", AccountType.GNUFM)
        initAuthConfirmation("listenbrainz", AccountType.LISTENBRAINZ)
        initAuthConfirmation("lb", AccountType.CUSTOM_LISTENBRAINZ)
        initAuthConfirmation("maloja", AccountType.MALOJA)
        initAuthConfirmation("pleroma", AccountType.PLEROMA)
        initAuthConfirmation("file", AccountType.FILE)
    }

    companion object {
        private const val STATE_LOGIN = 0
        private const val STATE_CONFIRM = 1
        private const val STATE_LOGOUT = 2
        private const val CONFIRM_TIME = 3000L
    }
}