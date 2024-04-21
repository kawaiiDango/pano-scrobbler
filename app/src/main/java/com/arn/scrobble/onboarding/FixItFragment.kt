package com.arn.scrobble.onboarding

import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.arn.scrobble.PersistentNotificationService
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogFixItBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.expandIfNeeded
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class FixItFragment : BottomSheetDialogFragment() {

    private var _binding: DialogFixItBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFixItBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            expandIfNeeded(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var optionsShown = 0

        if (!Stuff.isTv) {
            binding.fixItDkmaLayout.isVisible = true

            binding.fixItDkmaAction.setOnClickListener {
                Stuff.openInBrowser(requireContext(), "https://dontkillmyapp.com")
            }

            optionsShown++
        }

        if (!Stuff.isTv && !App.prefs.notiPersistent &&
            Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.TIRAMISU
        ) {
            binding.fixItPersistentNotiLayout.visibility = View.VISIBLE
            binding.fixItPersistentNotiAction.setOnClickListener { button ->
                App.prefs.notiPersistent = true
                ContextCompat.startForegroundService(
                    requireContext(),
                    Intent(requireContext(), PersistentNotificationService::class.java)
                )
                button.isEnabled = false
            }

            optionsShown++
        }
        val batteryIntent = if (Stuff.isTv) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Intent().setComponent(
                    ComponentName(Stuff.PACKAGE_TV_SETTINGS, Stuff.ACTIVITY_TV_SETTINGS)
                )
            } else
                null
        } else
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        if (batteryIntent != null) {
            val fixItBatteryTitleRes = if (Stuff.isTv)
                R.string.fix_it_energy_title
            else
                R.string.fix_it_battery_title

            binding.fixItBatteryTitle.text = getString(fixItBatteryTitleRes)
            binding.fixItBattery.visibility = View.VISIBLE
            binding.fixItBatteryAction.setOnClickListener {
                requireContext().toast(
                    getString(
                        R.string.check_nls,
                        if (Stuff.isTv)
                            getString(R.string.special_app_access)
                        else
                            getString(R.string.app_name)
                    )
                )
                startActivity(batteryIntent)
            }

            optionsShown++
        }

        if (optionsShown == 0) {
            binding.fixItNoOptions.visibility = View.VISIBLE
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Stuff.getScrobblerExitReasons(App.prefs.lastKillCheckTime, false)
                .firstOrNull()
                ?.let {
                    binding.fixItExitReason.text =
                        getString(R.string.kill_reason, "\n" + it.description)
                }

            App.prefs.lastKillCheckTime = System.currentTimeMillis()
            // todo: this is technically wrong
        }
    }
}