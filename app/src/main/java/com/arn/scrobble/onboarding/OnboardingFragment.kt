package com.arn.scrobble.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.databinding.ButtonStepperForLoginBinding
import com.arn.scrobble.databinding.ContentOnboardingStepperBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.toast
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
                if (isGranted)
                    adapter.checkIfStepsCompleted()
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        _binding = ContentOnboardingStepperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                val intent = if (Stuff.isTv) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        Intent().setComponent(
                            ComponentName(Stuff.PACKAGE_TV_SETTINGS, Stuff.ACTIVITY_TV_SETTINGS)
                        )
                    else
                        null
                } else
                    Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)

                intent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                if (intent != null) {
                    requireContext().startActivity(intent)
                    requireContext().toast(
                        getString(
                            R.string.check_nls,
                            if (intent.action == ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                getString(R.string.app_name)
                            else
                                getString(R.string.special_app_access)
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
                    .setIcon(R.drawable.vd_error)
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
                        requireContext(),
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

        // notifications dont work on tv anyways
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Stuff.isTv) {
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
                if (it != AccountType.LASTFM && !(Stuff.isTv && it == AccountType.FILE)) {
                    popup.menu.add(0, idx, 0, Scrobblables.getString(it))
                }
            }

            popup.setOnMenuItemClickListener { menuItem ->
                LoginFlows(findNavController()).go(serviceEnums[menuItem.itemId])
                true
            }

            popup.show()
        }

        return binding.root

        //prevent keyboard from showing up on start
//        viewLifecycleOwner.lifecycleScope.launch {
//            delay(200)
//            binding.testingPass.visibility = View.VISIBLE
//        }
    }


}