package com.arn.scrobble.edits

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ContentRegexEditAddBinding
import com.arn.scrobble.db.ExtractionPatterns
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.countNamedCaptureGroups
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getSingle
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class RegexEditsAddFragment : Fragment() {

    private var _binding: ContentRegexEditAddBinding? = null
    private val binding get() = _binding!!
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private val billingViewModel by activityViewModels<BillingViewModel>()
    private val pm by lazy { requireContext().packageManager }
    private val dao by lazy { PanoDb.db.getRegexEditsDao() }
    private var hasChanged
        get() = requireArguments().getBoolean(ARG_CHANGED, false)
        set(value) = requireArguments().putBoolean(ARG_CHANGED, value)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentRegexEditAddBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) // needs java 8
            binding.editExtract.isVisible = false
        else if (!billingViewModel.proStatus.value) {
            binding.editExtract.setOnClickListener {
                findNavController().navigate(R.id.billingFragment)
            }
        }

        val regexEdit = arguments?.getSingle<RegexEdit>()?.copy() ?: RegexEdit()
        val isNew = regexEdit.order == -1

        binding.editName.setText(regexEdit.name)
        binding.editReplaceInclude.editPattern.setText(regexEdit.pattern)
        binding.editReplaceInclude.editReplacement.setText(regexEdit.replacement)

        binding.editReplaceAll.text =
            getString(R.string.edit_replace) + " " + getString(R.string.edit_all)
        binding.editReplaceFirst.text =
            getString(R.string.edit_replace) + " " + getString(R.string.edit_first)

        binding.editExtractInclude.editExtractTrackLayout.hint =
            getString(R.string.edit_regex) + ": " + getString(R.string.track)
        binding.editExtractInclude.editExtractAlbumLayout.hint =
            getString(R.string.edit_regex) + ": " + getString(R.string.album)
        binding.editExtractInclude.editExtractArtistLayout.hint =
            getString(R.string.edit_regex) + ": " + getString(R.string.artist)
        binding.editExtractInclude.editExtractAlbumArtistLayout.hint =
            getString(R.string.edit_regex) + ": " + getString(R.string.album_artist)

        binding.editExtractInclude.editExtractTrack.setText(regexEdit.extractionPatterns?.extractionTrack)
        binding.editExtractInclude.editExtractAlbum.setText(regexEdit.extractionPatterns?.extractionAlbum)
        binding.editExtractInclude.editExtractArtist.setText(regexEdit.extractionPatterns?.extractionArtist)
        binding.editExtractInclude.editExtractAlbumArtist.setText(regexEdit.extractionPatterns?.extractionAlbumArtist)


        if (regexEdit.extractionPatterns != null)
            binding.editExtractInclude.root.isVisible = true
        else
            binding.editReplaceInclude.root.isVisible = true

        if (regexEdit.replaceAll)
            binding.editReplaceAll.isChecked = true
        if (regexEdit.caseSensitive)
            binding.editCaseSensitive.isChecked = true
        if (regexEdit.continueMatching)
            binding.editContinueMatching.isChecked = true

        if (regexEdit.pattern == null && regexEdit.extractionPatterns != null) {
            binding.editExtract.isChecked = true
        }

        binding.editReplaceChipgroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.size != 1)
                return@setOnCheckedStateChangeListener

            val isExtractMode = checkedIds.first() == R.id.edit_extract


            binding.editExtractInclude.root.isVisible = isExtractMode
            binding.editReplaceInclude.root.isVisible = !isExtractMode
            binding.editPackagesAll.isVisible = !isExtractMode
        }


        regexEdit.fields?.forEach { addField(it) }
        regexEdit.packages?.forEach { addPackage(it) }

        binding.editReplaceInclude.editFieldAdd.setOnClickListener {
            showFieldChooser()
        }

        binding.editPackagesAdd.setOnClickListener {
            val args = bundleOf(
                Stuff.ARG_ALLOWED_PACKAGES to
                        binding.editPackagesChipgroup
                            .children
                            .map { it.getTag(R.id.raw_text) as? String }
                            .filterNotNull()
                            .toList()
                            .toTypedArray()
            )

            saveRegexEditToArgs()

            findNavController().navigate(R.id.appListFragment, args)
        }

        if (!isNew) {
            binding.editDelete.isVisible = true
            binding.editDelete.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        dao.delete(regexEdit)
                    }
                    findNavController().popBackStack()
                }
            }
        }

        val fabData = FabData(
            viewLifecycleOwner,
            com.google.android.material.R.string.abc_action_mode_done,
            R.drawable.vd_check_simple,
            clickListener = {
                if (!validate())
                    return@FabData

                viewLifecycleOwner.lifecycleScope.launch {
                    val newRegexEdit = saveRegexEditToArgs()

                    if (!hasChanged) {
                        findNavController().popBackStack()
                        return@launch
                    }

                    if (isNew)
                        newRegexEdit.order = withContext(Dispatchers.IO) {
                            (dao.maxOrder() ?: -1) + 1
                        }

                    if (newRegexEdit.preset != null)
                        newRegexEdit.preset = null

                    if (newRegexEdit.extractionPatterns != null) {
                        newRegexEdit.pattern = null
                        newRegexEdit.replacement = ""
                        newRegexEdit.fields = null
                    } else {
                        newRegexEdit.extractionPatterns = null
                    }

                    withContext(Dispatchers.IO) {
                        dao.insert(listOf(newRegexEdit))
                    }
                    findNavController().popBackStack()
                }
            }
        )

        mainNotifierViewModel.setFabData(fabData)

        setFragmentResultListener(Stuff.ARG_ALLOWED_PACKAGES) { key, bundle ->
            if (key == Stuff.ARG_ALLOWED_PACKAGES) {
                bundle.getStringArray(key)!!
                    .forEach { addPackage(it) }
            }
        }
    }

    private fun addField(field: String) {
        if (field.isEmpty()) return

        val wasAdded =
            binding.editReplaceInclude.editFieldChipgroup.children.any { it.getTag(R.id.raw_text) == field }

        if (!wasAdded) {
            val chip = Chip(binding.editReplaceInclude.editFieldChipgroup.context).apply {
                id = View.generateViewId()
                setText(RegexEditsFragment.localizedFieldsMap[field]!!)
                setTag(R.id.raw_text, field)
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    binding.editReplaceInclude.editFieldChipgroup.removeView(it)
                    showOrHideAddFieldChip()
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

            binding.editReplaceInclude.editFieldChipgroup.addView(
                chip,
                binding.editReplaceInclude.editFieldChipgroup.childCount - 1
            )
        }

        showOrHideAddFieldChip()
    }

    private fun addPackage(pkgName: String) {
        if (pkgName.isEmpty()) return

        val wasAdded =
            binding.editPackagesChipgroup.children.any { it.getTag(R.id.raw_text) == pkgName }

        if (!wasAdded) {
            val chip = Chip(binding.editPackagesChipgroup.context).apply {
                id = View.generateViewId()
                text = pkgName
                setTag(R.id.raw_text, pkgName)
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    binding.editPackagesChipgroup.removeView(it)
                    showOrHideAllPackagesChip()
                }

                fun onResult(image: Drawable?) {
                    chipIcon = image
                }

                val request = ImageRequest.Builder(context)
                    .data(PackageName(pkgName))
                    .error(R.drawable.vd_apps)
                    .target(
                        onSuccess = ::onResult,
                        onError = ::onResult
                    )
                    .build()
                context.imageLoader.enqueue(request)

                viewLifecycleOwner.lifecycleScope.launch {
                    text = withContext(Dispatchers.IO) {
                        try {
                            pm.getApplicationLabel(pm.getApplicationInfo(pkgName, 0))
                        } catch (e: PackageManager.NameNotFoundException) {
                            pkgName
                        }
                    }
                }
            }

            binding.editPackagesChipgroup.addView(
                chip,
                binding.editPackagesChipgroup.childCount - 2
            )
        }

        showOrHideAllPackagesChip()
    }

    private fun showFieldChooser() {
        val availableFields = RegexEditsFragment.localizedFieldsMap.keys -
                binding.editReplaceInclude.editFieldChipgroup
                    .children
                    .map { it.getTag(R.id.raw_text) }
                    .filterNotNull()
                    .toSet()

        if (availableFields.isNotEmpty()) {
            val popup = PopupMenu(requireContext(), binding.editReplaceInclude.editFieldAdd)
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

    private fun showOrHideAddFieldChip() {
        binding.editReplaceInclude.editFieldAdd.isVisible =
            binding.editReplaceInclude.editFieldChipgroup.childCount - 1 < RegexEditsFragment.localizedFieldsMap.size
    }

    private fun showOrHideAllPackagesChip() {
        // the "all" and "add" button are always present
        binding.editPackagesAll.isVisible = binding.editPackagesChipgroup.childCount <= 2
    }

    private fun validate(): Boolean {
        if (binding.editName.text.isNullOrEmpty()) {
            binding.editName.error = getString(R.string.required_fields_empty)
            return false
        }

        if (binding.editExtract.isChecked) {
            if (!billingViewModel.proStatus.value) {
                findNavController().navigate(R.id.billingFragment)
                return false
            }

            val extractRulesResult = areExtractionRulesValid()

            if (extractRulesResult.isFailure) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(
                        getString(
                            R.string.edit_regex_invalid,
                            extractRulesResult.exceptionOrNull()!!.message
                        )
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return false
            }

            if (binding.editPackagesChipgroup.childCount <= 2) {
                requireContext().toast(R.string.no_apps_enabled)
                return false
            }

            // clear the other screen's fields
            binding.editReplaceInclude.editPattern.text = null
            binding.editReplaceInclude.editReplacement.text = null
        } else {
            if (binding.editReplaceInclude.editPattern.text.isNullOrBlank()) {
                binding.editReplaceInclude.editPattern.error =
                    getString(R.string.required_fields_empty)
                return false
            }

            try {
                Pattern.compile(binding.editReplaceInclude.editPattern.text.toString())
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(
                        getString(R.string.edit_regex_invalid, e.message)
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return false
            }
            // the add button is always present
            if (binding.editReplaceInclude.editFieldChipgroup.childCount <= 1) {
                requireContext().toast(R.string.required_fields_empty)
                return false
            }

            // clear the other screen's fields
            arrayOf(
                binding.editExtractInclude.editExtractTrack,
                binding.editExtractInclude.editExtractAlbum,
                binding.editExtractInclude.editExtractArtist,
                binding.editExtractInclude.editExtractAlbumArtist,
            ).forEach { it.text = null }
        }


        binding.editReplaceInclude.editPattern.error = null
        binding.editName.error = null
        return true
    }

    private fun saveRegexEditToArgs(): RegexEdit {
        val regexEditOld = arguments?.getSingle<RegexEdit>()
        val regexEdit = regexEditOld?.copy() ?: RegexEdit()

        regexEdit.apply {
            if (!binding.editName.text.isNullOrBlank())
                name = binding.editName.text.toString().trim()
            if (binding.editExtract.isChecked) {
                extractionPatterns = ExtractionPatterns(
                    binding.editExtractInclude.editExtractTrack.text.toString(),
                    binding.editExtractInclude.editExtractAlbum.text.toString(),
                    binding.editExtractInclude.editExtractArtist.text.toString(),
                    binding.editExtractInclude.editExtractAlbumArtist.text.toString(),
                )
            } else {
                pattern = binding.editReplaceInclude.editPattern.text.toString()
                replacement =
                    binding.editReplaceInclude.editReplacement.text.toString()
                fields = binding.editReplaceInclude.editFieldChipgroup
                    .children
                    .map { it.getTag(R.id.raw_text) as? String }
                    .filterNotNull()
                    .toSet()
            }

            packages = binding.editPackagesChipgroup
                .children
                .map { it.getTag(R.id.raw_text) as? String }
                .filterNotNull()
                .toSet()
                .ifEmpty { null }
            replaceAll = binding.editReplaceAll.isChecked
            caseSensitive = binding.editCaseSensitive.isChecked
            continueMatching = binding.editContinueMatching.isChecked
        }

        if (regexEditOld != regexEdit && !hasChanged) {
            hasChanged = true
        }

        requireArguments().putSingle(regexEdit)
        return regexEdit
    }

    private fun areExtractionRulesValid(): Result<Unit> {
        val regexEdit = saveRegexEditToArgs()
        val extractionPatterns = regexEdit.extractionPatterns
            ?: return Result.failure(
                IllegalArgumentException(getString(R.string.edit_extract_no_groups))
            )

        arrayOf(
            extractionPatterns.extractionTrack,
            extractionPatterns.extractionAlbum,
            extractionPatterns.extractionArtist,
            extractionPatterns.extractionAlbumArtist,
        ).all {
            try {
                Pattern.compile(it)
                true
            } catch (e: PatternSyntaxException) {
                return Result.failure(
                    IllegalArgumentException(getString(R.string.edit_regex_invalid, e.message))
                )
            }
        }

        val (trackGroups, albumGroups, artistGroups, albumArtistGroups) = regexEdit.countNamedCaptureGroups().values.toList()

        val isSuccess =
            trackGroups == 1 && albumGroups <= 1 && artistGroups == 1 && albumArtistGroups <= 1

        if (!isSuccess) {
            val errMsg = when {
                trackGroups == 0 || artistGroups == 0 -> getString(R.string.edit_extract_no_groups)
                trackGroups > 1 -> getString(R.string.edit_extract_extra_groups, NLService.B_TRACK)
                albumGroups > 1 -> getString(R.string.edit_extract_extra_groups, NLService.B_ALBUM)
                artistGroups > 1 -> getString(
                    R.string.edit_extract_extra_groups,
                    NLService.B_ARTIST
                )

                albumArtistGroups > 1 -> getString(
                    R.string.edit_extract_extra_groups,
                    "albumArtist"
                )

                else -> getString(R.string.edit_regex_invalid, "Unknown error")
            }

            return Result.failure(IllegalArgumentException(errMsg))
        }

        return Result.success(Unit)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveRegexEditToArgs()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_CHANGED = "changed"
    }
}