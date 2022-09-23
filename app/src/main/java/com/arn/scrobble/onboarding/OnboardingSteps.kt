package com.arn.scrobble.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.WebViewFragment
import com.arn.scrobble.databinding.ButtonStepperBinding
import com.arn.scrobble.databinding.ButtonStepperForLoginBinding
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MigratePrefs
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.postRequestFocus
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ernestoyaquello.com.verticalstepperform.Step
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OnboardingSteps(private val fragment: OnboardingFragment) {

    private val context = fragment.requireContext()
    private val prefs = MainPrefs(context)
    private val steps = mutableListOf<OnboardingStep>()

    val stepsList get() = steps as List<OnboardingStep>

    fun addStep(step: OnboardingSteps.() -> OnboardingStep) {
        steps += step()
    }

    abstract inner class OnboardingStep(
        title: String,
        subtitle: String = ""
    ) : Step<Boolean>(title, subtitle) {

        override fun getStepDataAsHumanReadableString() = ""

        override fun onStepOpened(animated: Boolean) {
            continueIfDataValid()
        }

        override fun onStepMarkedAsCompleted(animated: Boolean) {
            fragment.binding.stepperForm.goToNextStep(true)
        }

        override fun isStepDataValid(stepData: Boolean?) =
            IsDataValid(stepData ?: false)

        override fun onStepClosed(animated: Boolean) {
        }

        override fun onStepMarkedAsUncompleted(animated: Boolean) {
        }

        override fun restoreStepData(data: Boolean?) {
        }

        fun continueIfDataValid() {
            if (isStepDataValid(stepData).isValid)
                markAsCompleted(true)
        }
    }

    inner class LoginStep : OnboardingStep(
        context.getString(R.string.pref_login),
    ) {
        override fun getStepData() =
            OnboardingFragment.isLoggedIn(prefs)

        @SuppressLint("ClickableViewAccessibility")
        override fun createStepContentLayout(): View {

            fun openLoginFor(@StringRes serviceId: Int) {
                when (serviceId) {
                    R.string.lastfm -> {
                        val wf = WebViewFragment()
                        wf.arguments = Bundle().apply {
                            putString(Stuff.ARG_URL, Stuff.LASTFM_AUTH_CB_URL)
                            putBoolean(Stuff.ARG_SAVE_COOKIES, true)
                        }
                        fragment.parentFragmentManager.beginTransaction()
                            .replace(R.id.frame, wf)
                            .addToBackStack(null)
                            .commit()
                    }
                    R.string.others -> {
                        MaterialAlertDialogBuilder(fragment.context!!)
                            .setMessage(R.string.authenticate_with_lastfm_desc)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                    else -> {
                        throw IllegalArgumentException("Service ID $serviceId is invalid")
                    }
                }
            }

            val serviceIds = listOf(
                R.string.lastfm,
                R.string.others
            )

            val binding = ButtonStepperForLoginBinding.inflate(LayoutInflater.from(context))

            binding.buttonService.setOnClickListener {
                openLoginFor(R.string.lastfm)
            }
            binding.buttonService.postRequestFocus()

            binding.buttonServiceChooser.setOnClickListener {
                val popup = PopupMenu(context, binding.buttonServiceChooser)
                serviceIds.filter { it != R.string.lastfm }.forEach { strId ->
                    popup.menu.add(0, strId, 0, strId)
                }

                popup.setOnMenuItemClickListener { menuItem ->
                    openLoginFor(menuItem.itemId)
                    true
                }

                popup.show()
            }

            // setup testing password box
            if (fragment.arguments?.getBoolean(Stuff.ARG_NOPASS) == true) {
                binding.testingPass.visibility = View.GONE
            } else {
                if (Stuff.isTv)
                    binding.testingPass.isFocusable = false
                binding.testingPass.showSoftInputOnFocus = false

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
                            prefs.lastfmUsername = splits[0]
                            prefs.lastfmSessKey = splits[1]
                            MigratePrefs.migrateV2(prefs)
                            fragment.hideKeyboard()
                            continueIfDataValid()
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
            }
            if (fragment.arguments?.getBoolean(Stuff.ARG_NOPASS) != true) {
                //prevent keyboard from showing up on start
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    delay(200)
                    binding.testingPass.visibility = View.VISIBLE
                }
            }


            return binding.root
        }

    }

    inner class NotificationListenerStep : OnboardingStep(
        context.getString(R.string.grant_notification_access),
        context.getString(R.string.grant_notification_access_desc),
    ) {
        override fun getStepData() =
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .any { it == context.packageName }

        override fun restoreStepData(data: Boolean?) {
        }

        override fun createStepContentLayout(): View {
            val binding = ButtonStepperBinding.inflate(LayoutInflater.from(context))
            binding.openButton.setOnClickListener {
                val intent = if (Stuff.isTv && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    Intent().setComponent(
                        ComponentName(
                            "com.android.tv.settings",
                            "com.android.tv.settings.device.apps.AppsActivity"
                        )
                    )
                else
                    Intent(Stuff.NLS_SETTINGS)

                if (context!!.packageManager.resolveActivity(
                        intent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    ) != null
                    &&
                    !Stuff.isWindows11 // workaround until MS fixes it
                ) {
                    context.startActivity(intent)
                    if (Stuff.isTv)
                        context!!.toast(
                            context.getString(
                                R.string.check_nls_tv,
                                context.getString(R.string.app_name)
                            )
                        )
                    else
                        context!!.toast(
                            context.getString(
                                R.string.check_nls,
                                context.getString(R.string.app_name)
                            )
                        )
                } else {
                    val wf = WebViewFragment()
                    wf.arguments = Bundle().apply {
                        putString(Stuff.ARG_URL, context.getString(R.string.tv_link))
                    }
                    fragment.parentFragmentManager.beginTransaction()
                        .replace(R.id.frame, wf)
                        .addToBackStack(null)
                        .commit()
                }

            }

            binding.openButton.postRequestFocus()

            binding.skipButton.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.will_not_scrobble)
                    .setIcon(R.drawable.vd_error_circle)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        steps.filter { it !is SendNotificationsStep }
                            .forEach { it.markAsCompleted(false) }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            }

            return binding.root
        }

    }

    inner class DkmaStep : OnboardingStep(
        context.getString(R.string.allow_background),
    ) {
        private var clicked = false

        override fun getStepData() =
            clicked

        override fun restoreStepData(data: Boolean?) {
            clicked = data ?: false
        }

        override fun createStepContentLayout(): View {
            val binding = ButtonStepperBinding.inflate(LayoutInflater.from(context))

            binding.openButton.setOnClickListener {
                Stuff.openInBrowser(
                    "https://dontkillmyapp.com/" + Build.MANUFACTURER.lowercase()
                )
                clicked = true
            }

            binding.openButton.postRequestFocus()

            binding.skipButton.visibility = View.GONE
//            binding.skipButton.setOnClickListener {
//                clicked = true
//                continueIfComplete()
//            }

            return binding.root
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    inner class SendNotificationsStep(private val notificationPermRequest: ActivityResultLauncher<String>) :
        OnboardingStep(
            context.getString(R.string.send_notifications),
            context.getString(R.string.send_notifications_desc),
        ) {
        override fun getStepData() =
            ActivityCompat.checkSelfPermission(
                fragment.activity!!,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        override fun createStepContentLayout(): View {
            val binding = ButtonStepperBinding.inflate(LayoutInflater.from(context))

            binding.openButton.setOnClickListener {
                notificationPermRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                markAsCompleted(true)
            }
            binding.openButton.postRequestFocus()

            binding.skipButton.setOnClickListener {
                markAsCompleted(true)
            }

            return binding.root
        }

    }

    inner class ChooseAppsStep : OnboardingStep(
        context.getString(R.string.choose_apps),
    ) {

        override fun getStepData() = prefs.appListWasRun

        override fun createStepContentLayout(): View {
            val binding = ButtonStepperBinding.inflate(LayoutInflater.from(context))
            binding.openButton.setOnClickListener {
                fragment.parentFragmentManager.beginTransaction()
                    .replace(R.id.frame, AppListFragment())
                    .addToBackStack(null)
                    .commit()
            }
            binding.openButton.postRequestFocus()

            binding.skipButton.visibility = View.GONE
            return binding.root
        }

    }
}