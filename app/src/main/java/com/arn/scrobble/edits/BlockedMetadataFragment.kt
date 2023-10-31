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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.databinding.ContentBlockedMetadataBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.UiUtils.autoNotify
import com.arn.scrobble.ui.UiUtils.hideKeyboard
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BlockedMetadataFragment : Fragment(), ItemClickListener {
    private var _binding: ContentBlockedMetadataBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<BlockedMetadataVM>()
    private val mutex = Mutex()
    private lateinit var adapter: BlockedMetadataAdapter
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        mainNotifierViewModel.fabData.value = FabData(
            viewLifecycleOwner,
            R.string.add,
            R.drawable.vd_add_borderless,
            {
                showAddEditDialog(null)
            }
        )

        binding.blockList.setupInsets()

        adapter = BlockedMetadataAdapter(
            viewModel,
            this
        )
        binding.blockList.adapter = adapter
        binding.blockList.layoutManager = LinearLayoutManager(requireContext())
        binding.empty.text = resources.getQuantityString(R.plurals.num_blocked_metadata, 0, 0)

        viewModel.blockedMetadataReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            update(it)

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

        binding.searchEdittext.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                viewModel.blockedMetadataReceiver.value?.let { update(it) }
            }

        })
    }

    private fun update(blockedMetadata: List<BlockedMetadata>) {

        viewLifecycleOwner.lifecycleScope.launch {
            mutex.withLock {
                if (viewModel.blockedMetadata.isNotEmpty())
                    delay(300)

                val oldList = viewModel.blockedMetadata.toList()
                val searchTerm = binding.searchEdittext.text?.trim()?.toString()?.lowercase()

                withContext(Dispatchers.Default) {
                    viewModel.blockedMetadata.clear()
                    if (searchTerm.isNullOrEmpty())
                        viewModel.blockedMetadata.addAll(blockedMetadata)
                    else
                        viewModel.blockedMetadata.addAll(blockedMetadata.filter {
                            it.artist.startsWith(searchTerm) ||
                                    it.album.startsWith(searchTerm) ||
                                    it.albumArtist.startsWith(searchTerm) ||
                                    it.track.startsWith(searchTerm)
                        })
                }

                binding.empty.visibility = if (viewModel.blockedMetadata.isEmpty())
                    View.VISIBLE
                else
                    View.INVISIBLE

//                binding.blockList.visibility = if (viewModel.blockedMetadata.isNotEmpty())
//                    View.VISIBLE
//                else
//                    View.INVISIBLE

                binding.searchTerm.isVisible =
                    blockedMetadata.size > Stuff.MIN_ITEMS_TO_SHOW_SEARCH

                adapter.autoNotify(oldList, viewModel.blockedMetadata) { o, n -> o._id == n._id }
            }
        }
    }

    private fun showAddEditDialog(blockedMetadata: BlockedMetadata?) {
        val args = Bundle().apply {
            putSingle(blockedMetadata ?: return@apply)
        }
        findNavController().navigate(R.id.blockedMetadataAddDialogFragment, args)
    }

    override fun onItemClick(view: View, position: Int) {
        if (view.id == R.id.delete)
            viewModel.delete(position)
        else {
            showAddEditDialog(viewModel.blockedMetadata[position])
        }
    }
}