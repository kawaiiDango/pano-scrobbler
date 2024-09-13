package com.arn.scrobble.charts

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogUserTagsBinding
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class HiddenTagsFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var _binding: DialogUserTagsBinding? = null
    private val binding
        get() = _binding!!
    private val mainPrefs = PlatformStuff.mainPrefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onShow(p0: DialogInterface?) {
        val addButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)

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
            val tag = binding.userTagsInputEdittext.text.toString().trim().lowercase()
            if (tag.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    addTag(tag, save = true)
                }
                binding.userTagsInputEdittext.text.clear()
            } else {
                requireContext().toast(R.string.required_fields_empty)
            }
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUserTagsBinding.inflate(layoutInflater)
        binding.userTagsStatus.visibility = View.GONE
        binding.userTagsProgress.visibility = View.GONE
        binding.userTagsInput.endIconMode = TextInputLayout.END_ICON_NONE
        binding.userTagsInput.isEndIconVisible = false

        runBlocking {
            mainPrefs.data.map { it.hiddenTags }.first()
                .forEach { addTag(it.lowercase(), save = false) }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(UiUtils.getColoredTitle(requireContext(), getString(R.string.hidden_tags)))
            .setIcon(R.drawable.vd_tag)
            .setView(binding.root)
            .setPositiveButton(R.string.add, null)
            .create()
            .apply {
                setOnShowListener(this@HiddenTagsFragment)
            }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private suspend fun removeTag(tag: String) {
        mainPrefs.updateData {
            it.copy(hiddenTags = it.hiddenTags - tag)
        }
    }

    private suspend fun addTag(tag: String, save: Boolean) {
        if (save && tag in mainPrefs.data.map { it.hiddenTags }.first())
            return

        if (save) {
            mainPrefs.updateData {
                it.copy(hiddenTags = it.hiddenTags + tag)
            }
        }

        val chip = Chip(binding.root.context).apply {
            text = tag
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    removeTag(tag)
                }
                binding.userTagsChipGroup.removeView(it)
            }
        }
        binding.userTagsChipGroup.addView(chip)
    }
}
