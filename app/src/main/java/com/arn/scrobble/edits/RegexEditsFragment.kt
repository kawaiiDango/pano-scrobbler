package com.arn.scrobble.edits

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentRegexEditBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.main.App
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.hideKeyboard
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RegexEditsFragment : Fragment(), ItemClickListener<RegexEdit> {
    private var _binding: ContentRegexEditBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<RegexEditsVM>()
    private lateinit var adapter: RegexEditsAdapter
    private val prefs = App.prefs
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        _binding = ContentRegexEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        hideKeyboard()
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

        val fabData = FabData(
            viewLifecycleOwner,
            R.string.add,
            R.drawable.vd_add_borderless,
            {
                showAddDialog(-1)
            }
        )

        mainNotifierViewModel.setFabData(fabData)

        binding.editsList.setupInsets()

        adapter = RegexEditsAdapter(this, viewModel)
        RegexItemTouchHelper(adapter, viewModel, viewLifecycleOwner).apply {
            attachToRecyclerView(binding.editsList)
            adapter.itemTouchHelper = this
        }
        binding.editsList.adapter = adapter
        binding.editsList.layoutManager = LinearLayoutManager(requireContext())

        binding.empty.text = resources.getQuantityString(R.plurals.num_regex_edits, 0, 0)

        binding.regexEditsChipTest.setOnClickListener {
            findNavController().navigate(R.id.regexEditsTestFragment)
        }

        binding.regexEditsChipPresets.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                showPresetsDialog()
            }
        }

        collectLatestLifecycleFlow(viewModel.regexes.filterNotNull()) {
            binding.empty.isVisible = it.isEmpty()
            binding.editsList.isVisible = it.isNotEmpty()

            adapter.submitList(it) {
                (view.parent as? ViewGroup)?.doOnPreDraw {
                    startPostponedEnterTransition()
                }
            }
        }

        collectLatestLifecycleFlow(viewModel.limitReached.filterNotNull()) {
            if (it)
                requireContext().toast(getString(R.string.edit_max_patterns, Stuff.MAX_PATTERNS))
        }

        if (arguments?.getBoolean(Stuff.ARG_SHOW_DIALOG, false) == true)
            viewLifecycleOwner.lifecycleScope.launch {
                showPresetsDialog()
            }
    }

    override fun onItemClick(view: View, position: Int, item: RegexEdit) {
        showAddDialog(position)
    }

    private fun showAddDialog(position: Int) {
        val isNew = position == -1

        if (isNew && viewModel.limitReached.value == true)
            return

        val regexEdit = if (!isNew)
            viewModel.regexes.value?.get(position)
                ?.let { RegexPresets.getPossiblePreset(it.copy()) }
        else
            null

        val args = Bundle().apply {
            putSingle(regexEdit ?: return@apply)
        }

        if (prefs.regexLearnt) {
            findNavController().navigate(R.id.regexEditsAddFragment, args)
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.edit_regex_warning)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    findNavController().navigate(R.id.regexEditsAddFragment, args)
                }
                .setNeutralButton(R.string.learn) { _, _ ->
                    runCatching {
                        startActivity(
                            Intent(Intent.ACTION_WEB_SEARCH)
                                .putExtra(SearchManager.QUERY, "regex tutorial")
                        )
                    }
                }
                .setNegativeButton(R.string.hide) { _, _ ->
                    prefs.regexLearnt = true
                    findNavController().navigate(R.id.regexEditsAddFragment, args)
                }
                .show()
        }
    }

    private suspend fun showPresetsDialog() {
        if (viewModel.limitReached.value == true)
            return

        val dao = PanoDb.db.getRegexEditsDao()

        val presetsAvailable = withContext(Dispatchers.IO) {
            (RegexPresets.presetKeys - dao.allPresets().map {
                it.preset
            }.toSet()).toList()
        }

        if (presetsAvailable.isEmpty()) {
            requireContext().toast(R.string.edit_no_presets_available)
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_presets_available)
            .setNegativeButton(android.R.string.cancel, null)
            .setItems(presetsAvailable
                .map { RegexPresets.getString(it!!) }
                .toTypedArray()
            ) { _, idx ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val regexEdit = RegexEdit(
                        preset = presetsAvailable[idx],
                        order = 0
                    )
                    withContext(Dispatchers.IO) {
                        dao.shiftDown()
                        dao.insert(listOf(regexEdit))
                    }

                    delay(200)
                    binding.editsList.smoothScrollToPosition(0)
                }
            }
            .show()
    }

    companion object {
        val localizedFieldsMap = mapOf(
            NLService.B_TRACK to R.string.track,
            NLService.B_ALBUM to R.string.album,
            NLService.B_ARTIST to R.string.artist,
            NLService.B_ALBUM_ARTIST to R.string.album_artist
        )
    }
}