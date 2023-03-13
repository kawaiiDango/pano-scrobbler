package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.doOnNextLayout
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentAlbumTracksBinding
import com.arn.scrobble.databinding.ContentInfoBinding
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.startFadeLoop
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.divider.MaterialDividerItemDecoration
import de.umass.lastfm.Album
import kotlin.math.max


class InfoFragment : BottomSheetDialogFragment() {

    private val viewModel by viewModels<InfoVM>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private var _binding: ContentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val artist = requireArguments().getString(NLService.B_ARTIST)!!
        val album = requireArguments().getString(NLService.B_ALBUM)
        val track = requireArguments().getString(NLService.B_TRACK)
        val username = activityViewModel.currentUser.name
        val pkgName = requireArguments().getString(Stuff.ARG_PKG)

        val adapter = InfoAdapter(
            viewModel,
            activityViewModel,
            this,
            pkgName,
        )
        binding.infoList.layoutManager = LinearLayoutManager(requireContext())
        binding.infoList.itemAnimator = null

        val itemDecor =
            MaterialDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDividerInsetStartResource(requireContext(), R.dimen.divider_inset)
                setDividerInsetEndResource(requireContext(), R.dimen.divider_inset)
                isLastItemDecorated = false
            }

        binding.infoList.addItemDecoration(itemDecor)
        binding.infoList.adapter = adapter

        if (viewModel.infoMap.isEmpty()) {
            binding.root.startFadeLoop()
            viewModel.loadInfo(artist, album, track, username)
        }

        viewModel.infoMapReceiver.observe(viewLifecycleOwner) {
            viewModel.infoMap.putAll(it)
            binding.root.clearAnimation()
            scheduleTransition()
            adapter.notifyDataSetChanged()
        }

    }

    private fun toggleInfoExtra(key: String, container: FrameLayout, button: TextView) {
        if (container.isVisible) { // hide
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.vd_arrow_up,
                0
            )
        } else {
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.vd_arrow_down,
                0
            )
            container.doOnNextLayout {
                binding.infoList.smoothScrollBy(
                    0,
                    max(binding.infoList.height - 300.dp, 300.dp)
                )
            }
        }
        viewModel.infoExtraExpandedMap[key] = !container.isVisible
        container.isVisible = !container.isVisible
    }

    fun toggleAlbumTracks(album: Album, container: FrameLayout, button: TextView) {
//            scheduleTransition()
        val wasPopulated: Boolean
        val localBinding: ContentAlbumTracksBinding

        if (container.isEmpty()) {
            localBinding = ContentAlbumTracksBinding.inflate(layoutInflater, container, false)
            container.addView(localBinding.root)
            wasPopulated = false
        } else {
            localBinding = ContentAlbumTracksBinding.bind(container[0])
            wasPopulated = true
        }
        if (!wasPopulated) {
            val tracks = album.tracks.toList()
            val albumTracksAdapter = AlbumTracksAdapter(tracks)

            albumTracksAdapter.itemClickListener = object : ItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    val args = Bundle().apply {
                        putString(NLService.B_ARTIST, album.artist)
                        putString(NLService.B_ALBUM, album.name)
                        putString(NLService.B_TRACK, tracks[position].name)
                    }
                    findNavController().navigate(R.id.infoFragment, args)
                }
            }

            localBinding.tracksList.layoutManager = LinearLayoutManager(requireContext())
            localBinding.tracksList.adapter = albumTracksAdapter
        }

        toggleInfoExtra(NLService.B_ALBUM, container, button)
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }
}