package com.arn.scrobble.info

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.databinding.DialogUserTagsBinding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getData
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.hideKeyboard
import com.arn.scrobble.utils.UiUtils.showKeyboard
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.filterNotNull

class UserTagsFragment : DialogFragment(), DialogInterface.OnShowListener {
    private val viewModel by viewModels<UserTagsVM>()

    private val historyAdapter by lazy {
        object : ArrayAdapter<String>(
            requireContext(),
            R.layout.list_item_history,
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val historyTextView = super.getView(position, convertView, parent)
                if (convertView == null) {
                    historyTextView.setOnClickListener {
                        val item = getItem(position)
                        hideKeyboard()
                        binding.userTagsInputEdittext.setText(item, false)
                        binding.userTagsInputEdittext.clearFocus()
                    }
                    historyTextView.setOnLongClickListener {
                        MaterialAlertDialogBuilder(context)
                            .setMessage(R.string.clear_history_specific)
                            .setPositiveButton(R.string.yes) { dialogInterface, i ->
                                val item = getItem(position)
                                viewModel.historyPref.remove(item)
                            }
                            .setNegativeButton(R.string.no, null)
                            .setNeutralButton(R.string.clear_all_history) { dialogInterface, i ->
                                viewModel.historyPref.removeAll()
                            }
                            .show()
                        false
                    }
                }
                return historyTextView
            }

            override fun getItem(position: Int) = viewModel.historyPref.history[position]

            override fun getCount() = viewModel.historyPref.history.size
        }
    }
    private var _binding: DialogUserTagsBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onShow(p0: DialogInterface?) {
        collectLatestLifecycleFlow(viewModel.tags.filterNotNull()) {
            binding.userTagsProgress.hide()
            updateChips(it)
        }

        val addButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)


        binding.userTagsInputEdittext.setOnClickListener { v ->
            if (Stuff.isTv)
                showKeyboard(v)
        }

        binding.userTagsInputEdittext.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
            ) {
                addButton.callOnClick()
                true
            } else
                false
        }
        addButton.setOnClickListener {
            val tags = binding.userTagsInputEdittext.text.toString().trim()
            if (tags.isNotEmpty()) {
                viewModel.splitTags(tags).forEach {
                    viewModel.historyPref.add(it.trim())
                }
                viewModel.addTag(tags)
                binding.userTagsInputEdittext.text.clear()
            } else {
                requireContext().toast(R.string.required_fields_empty)
            }
        }

        binding.userTagsInputEdittext.setAdapter(historyAdapter)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val entry: MusicEntry = requireArguments().getData()!!
        @DrawableRes val icon = when (entry) {
            is Track -> R.drawable.vd_note
            is Artist -> R.drawable.vd_mic
            is Album -> R.drawable.vd_album
        }

        viewModel.setEntry(entry)
        _binding = DialogUserTagsBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(UiUtils.getColoredTitle(requireContext(), entry.name))
            .setIcon(icon)
            .setView(binding.root)
            .setPositiveButton(R.string.add, null)
            .create()
            .apply {
                setOnShowListener(this@UserTagsFragment)
            }
    }

    override fun onDestroyView() {
        _binding = null
        viewModel.historyPref.save()
        super.onDestroyView()
    }

    private fun updateChips(tags: Set<String>) {
        val existingChipTags =
            binding.userTagsChipGroup.children.map { (it as Chip).text.toString() }.toSet()
        val tagsToAdd = tags - existingChipTags

        binding.userTagsChipGroup.children.forEach {
            if ((it as Chip).text.toString() !in tags)
                binding.userTagsChipGroup.removeView(it)
        }

        tagsToAdd.forEach {
            val chip = Chip(requireContext()).apply {
                text = it
                isCloseIconVisible = true
                setOnCloseIconClickListener { _ ->
                    viewModel.deleteTag(it)
                }
            }
            binding.userTagsChipGroup.addView(chip)
        }

        binding.userTagsStatus.isVisible = binding.userTagsChipGroup.childCount == 0
    }
}