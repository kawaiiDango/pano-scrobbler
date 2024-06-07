package com.arn.scrobble.edits

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentBlockedMetadataBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.hideKeyboard
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.filterNotNull

class BlockedMetadataFragment : Fragment() {
    private var _binding: ContentBlockedMetadataBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<BlockedMetadataVM>()
    private lateinit var adapter: BlockedMetadataAdapter
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        _binding = ContentBlockedMetadataBinding.inflate(inflater, container, false)
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
                showAddEditDialog(null)
            }
        )

        mainNotifierViewModel.setFabData(fabData)

        binding.blockList.setupInsets()

        adapter = BlockedMetadataAdapter(
            onItemClick = { showAddEditDialog(it) },
            onDelete = { viewModel.delete(it) }
        )

        binding.blockList.adapter = adapter
        binding.blockList.layoutManager = LinearLayoutManager(requireContext())
        binding.empty.text = resources.getQuantityString(R.plurals.num_blocked_metadata, 0, 0)

        collectLatestLifecycleFlow(viewModel.count) {
            binding.searchTerm.isVisible = it > Stuff.MIN_ITEMS_TO_SHOW_SEARCH
        }

        collectLatestLifecycleFlow(viewModel.blockedMetadataFiltered.filterNotNull()) {
            binding.empty.isVisible = it.isEmpty()
            binding.blockList.isVisible = it.isNotEmpty()

            adapter.submitList(it) {
                (view.parent as? ViewGroup)?.doOnPreDraw {
                    startPostponedEnterTransition()
                }
            }
        }

        binding.searchEdittext.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                viewModel.setFilter(editable.toString())
            }

        })
    }

    private fun showAddEditDialog(blockedMetadata: BlockedMetadata?) {
        val args = Bundle().apply {
            putSingle(blockedMetadata ?: return@apply)
        }
        findNavController().navigate(R.id.blockedMetadataAddDialogFragment, args)
    }

}