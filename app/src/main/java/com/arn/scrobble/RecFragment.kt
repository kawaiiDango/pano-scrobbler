package com.arn.scrobble

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.databinding.ContentRecBinding
import com.arn.scrobble.scrobbleable.LoginFlows
import com.arn.scrobble.ui.UiUtils.focusOnTv
import com.arn.scrobble.ui.UiUtils.setTextAndAnimate
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class RecFragment : Fragment() {
    private val prefs = App.prefs
    private lateinit var micPermRequest: ActivityResultLauncher<String>
    private val viewModel by viewModels<RecVM>()
    private var _binding: ContentRecBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAxisTransitions(MaterialSharedAxis.Y)

        micPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    if (!viewModel.started)
                        viewModel.start()
                } else
                    viewModel.statusText.value = getString(R.string.grant_rec_perm)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentRecBinding.inflate(inflater, container, false)
        binding.root.setupInsets()
//        if (!Stuff.isTv)
//            setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefs.demoMode)
            binding.recShazam.text = binding.recShazam.text.toString().replace("Shazam", "S app")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Stuff.isTv)
            binding.recShazam.visibility = View.GONE
        else
            binding.recShazam.movementMethod = LinkMovementMethod.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)

            if (!viewModel.started) {
                if (savedInstanceState == null)
                    startListening()
            }

            binding.recProgress.setOnClickListener {
                if (!viewModel.started)
                    startListening()
                else
                    viewModel.stop()
            }
        }

        viewModel.fadeValue.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.recImg.alpha = it
        }

        viewModel.progressValue.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.recProgress.progress = it
        }

        viewModel.statusText.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.recStatus.setTextAndAnimate(it)
        }

        viewModel.rateLimitedEvent.observe(viewLifecycleOwner) {
            showAddApiKey()
        }

        viewModel.scrobbleEvent.observe(viewLifecycleOwner) {
            Snackbar.make(requireView(), R.string.state_scrobbled, Stuff.SCROBBLE_FROM_MIC_DELAY)
                .setAction(android.R.string.cancel) {
                    viewModel.scrobbleJob?.cancel()
                }
                .focusOnTv()
                .show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(requireContext(), RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        )
            micPermRequest.launch(RECORD_AUDIO)
        else if (!viewModel.started)
            viewModel.start()
    }
    /*
        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.rec_menu, menu)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                menu.findItem(R.id.menu_add_to_hs).isVisible = false
            if (prefs.acrcloudHost != null)
                menu.findItem(R.id.menu_add_acr_key).title = getString(R.string.remove_acr_key)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            if (item.itemId == R.id.menu_add_to_hs && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val shortcutManager = requireActivity().getSystemService(ShortcutManager::class.java)
                if (shortcutManager!!.isRequestPinShortcutSupported) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        val pinShortcutInfo = ShortcutInfo.Builder(context, "rec").build()
                        val pinnedShortcutCallbackIntent =
                            shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                        val successCallback = PendingIntent.getBroadcast(
                            context, 0,
                            pinnedShortcutCallbackIntent, Stuff.updateCurrentOrImmutable
                        )

                        shortcutManager.requestPinShortcut(
                            pinShortcutInfo,
                            successCallback.intentSender
                        )
                    }
                }
                return true
            } else if (item.itemId == R.id.menu_add_acr_key) {
                if (item.title == getString(R.string.remove_acr_key)) {
                    item.title = getString(R.string.add_acr_key)
                    removeKey()
                } else
                    openAddKey()
                return true
            }
            return super.onOptionsItemSelected(item)
        }


    private fun removeApiKey() {
        prefs.acrcloudHost = null
        prefs.acrcloudKey = null
        prefs.acrcloudSecret = null
    }
    */
    
    private fun showAddApiKey() {
        Snackbar.make(requireView(), R.string.add_acr_consider, 8 * 1000)
            .setAction(R.string.add) {
                LoginFlows(findNavController()).acrCloud()
            }
            .focusOnTv()
            .show()
    }
}