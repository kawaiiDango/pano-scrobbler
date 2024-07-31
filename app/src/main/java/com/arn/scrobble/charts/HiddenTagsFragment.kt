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
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogUserTagsBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class HiddenTagsFragment : DialogFragment(), DialogInterface.OnShowListener {
    private var _binding: DialogUserTagsBinding? = null
    private val binding
        get() = _binding!!
    private val prefs = App.prefs
    private var changed = false

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
                addTag(tag, save = true)
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

        prefs.hiddenTags.forEach { addTag(it.lowercase(), save = false) }

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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().supportFragmentManager.setFragmentResult(
            Stuff.ARG_HIDDEN_TAGS_CHANGED,
            bundleOf(Stuff.ARG_HIDDEN_TAGS_CHANGED to changed)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun removeTag(tag: String) {
        prefs.hiddenTags = prefs.hiddenTags.toMutableSet().apply { remove(tag) }
        changed = true
    }

    private fun addTag(tag: String, save: Boolean) {
        if (save && tag in prefs.hiddenTags)
            return

        if (save) {
            prefs.hiddenTags = prefs.hiddenTags.toMutableSet().apply { add(tag) }
            changed = true
        }

        val chip = Chip(binding.root.context).apply {
            text = tag
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                removeTag(tag)
                binding.userTagsChipGroup.removeView(it)
            }
        }
        binding.userTagsChipGroup.addView(chip)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(::changed.name, changed)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        changed = savedInstanceState?.getBoolean(::changed.name) ?: false
    }
}
