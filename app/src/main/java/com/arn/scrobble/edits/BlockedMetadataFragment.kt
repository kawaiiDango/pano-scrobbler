package com.arn.scrobble.edits

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.autoNotify
import com.arn.scrobble.Stuff.hideKeyboard
import com.arn.scrobble.VMFactory
import com.arn.scrobble.billing.BillingFragment
import com.arn.scrobble.databinding.ContentBlockedMetadataBinding
import com.arn.scrobble.databinding.DialogBlockedTagsBinding
import com.arn.scrobble.databinding.TextInputEditBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.ui.ItemClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BlockedMetadataFragment: Fragment(), ItemClickListener {
    private var _binding: ContentBlockedMetadataBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by lazy { VMFactory.getVM(this, BlockedMetadataVM::class.java) }
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
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.pref_blocked_metadata)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.blockAdd.setOnClickListener {
            showAddDialog(-1)
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
        binding.empty.text = getString(R.string.n_blocked_metadata, 0)

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

        lifecycleScope.launch {
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

    private fun showAddDialog(position: Int) {
        val binding = DialogBlockedTagsBinding.inflate(layoutInflater)
        val isNew = position == -1
        val blockedMetadata = if (isNew)
            BlockedMetadata()
        else
            viewModel.blockedMetadata[position].copy()

        fun trimmedText(tib: TextInputEditBinding) = tib.edittext.text.toString().trim()

        val dialog = MaterialAlertDialogBuilder(context!!)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                val prevBlockedTag = blockedMetadata.copy()
                blockedMetadata.apply {
                    artist = trimmedText(binding.blockArtist)
                    albumArtist = trimmedText(binding.blockAlbumArtist)
                    album = trimmedText(binding.blockAlbum)
                    track = trimmedText(binding.blockTrack)

                    if (listOf(artist, albumArtist, album, track)
                            .all { it == ""})
                        return@setPositiveButton
                }
                if (prevBlockedTag != blockedMetadata)
                    viewModel.upsert(blockedMetadata)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        val focusClickListener = View.OnFocusChangeListener { view, isFocused ->
            if (isFocused && (activity as Main).billingViewModel.proStatus.value != true) {
                dialog.dismiss()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame, BillingFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.apply {
            blockArtist.root.hint = getString(R.string.artist_channel)
            blockAlbumArtist.root.hint = getString(R.string.album_artist)
            blockAlbum.root.hint = getString(R.string.album)
            blockTrack.root.hint = getString(R.string.track)

            blockArtist.edittext.setText(blockedMetadata.artist)
            blockAlbumArtist.edittext.setText(blockedMetadata.albumArtist)
            blockAlbum.edittext.setText(blockedMetadata.album)
            blockTrack.edittext.setText(blockedMetadata.track)

            blockArtist.edittext.onFocusChangeListener = focusClickListener
            blockAlbumArtist.edittext.onFocusChangeListener = focusClickListener
            blockAlbum.edittext.onFocusChangeListener = focusClickListener
            blockTrack.edittext.onFocusChangeListener = focusClickListener
        }
        dialog.show()
    }

    override fun onItemClick(view: View, position: Int) {
        if (view.id == R.id.edits_delete)
            viewModel.delete(position)
        else {
            showAddDialog(position)
        }
    }
}