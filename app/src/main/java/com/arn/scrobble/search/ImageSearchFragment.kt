package com.arn.scrobble.search

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.spotify.SpotifyMusicItem
import com.arn.scrobble.databinding.ContentImageSearchBinding
import com.arn.scrobble.db.CustomSpotifyMapping
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.main.App
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getData
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.hideKeyboard
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.toast
import com.faltenreich.skeletonlayout.Skeleton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSearchFragment : Fragment(), ItemClickListener<SpotifyMusicItem> {
    private var _binding: ContentImageSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ImageSearchVM>()
    private lateinit var skeleton: Skeleton
    private val musicEntry by lazy { requireArguments().getData<MusicEntry>()!! }
    private val originalMusicEntry by lazy { requireArguments().getData<MusicEntry>(Stuff.ARG_ORIGINAL) }
    private val existingMappings = mutableListOf<CustomSpotifyMapping>()
    private lateinit var imagePickerRequest: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var documentPickerFallbackRequest: ActivityResultLauncher<Array<String>>
    private val prefs = App.prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAxisTransitions(MaterialSharedAxis.X)

        imagePickerRequest =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                setImage(uri)
            }

        documentPickerFallbackRequest =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                setImage(uri)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentImageSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val musicEntry = musicEntry
        val originalMusicEntry = originalMusicEntry

        val searchType = if (musicEntry is Album) Stuff.TYPE_ALBUMS else Stuff.TYPE_ARTISTS
        val searchTerm = if (musicEntry is Album)
            musicEntry.artist!!.name + " " + musicEntry.name
        else
            musicEntry.name

        val printableEntryName = if (musicEntry is Album) {
            getString(R.string.artist_title, musicEntry.artist!!.name, musicEntry.name)
        } else {
            musicEntry.name
        }

        binding.searchTerm.hint = printableEntryName

        binding.searchEdittext.setText(searchTerm)

        binding.imageFromFile.setOnClickListener {
            if (!prefs.squarePhotoLearnt) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.square_photo_hint)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        prefs.squarePhotoLearnt = true
                        launchImagePickerCompat()
                    }
                    .show()
            } else {
                launchImagePickerCompat()
            }
        }

        val adapter = ImageSearchResultsAdapter(requireContext(), this)

        binding.searchResultsList.adapter = adapter
        binding.searchResultsList.layoutManager = LinearLayoutManager(context)
        binding.searchResultsList.itemAnimator?.changeDuration = 0

        skeleton = binding.searchResultsList.createSkeletonWithFade(
            R.layout.list_item_recents_skeleton,
        )

        binding.searchTerm.editText!!.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
            ) {
                if (textView.text.isNotEmpty())
                    doSearch(textView.text.toString(), searchType)
                hideKeyboard()
                textView.clearFocus()
            }
            true
        }

        binding.searchTerm.editText!!.addTextChangedListener(object : TextWatcher {

            override fun onTextChanged(cs: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {
            }

            override fun afterTextChanged(editable: Editable) {
                val term = editable.trim().toString()
                if (term.isNotEmpty()) {
                    viewModel.search(term, searchType)
                }
            }

        })

        viewLifecycleOwner.lifecycleScope.launch {

            val customSpotifyMapping = withContext(Dispatchers.IO) {
                when (musicEntry) {
                    is Album -> PanoDb.db.getCustomSpotifyMappingsDao()
                        .searchAlbum(musicEntry.artist!!.name, musicEntry.name)

                    is Artist -> PanoDb.db.getCustomSpotifyMappingsDao()
                        .searchArtist(musicEntry.name)

                    else -> null
                }
            }

            val customSpotifyMappingOrig = if (hasRedirect())
                withContext(Dispatchers.IO) {
                    when (originalMusicEntry) {
                        is Album -> PanoDb.db.getCustomSpotifyMappingsDao()
                            .searchAlbum(originalMusicEntry.artist!!.name, musicEntry.name)

                        is Artist -> PanoDb.db.getCustomSpotifyMappingsDao()
                            .searchArtist(originalMusicEntry.name)

                        else -> null
                    }
                }
            else
                null

            existingMappings += listOfNotNull(customSpotifyMapping, customSpotifyMappingOrig)

            if (existingMappings.isNotEmpty()) {
                binding.imageReset.isVisible = true

                binding.imageReset.setOnClickListener {
                    existingMappings.forEach {
                        viewLifecycleOwner.lifecycleScope.launch {
                            revokeUriPermission(it)

                            withContext(Dispatchers.IO) {
                                PanoDb.db.getCustomSpotifyMappingsDao()
                                    .delete(it)
                            }
                        }
                    }
                    finish()
                }
            }
        }

        doSearch(searchTerm, searchType)

        collectLatestLifecycleFlow(viewModel.searchResults.filterNotNull()) {
            skeleton.showOriginal()
            if (isResumed)
                binding.searchResultsList.scheduleLayoutAnimation()
            adapter.submitList(it.artists?.items ?: it.albums?.items ?: emptyList())
        }
    }

    private fun launchImagePickerCompat() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable())
            imagePickerRequest.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else {
            // the gms image picker fails on takePersistableUriPermission()
            // so do this until google fixes it
            documentPickerFallbackRequest.launch(arrayOf("image/*"))
        }
    }

    private fun doSearch(term: String, searchType: Int) {
        skeleton.showSkeleton()
        viewModel.search(term, searchType)
    }

    override fun onItemClick(view: View, position: Int, item: SpotifyMusicItem) {
        insertCustomMappings(item, null)
    }

    private fun createCustomMapping(
        musicEntry: MusicEntry,
        spotifyItem: SpotifyMusicItem?,
        fileUri: Uri?
    ): CustomSpotifyMapping {
        return when (musicEntry) {
            is Album -> CustomSpotifyMapping(
                artist = musicEntry.artist!!.name,
                album = musicEntry.name,
                spotifyId = spotifyItem?.id,
                fileUri = fileUri?.toString()
            )

            is Artist -> CustomSpotifyMapping(
                artist = musicEntry.name,
                spotifyId = spotifyItem?.id,
                fileUri = fileUri?.toString()
            )

            else -> throw IllegalArgumentException("Invalid item type: $spotifyItem")
        }
    }

    private fun insertCustomMappings(
        spotifyItem: SpotifyMusicItem?,
        fileUri: Uri?
    ) {
        val musicEntry = musicEntry
        val originalMusicEntry = originalMusicEntry

        val mappings = mutableListOf<CustomSpotifyMapping>()
        mappings += createCustomMapping(musicEntry, spotifyItem, fileUri)

        // revoke uri permission for existing mappings
        existingMappings.forEach { revokeUriPermission(it) }

        // create another mapping for the redirected artist/album
        if (hasRedirect())
            mappings += createCustomMapping(originalMusicEntry!!, spotifyItem, fileUri)

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                PanoDb.db.getCustomSpotifyMappingsDao()
                    .insert(mappings)
            }
            finish()
        }
    }

    private fun revokeUriPermission(mapping: CustomSpotifyMapping) {
        if (mapping.fileUri != null) {
            try {
                requireContext().contentResolver.releasePersistableUriPermission(
                    mapping.fileUri.toUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
            }
        }
    }

    private fun hasRedirect(): Boolean {
        val musicEntry = musicEntry
        val originalMusicEntry = originalMusicEntry

        return (musicEntry is Artist && originalMusicEntry is Artist && musicEntry.name != originalMusicEntry.name) ||
                (musicEntry is Album && originalMusicEntry is Album &&
                        (musicEntry.name != originalMusicEntry.name ||
                                musicEntry.artist!!.name != originalMusicEntry.artist!!.name))

    }

    private fun setImage(uri: Uri?) {
        uri ?: return

        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

            insertCustomMappings(null, uri)
        } catch (e: SecurityException) {
            requireContext().toast(e.localizedMessage ?: e.toString())
        }
    }

    private fun finish() {
        val app = requireActivity().application as App

        app.clearMusicEntryImageCache(musicEntry)
        if (hasRedirect())
            app.clearMusicEntryImageCache(originalMusicEntry!!)
        requireContext().toast(com.google.android.material.R.string.abc_action_mode_done)
        findNavController().popBackStack()
    }

}