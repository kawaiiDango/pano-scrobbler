package com.arn.scrobble.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.App
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.databinding.ButtonStepperForLoginBinding
import com.arn.scrobble.databinding.ContentOnboardingStepperBinding
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.toast
import com.arn.scrobble.utils.Stuff
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis

class OnboardingFragment : Fragment() {
    private var _binding: ContentOnboardingStepperBinding? = null
    val binding
        get() = _binding!!
    private lateinit var notificationPermRequest: ActivityResultLauncher<String>
    private val viewModel by viewModels<OnboardingVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private lateinit var adapter: VerticalStepperAdapter
    private val steps = mutableListOf<OnboardingStepData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    adapter.checkIfStepsCompleted()
            }

        setupAxisTransitions(MaterialSharedAxis.Y, MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentOnboardingStepperBinding.inflate(inflater, container, false)

        adapter = VerticalStepperAdapter(
            viewModel = viewModel,
            loginStepView = ::createLoginStepView,
            onCompleted = {
                mainNotifierViewModel.initializeCurrentUser(Scrobblables.currentScrobblableUser!!)
                val navigated = findNavController().navigateUp()
                if (!navigated)
                    findNavController().navigate(R.id.action_onboardingFragment_to_myHomePagerFragment)
            }
        )
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.onboardingStepperList.setupInsets()

        binding.onboardingStepperList.adapter = adapter
        binding.onboardingStepperList.layoutManager = LinearLayoutManager(requireContext())
        putSteps()

        binding.onboardingPrivacyPolicy.setOnClickListener {
            val args = Bundle().apply {
                putString(Stuff.ARG_URL, getString(R.string.privacy_policy_link))
            }
            findNavController().navigate(R.id.webViewFragment, args)
        }
    }


    override fun onStart() {
        super.onStart()
        adapter.checkIfStepsCompleted()
    }

    private fun putSteps() {
        steps.clear()

        steps += OnboardingStepData(
            type = OnboardingStepType.LOGIN,
            title = getString(R.string.pref_login),
            description = null,
            canSkip = false,
            isCompleted = { Stuff.isLoggedIn() },
            openAction = {}
        )

        val notificationListenerStep = OnboardingStepData(
            type = OnboardingStepType.NOTIFICATION_LISTENER,
            title = getString(R.string.grant_notification_access),
            description = getString(R.string.grant_notification_access_desc),
            canSkip = true,
            isCompleted = { Stuff.isNotificationListenerEnabled() },
            openAction = {
                val intent = if (Stuff.isTv &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
                )
                    Intent().setComponent(
                        ComponentName(
                            "com.android.tv.settings",
                            "com.android.tv.settings.device.apps.AppsActivity"
                        )
                    )
                else
                    Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)

                if (requireContext().packageManager.resolveActivity(
                        intent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    ) != null
                ) {
                    requireContext().startActivity(intent)
                    if (intent.action == ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        requireContext().toast(
                            getString(
                                R.string.check_nls,
                                getString(R.string.app_name)
                            )
                        )
                    else
                        requireContext().toast(
                            getString(
                                R.string.check_nls_tv,
                                getString(R.string.app_name)
                            )
                        )
                } else {
                    val args = Bundle().apply {
                        putString(Stuff.ARG_URL, getString(R.string.tv_link))
                    }
                    findNavController().navigate(R.id.webViewFragment, args)
                }
            },
            skipAction = {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.will_not_scrobble)
                    .setIcon(R.drawable.vd_error_circle)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        adapter.skipToNextStep()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )

        steps += notificationListenerStep

        if (Stuff.isDkmaNeeded() && !notificationListenerStep.isCompleted()) {
            var dkmaClicked = false
            steps += OnboardingStepData(
                type = OnboardingStepType.DKMA,
                title = getString(R.string.allow_background),
                description = null,
                canSkip = false,
                isCompleted = { dkmaClicked },
                openAction = {
                    Stuff.openInBrowser(
                        "https://dontkillmyapp.com/" + Build.MANUFACTURER.lowercase()
                    )
                    dkmaClicked = true
                }
            )
        }

        steps += OnboardingStepData(
            type = OnboardingStepType.CHOOSE_APPS,
            title = getString(R.string.pref_scrobble_from),
            description = getString(R.string.choose_apps),
            canSkip = false,
            isCompleted = { App.prefs.appListWasRun },
            openAction = {
                findNavController().navigate(R.id.appListFragment)
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            steps += OnboardingStepData(
                type = OnboardingStepType.SEND_NOTIFICATIONS,
                title = getString(R.string.send_notifications),
                description = getString(R.string.send_notifications_desc),
                canSkip = true,
                isCompleted = {
                    ActivityCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                },
                openAction = {
                    notificationPermRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }
        adapter.submitList(steps)

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createLoginStepView(): View {
        val serviceEnums = AccountType.entries.toTypedArray()

        val binding =
            ButtonStepperForLoginBinding.inflate(LayoutInflater.from(binding.root.context))

        binding.buttonService.setOnClickListener {
            LoginFlows(findNavController()).go(AccountType.LASTFM)
        }
        binding.buttonService.post { binding.buttonService.requestFocus() }

        binding.buttonServiceChooser.setOnClickListener { v ->
            val popup = PopupMenu(binding.root.context, v)
            serviceEnums.forEachIndexed { idx, it ->
                if (it != AccountType.LASTFM)
                    popup.menu.add(0, idx, 0, Scrobblables.getString(it))
            }

            popup.setOnMenuItemClickListener { menuItem ->
                LoginFlows(findNavController()).go(serviceEnums[menuItem.itemId])
                true
            }

            popup.show()
        }

        binding.buttonServiceChooser.setOnLongClickListener {
            binding.testingPass.isVisible = true
            binding.testingPass.alpha = 1f
            true
        }

        // setup testing password box
        if (Stuff.isTv)
            binding.testingPass.isFocusable = false
        binding.testingPass.showSoftInputOnFocus = false

        if (Stuff.isTestLab)
            binding.testingPass.isVisible = true

        binding.testingPass.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(
                s: CharSequence,
                arg1: Int,
                arg2: Int,
                arg3: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable) {
                val splits = editable.split(',')
                if (splits.size == 3) {
                    val username = splits[0]
                    val authKey = splits[1]

                    Scrobblables.add(
                        UserAccountSerializable(
                            AccountType.LASTFM,
                            UserCached(
                                username,
                                "https://last.fm/user/$username",
                                username,
                                "",
                                -1,
                            ),
                            authKey
                        )
                    )
                    hideKeyboard()
                    adapter.skipToNextStep()
                }
            }

        })

        binding.testingPass.setOnTouchListener { v, event ->
            if (v != null) {
                if (Stuff.isTv)
                    v.isFocusable = true
                v.onTouchEvent(event)
                v.alpha = 0.2f
            }
            true
        }

        return binding.root

        //prevent keyboard from showing up on start
//        viewLifecycleOwner.lifecycleScope.launch {
//            delay(200)
//            binding.testingPass.visibility = View.VISIBLE
//        }
    }


}