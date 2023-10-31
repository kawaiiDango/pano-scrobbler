package com.arn.scrobble.edits

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentSimpleEditsBinding
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.UiUtils.autoNotify
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


class SimpleEditsFragment : Fragment(), ItemClickListener {

    private var _binding: ContentSimpleEditsBinding? = null
    private val binding
        get() = _binding!!

    private val mutex = Mutex()
    private val adapter by lazy { SimpleEditsAdapter(viewModel, this) }
    private val viewModel by viewModels<SimpleEditsVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private var lastSearchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(
            Stuff.ARG_EDIT,
        ) { _, bundle ->
            val edit =
                bundle.getParcelable<SimpleEdit>(Stuff.ARG_EDIT) ?: return@setFragmentResultListener
            viewModel.upsert(edit)
        }

        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentSimpleEditsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        hideKeyboard()
        _binding = null
        super.onDestroyView()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

        binding.editsList.setupInsets()

        binding.editsList.layoutManager = LinearLayoutManager(requireContext())
        binding.editsList.adapter = adapter

        binding.searchTerm.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    viewModel.editsReceiver.value?.let { update(it) }
                }
            }

        })

        mainNotifierViewModel.fabData.value = FabData(
            viewLifecycleOwner,
            R.string.add,
            R.drawable.vd_add_borderless,
            {
                showEditDialog(null)
            }
        )

        binding.searchTerm.editText?.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                textView.clearFocus()
                true
            } else
                false
        }

        binding.empty.text = resources.getQuantityString(R.plurals.num_simple_edits, 0, 0)

        if (!binding.root.isInTouchMode)
            binding.searchTerm.requestFocus()

        viewModel.editsReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            update(it)
            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }
    }

    private fun update(simpleEdits: List<SimpleEdit>) {

        lastSearchJob?.cancel()
        lastSearchJob = viewLifecycleOwner.lifecycleScope.launch {
            mutex.withLock {

                if (viewModel.edits.isNotEmpty())
                    delay(300)

                val oldList = viewModel.edits.toList()
                val searchTerm = binding.searchEdittext.text?.trim()?.toString()?.lowercase()

                withContext(Dispatchers.Default) {
                    viewModel.edits.clear()
                    if (searchTerm.isNullOrEmpty())
                        viewModel.edits.addAll(simpleEdits)
                    else
                        viewModel.edits.addAll(simpleEdits.filter {
                            it.artist.startsWith(searchTerm, ignoreCase = true) ||
                                    it.album.startsWith(searchTerm, ignoreCase = true) ||
                                    it.albumArtist.startsWith(searchTerm, ignoreCase = true) ||
                                    it.track.startsWith(searchTerm, ignoreCase = true)
                        })
                }

                binding.empty.visibility = if (viewModel.edits.isEmpty())
                    View.VISIBLE
                else
                    View.INVISIBLE

//                binding.editsList.visibility = if (viewModel.edits.isNotEmpty())
//                    View.VISIBLE
//                else
//                    View.INVISIBLE

                binding.searchTerm.visibility =
                    if (simpleEdits.size > Stuff.MIN_ITEMS_TO_SHOW_SEARCH)
                        View.VISIBLE
                    else
                        View.GONE

                adapter.autoNotify(oldList, viewModel.edits) { o, n -> o._id == n._id }
            }
        }
    }

    private fun showEditDialog(edit: SimpleEdit?) {
        findNavController().navigate(R.id.simpleEditsEditFragment, bundleOf(Stuff.ARG_EDIT to edit))
    }

    override fun onItemClick(view: View, position: Int) {
        if (view.id == R.id.edits_delete) {
            viewModel.delete(position)
        } else if (viewModel.edits[position].legacyHash == null) {
            showEditDialog(viewModel.edits[position])
        }
    }
}