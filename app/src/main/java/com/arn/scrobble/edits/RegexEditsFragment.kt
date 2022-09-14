package com.arn.scrobble.edits

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.databinding.ContentRegexEditBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.UiUtils.autoNotify
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegexEditsFragment : Fragment(), ItemClickListener {
    private var _binding: ContentRegexEditBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<RegexEditsVM>()
    private lateinit var adapter: RegexEditsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentRegexEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        hideKeyboard()
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        setTitle(R.string.pref_regex_edits)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editRegexWarning.movementMethod = LinkMovementMethod.getInstance()

        binding.editAdd.setOnClickListener {
            showAddDialog(-1)
        }

        adapter = RegexEditsAdapter(viewModel, this)
        RegexItemTouchHelper(adapter, viewModel).apply {
            attachToRecyclerView(binding.editsList)
            adapter.itemTouchHelper = this
        }
        binding.editsList.adapter = adapter
        binding.editsList.layoutManager = LinearLayoutManager(context!!)

        binding.empty.text = resources.getQuantityString(R.plurals.num_regex_edits, 0, 0)

        binding.editBottomAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_test -> {
                    RegexEditsTestDialogFragment()
                        .show(childFragmentManager, null)
                    true
                }
                R.id.menu_presets -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        showPresetsDialog()
                    }
                    true
                }
                else -> false
            }
        }

        viewModel.regexesReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe

            binding.empty.visibility = if (it.isEmpty())
                View.VISIBLE
            else
                View.INVISIBLE

            binding.editsList.visibility = if (it.isNotEmpty())
                View.VISIBLE
            else
                View.INVISIBLE

            val oldList = viewModel.regexes.toList()
            viewModel.regexes.clear()
            viewModel.regexes.addAll(it)
            adapter.autoNotify(oldList, viewModel.regexes) { o, n -> o._id == n._id }
        }

        viewModel.countReceiver.observe(viewLifecycleOwner) {
        }

        if (arguments?.getBoolean(Stuff.ARG_SHOW_DIALOG, false) == true)
            viewLifecycleOwner.lifecycleScope.launch {
                showPresetsDialog()
            }
    }

    override fun onItemClick(view: View, position: Int) {
        showAddDialog(position)
    }

    private fun showAddDialog(position: Int) {
        val isNew = position == -1

        if (isNew && hasReachedLimit())
            return

        val regexEdit = if (!isNew)
            RegexPresets.getPossiblePreset(viewModel.regexes[position].copy())
        else
            null

        val df = RegexEditsAddDialogFragment()
        df.arguments = Bundle().apply {
            putSingle(regexEdit ?: return@apply)
        }
        df.show(childFragmentManager, null)
    }

    private suspend fun showPresetsDialog() {
        if (hasReachedLimit())
            return

        val dao = PanoDb.getDb(context!!).getRegexEditsDao()

        val presetsAvailable = withContext(Dispatchers.IO) {
            (RegexPresets.presetKeys - dao.allPresets.map {
                it.preset
            }.toSet()).toList()
        }

        if (presetsAvailable.isEmpty()) {
            context!!.toast(R.string.edit_no_presets_available)
            return
        }

        MaterialAlertDialogBuilder(context!!)
            .setTitle(R.string.edit_presets_available)
            .setNegativeButton(android.R.string.cancel, null)
            .setItems(presetsAvailable
                .map { RegexPresets.getString(context!!, it!!) }
                .toTypedArray()
            ) { dialogInterface, idx ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val regexEdit = RegexEdit(
                        preset = presetsAvailable[idx],
                        order = 0
                    )
                    dao.shiftDown()
                    dao.insert(regexEdit)
                }
            }
            .show()
    }

    private fun hasReachedLimit(): Boolean {
        viewModel.countReceiver.value?.let {
            if (it >= Stuff.MAX_PATTERNS) {
                context!!.toast(getString(R.string.edit_max_patterns, Stuff.MAX_PATTERNS))
                return true
            }
        }
        return false
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