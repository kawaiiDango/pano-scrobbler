package com.arn.scrobble.search

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.arn.scrobble.App
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.DialogIndexingBinding
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IndexingDialogFragment: BottomSheetDialogFragment() {

    private val viewModel by viewModels<IndexingVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private var _binding: DialogIndexingBinding? = null
    private val binding get() = _binding!!
    private val prefs = App.prefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogIndexingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        mainNotifierViewModel.updateCanIndex()
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.indexingProgress.observe(viewLifecycleOwner) {
            it ?: return@observe
            val progressInt = (binding.indexingProgress.max * it).toInt()
            binding.indexingProgress.isIndeterminate = progressInt == 0
            if (progressInt == 0) {
                binding.indexingProgress.show()
            }
            ObjectAnimator.ofInt(binding.indexingProgress, "progress", progressInt).apply {
                duration = 150
                start()
            }

            if (it == 1.0) {
                binding.indexingProgress.hide()
            viewModel.indexingMessage.value = getString(com.google.android.material.R.string.abc_action_mode_done)
            }
        }

        viewModel.indexingError.observe(viewLifecycleOwner) { exception ->
            binding.indexingProgress.hide()
            viewModel.indexingMessage.value = "❗" + exception.message
        }

        viewModel.indexingMessage.observe(viewLifecycleOwner) {
            binding.indexingMsg.text = it
        }

        if (prefs.lastFullIndexTime != null && System.currentTimeMillis() - prefs.lastFullIndexTime!! < Stuff.FULL_INDEX_ALLOWED_INTERVAL)
            binding.indexingOptions.isVisible = false

        binding.indexingOptions.setOnClickListener {
            val popup = PopupMenu(binding.indexingOptions.context, binding.indexingOptions)
            popup.inflate(R.menu.indexing_menu)

            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_full_index -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage("⚠ " + binding.root.context.getString(R.string.full_index_desc))
                            .setIcon(R.drawable.vd_error)
                            .setPositiveButton(R.string.full_index) { _, _ ->
                                binding.indexingOptions.isVisible = false
                                viewModel.fullIndex()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()

                    }
                }
                true
            }
            popup.show()
        }
    }
}