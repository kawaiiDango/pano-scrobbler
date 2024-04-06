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
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.databinding.ContentRecBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setTextAndAnimate
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
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
        setupAxisTransitions(MaterialSharedAxis.Y)

        _binding = ContentRecBinding.inflate(inflater, container, false)
        binding.root.setupInsets()
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

        binding.recCancelScrobble.setOnClickListener {
            viewModel.scrobbleJob?.cancel()
            it.isInvisible = true
            viewModel.statusText.value = ""
        }

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

        collectLatestLifecycleFlow(viewModel.fadeValue) {
            binding.recImg.alpha = it
        }

        collectLatestLifecycleFlow(viewModel.progressValue) {
            binding.recProgress.progress = it
        }

        collectLatestLifecycleFlow(viewModel.statusText) {
            binding.recStatus.setTextAndAnimate(it)
        }

        collectLatestLifecycleFlow(viewModel.scrobbleEvent) {
            binding.recCancelScrobble.isInvisible = false
            delay(Stuff.SCROBBLE_FROM_MIC_DELAY)
            binding.recCancelScrobble.isInvisible = true
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
}