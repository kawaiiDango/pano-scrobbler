package com.arn.scrobble

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.arn.scrobble.databinding.DialogFixItBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
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

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fixItDkmaAction.setOnClickListener {
            Stuff.openInBrowser("https://dontkillmyapp.com")
        }
        val prefs = MainPrefs(requireContext())
        if (!prefs.notiPersistent) {
            binding.fixItPersistentNotiLayout.visibility = View.VISIBLE
            binding.fixItPersistentNotiAction.setOnClickListener { button ->
                prefs.notiPersistent = true
                ContextCompat.startForegroundService(
                    requireContext(),
                    Intent(requireContext(), PersistentNotificationService::class.java)
                )
                button.isEnabled = false
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            if (requireActivity().packageManager.queryIntentActivities(
                    batteryIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                ).isNotEmpty()
            ) {
                binding.fixItBattery.visibility = View.VISIBLE
                binding.fixItBatteryAction.setOnClickListener {
                    startActivity(batteryIntent)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Stuff.getScrobblerExitReasons(prefs.lastKillCheckTime, false)
                .firstOrNull()
                ?.let {
                    binding.fixItExitReason.text =
                        getString(R.string.kill_reason, "\n" + it.description)
                }

            prefs.lastKillCheckTime = System.currentTimeMillis()
            // this is technically wrong
        }
    }
}