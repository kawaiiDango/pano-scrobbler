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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentSimpleEditsBinding
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.utils.Stuff
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.filterNotNull


class SimpleEditsFragment : Fragment(), ItemClickListener<SimpleEdit> {

    private var _binding: ContentSimpleEditsBinding? = null
    private val binding
        get() = _binding!!

    private val adapter by lazy { SimpleEditsAdapter(this) }
    private val viewModel by viewModels<SimpleEditsVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()

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
                viewModel.setFilter(editable.toString())
            }

        })

        val fabData = FabData(
            viewLifecycleOwner,
            R.string.add,
            R.drawable.vd_add_borderless,
            {
                showEditDialog(null)
            }
        )

        mainNotifierViewModel.setFabData(fabData)

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

        collectLatestLifecycleFlow(viewModel.count) {
            binding.searchTerm.isVisible = it > Stuff.MIN_ITEMS_TO_SHOW_SEARCH

            binding.empty.isVisible = it == 0

        }

        collectLatestLifecycleFlow(viewModel.simpleEditsFiltered.filterNotNull()) {
            adapter.submitList(it)

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }
    }

    private fun showEditDialog(edit: SimpleEdit?) {
        findNavController().navigate(R.id.simpleEditsEditFragment, bundleOf(Stuff.ARG_EDIT to edit))
    }

    override fun onItemClick(view: View, position: Int, item: SimpleEdit) {
        if (view.id == R.id.edits_delete) {
            viewModel.delete(item)
        } else if (item.legacyHash == null) {
            showEditDialog(item)
        }
    }
}