package com.arn.scrobble.edits

import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.autoNotify
import com.arn.scrobble.Stuff.hideKeyboard
import com.arn.scrobble.VMFactory
import com.arn.scrobble.databinding.ContentRegexEditBinding
import com.arn.scrobble.databinding.DialogRegexEditBinding
import com.arn.scrobble.databinding.DialogRegexTestBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.ui.ItemClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.scrobble.ScrobbleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class RegexEditsFragment : Fragment(), ItemClickListener {
    private var _binding: ContentRegexEditBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by lazy { VMFactory.getVM(this, RegexEditsVM::class.java) }
    lateinit var adapter: RegexEditsAdapter

    private val localizedFieldsMap by lazy {
        mapOf(
            NLService.B_TRACK to getString(R.string.track),
            NLService.B_ALBUM to getString(R.string.album),
            NLService.B_ARTIST to getString(R.string.artist),
            NLService.B_ALBUM_ARTIST to getString(R.string.album_artist),
        )
    }

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
        Stuff.setTitle(activity!!, R.string.pref_regex_edits)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.editRegexWarning.movementMethod = LinkMovementMethod.getInstance()

        binding.editAdd.setOnClickListener {
            showAddDialog(-1)
        }

        binding.editPresets.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                showPresetsDialog()
            }
        }

        binding.editTest.setOnClickListener {
            showTestDialog()
        }

        adapter = RegexEditsAdapter(viewModel, this)
        RegexItemTouchHelper(adapter, viewModel).apply {
            attachToRecyclerView(binding.editsList)
            adapter.itemTouchHelper = this
        }
        binding.editsList.adapter = adapter
        binding.editsList.layoutManager = LinearLayoutManager(context!!)

        binding.empty.text = resources.getQuantityString(R.plurals.num_regex_edits, 0, 0)

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
            binding.editPresets.callOnClick()
    }

    override fun onItemClick(view: View, position: Int) {
        showAddDialog(position)

    }

    private fun showAddDialog(position: Int) {
        val isNew = position == -1

        if (isNew && hasReachedLimit())
            return

        val binding = DialogRegexEditBinding.inflate(layoutInflater)
        val dialogBuilder = MaterialAlertDialogBuilder(context!!)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
        val regexEdit = if (isNew)
            RegexEdit()
        else
            RegexPresets.getPossiblePreset(viewModel.regexes[position].copy())

        binding.editName.setText(regexEdit.name)
        binding.editPattern.setText(regexEdit.pattern)
        binding.editReplacement.setText(regexEdit.replacement)
        if (regexEdit.field != null)
            binding.editField.setText(localizedFieldsMap[regexEdit.field])
        if (regexEdit.replaceAll)
            binding.editReplaceAll.isChecked = true
        if (regexEdit.caseSensitive)
            binding.editCaseSensitive.isChecked = true
        if (regexEdit.continueMatching)
            binding.editContinueMatching.isChecked = true

        if (!isNew)
            dialogBuilder.setNeutralButton(R.string.delete) { _, _ ->
                viewModel.delete(position)
            }


        binding.editField.setAdapter(
            ArrayAdapter(
                context!!,
                R.layout.list_item_field,
                localizedFieldsMap.values.toTypedArray()
            )
        )

        fun validate(): Boolean {
            if (binding.editPattern.text.isNullOrBlank()) {
                binding.editPattern.error = "❗"
                return false
            }

            try {
                Pattern.compile(binding.editPattern.text.toString())
            } catch (e: Exception) {
                binding.editPattern.error = "❗"
                return false
            }

            if (binding.editField.text.isNullOrBlank()) {
                binding.editField.error = "❗"
                return false
            }

            binding.editField.error = null
            binding.editPattern.error = null
            return true
        }

        val dialog = dialogBuilder.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validate()) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val dao = PanoDb.getDb(context!!).getRegexEditsDao()

                    val prevRegexEdit = regexEdit.copy()
                    regexEdit.apply {
                        if (isNew)
                            order = (dao.maxOrder ?: -1) + 1
                        if (!binding.editName.text.isNullOrBlank())
                            name = binding.editName.text.toString().trim()
                        pattern = binding.editPattern.text.toString()
                        replacement = binding.editReplacement.text.toString()
                        field = localizedFieldsMap.entries
                            .firstOrNull { it.value == binding.editField.text.toString() }!!.key
                        replaceAll = binding.editReplaceAll.isChecked
                        caseSensitive = binding.editCaseSensitive.isChecked
                        continueMatching = binding.editContinueMatching.isChecked
                    }

                    if (regexEdit == prevRegexEdit)
                        return@launch

                    if (regexEdit.preset != null)
                        regexEdit.preset = null

                    withContext(Dispatchers.Main) {
                        viewModel.upsert(regexEdit)
                    }
                }

                dialog.dismiss()
            }
        }
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
            Stuff.toast(context, getString(R.string.edit_no_presets_available))
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

    private fun showTestDialog() {
        val binding = DialogRegexTestBinding.inflate(layoutInflater)
        val mutex = Mutex()
        val dao = PanoDb.getDb(context!!).getRegexEditsDao()
        binding.matches.text = resources.getQuantityString(R.plurals.num_matches, 0, 0)
        binding.text.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val text = editable.toString()
                        val sd = ScrobbleData()
                        sd.artist = text
                        sd.album = text
                        sd.albumArtist = text
                        sd.track = text
                        val numMatches = dao.performRegexReplace(sd)

                        withContext(Dispatchers.Main) {
                            if (numMatches.all { it.value == 0 })
                                binding.matches.text =
                                    resources.getQuantityString(R.plurals.num_matches, 0, 0)
                            else {
                                val lines = mutableListOf<String>()
                                numMatches.forEach { (field, count) ->
                                    if (count > 0) {
                                        val replacement = when (field) {
                                            NLService.B_ARTIST -> sd.artist
                                            NLService.B_ALBUM -> sd.album
                                            NLService.B_ALBUM_ARTIST -> sd.albumArtist
                                            NLService.B_TRACK -> sd.track
                                            else -> throw IllegalArgumentException()
                                        }
                                        lines += "<b>${localizedFieldsMap[field]!!}</b> " +
                                                "<i>(${
                                                    resources.getQuantityString(
                                                        R.plurals.num_matches,
                                                        count,
                                                        count
                                                    )
                                                }):</i>" +
                                                "<br/>$replacement"
                                    }
                                }
                                binding.matches.text = Html.fromHtml(lines.joinToString("<br/>"))
                            }
                        }
                    }
                }
            }

        })

        MaterialAlertDialogBuilder(context!!)
            .setNegativeButton(R.string.close, null)
            .setView(binding.root)
            .show()
    }

    private fun hasReachedLimit(): Boolean {
        viewModel.countReceiver.value?.let {
            if (it >= Stuff.MAX_PATTERNS) {
                Stuff.toast(context, getString(R.string.edit_max_patterns, Stuff.MAX_PATTERNS))
                return true
            }
        }
        return false
    }
}