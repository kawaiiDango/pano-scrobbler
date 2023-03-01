package com.arn.scrobble.edits

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.databinding.DialogRegexEditBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class RegexEditsAddDialogFragment : DialogFragment() {

    private var _binding: DialogRegexEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var usedFields: MutableSet<String>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogRegexEditBinding.inflate(layoutInflater)
        val regexEditArg = arguments?.getSingle<RegexEdit>()?.copy()
        val isNew = regexEditArg == null
        val regexEdit = regexEditArg ?: RegexEdit()
        val dao by lazy { PanoDb.db.getRegexEditsDao() }
        val fieldArgs = arguments?.getStringArray("fields")?.toMutableSet()
        usedFields = fieldArgs ?: mutableSetOf()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (!isNew) {
                    setNeutralButton(R.string.delete) { _, _ ->
                        GlobalScope.launch(Dispatchers.IO) {
                            dao.delete(regexEdit)
                        }
                    }
                }
            }
            .create()

        binding.editName.setText(regexEdit.name)
        binding.editPattern.setText(regexEdit.pattern)
        binding.editReplacement.setText(regexEdit.replacement)

        if (fieldArgs == null)
            regexEdit.fields?.forEach { addField(it) }
        else
            fieldArgs.forEach { addField(it) }

        if (regexEdit.replaceAll)
            binding.editReplaceAll.isChecked = true
        if (regexEdit.caseSensitive)
            binding.editCaseSensitive.isChecked = true
        if (regexEdit.continueMatching)
            binding.editContinueMatching.isChecked = true

        binding.editFieldAdd.setOnClickListener {
            showFieldChooser()
        }

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (validate()) {
                    requireActivity().lifecycleScope.launch(Dispatchers.IO) {
                        val prevRegexEdit = regexEdit.copy()
                        regexEdit.apply {
                            if (isNew)
                                order = (dao.maxOrder ?: -1) + 1
                            if (!binding.editName.text.isNullOrBlank())
                                name = binding.editName.text.toString().trim()
                            pattern = binding.editPattern.text.toString()
                            replacement = binding.editReplacement.text.toString()
                            fields = usedFields
                            replaceAll = binding.editReplaceAll.isChecked
                            caseSensitive = binding.editCaseSensitive.isChecked
                            continueMatching = binding.editContinueMatching.isChecked
                        }

                        if (regexEdit == prevRegexEdit)
                            return@launch

                        if (regexEdit.preset != null)
                            regexEdit.preset = null

                        dao.insert(regexEdit)
                    }

                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    private fun addField(field: String) {
        val wasAdded = usedFields.add(field)

        if (wasAdded) {
            val chip = Chip(binding.editFieldChipgroup.context).apply {
                id = View.generateViewId()
                setText(RegexEditsFragment.localizedFieldsMap[field]!!)
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    usedFields.remove(field)
                    binding.editFieldChipgroup.removeView(it)
                    showOrHideAddButton()
                }

                val chipIconRes = when (field) {
                    NLService.B_TRACK -> R.drawable.vd_note
                    NLService.B_ARTIST -> R.drawable.vd_mic
                    NLService.B_ALBUM -> R.drawable.vd_album
                    NLService.B_ALBUM_ARTIST -> R.drawable.vd_album_artist
                    else -> throw IllegalArgumentException("Unknown field: $field")
                }
                setChipIconResource(chipIconRes)
            }

            binding.editFieldChipgroup.addView(chip, binding.editFieldChipgroup.childCount - 1)
        }

        showOrHideAddButton()
    }

    private fun showFieldChooser() {
        val availableFields = RegexEditsFragment.localizedFieldsMap.keys - usedFields

        if (availableFields.isNotEmpty()) {
            val popup = PopupMenu(requireContext(), binding.editFieldAdd)
            RegexEditsFragment.localizedFieldsMap.forEach { (field, stringRes) ->
                if (field in availableFields) {
                    popup.menu.add(Menu.NONE, stringRes, Menu.NONE, getString(stringRes)).apply {
                        titleCondensed = field
                    }
                }
            }

            popup.setOnMenuItemClickListener {
                addField(it.titleCondensed.toString())
                true
            }

            popup.show()
        }
    }

    private fun showOrHideAddButton() {
        binding.editFieldAdd.visibility =
            if (usedFields.size >= RegexEditsFragment.localizedFieldsMap.size) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun validate(): Boolean {
        if (binding.editPattern.text.isNullOrBlank()) {
            binding.editPattern.error = getString(R.string.required_fields_empty)
            return false
        }

        try {
            Pattern.compile(binding.editPattern.text.toString())
        } catch (e: Exception) {
            binding.editPattern.error = e.message
            return false
        }

        if (usedFields.isEmpty()) {
//            binding.editFieldEdittext.error = getString(R.string.required_fields_empty)
            return false
        }

        binding.editPattern.error = null
        return true
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}