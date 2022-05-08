package com.arn.scrobble.edits

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.autoNotify
import com.arn.scrobble.Stuff.hideKeyboard
import com.arn.scrobble.databinding.ContentBlockedMetadataBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.ui.ItemClickListener
import kotlinx.coroutines.Dispatchers
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

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity!!, R.string.pref_blocked_metadata)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.blockAdd.setOnClickListener {
            showAddEditDialog(null)
        }

        adapter = BlockedMetadataAdapter(
            viewModel,
            this
        )
        binding.blockList.adapter = adapter
        binding.blockList.layoutManager = LinearLayoutManager(context!!)
        binding.blockList.setOnTouchListener { v, motionEvent ->
            if (binding.searchTerm.editText!!.isFocused) {
                hideKeyboard()
                binding.searchTerm.clearFocus()
            }
            false
        }

        if (!binding.root.isInTouchMode)
            binding.blockAdd.requestFocus()
        binding.empty.text = resources.getQuantityString(R.plurals.num_blocked_metadata, 0, 0)

        viewModel.blockedMetadataReceiver.observe(viewLifecycleOwner) {
            update(it)
        }

        binding.searchEdittext.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                update(viewModel.blockedMetadataReceiver.value)
            }

        })
    }

    private fun update(blockedMetadata: List<BlockedMetadata>?) {
        blockedMetadata ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            mutex.withLock {
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

                binding.blockList.visibility = if (viewModel.blockedMetadata.isNotEmpty())
                    View.VISIBLE
                else
                    View.INVISIBLE

                binding.searchTerm.visibility =
                    if (blockedMetadata.size > Stuff.MIN_ITEMS_TO_SHOW_SEARCH)
                        View.VISIBLE
                    else
                        View.GONE

                adapter.autoNotify(oldList, viewModel.blockedMetadata) { o, n -> o._id == n._id }
            }
        }
    }

    private fun showAddEditDialog(blockedMetadata: BlockedMetadata?) {
        val df = BlockedMetadataAddDialogFragment()
        df.arguments = Bundle().apply {
            putParcelable(Stuff.ARG_DATA, blockedMetadata)
        }
        df.show(childFragmentManager, null)
    }

    override fun onItemClick(view: View, position: Int) {
        if (view.id == R.id.delete)
            viewModel.delete(position)
        else {
            showAddEditDialog(viewModel.blockedMetadata[position])
        }
    }
}