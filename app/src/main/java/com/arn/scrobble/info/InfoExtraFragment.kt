package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.charts.*
import com.arn.scrobble.databinding.ContentInfoExtraBinding
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.ui.EntryItemClickListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.umass.lastfm.*


class InfoExtraFragment: BottomSheetDialogFragment(), EntryItemClickListener {

    private lateinit var artistsFragment: FakeArtistFragment
    private lateinit var albumsFragment: FakeAlbumFragment
    private lateinit var tracksFragment: FakeTrackFragment
    private val username: String?
        get() = arguments?.getString(Stuff.ARG_USERNAME)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = ContentInfoExtraBinding.inflate(inflater, container, false)

        val artist = arguments!!.getString(NLService.B_ARTIST)!!
        val track = arguments!!.getString(NLService.B_TRACK)

        tracksFragment = childFragmentManager.findFragmentByTag(Stuff.TYPE_TRACKS.toString()) as? FakeTrackFragment ?: FakeTrackFragment()
        if (!tracksFragment.isAdded)
                childFragmentManager.beginTransaction().add(tracksFragment, Stuff.TYPE_TRACKS.toString()).commitNow()

        if (track == null) {
            artistsFragment = childFragmentManager.findFragmentByTag(Stuff.TYPE_ARTISTS.toString()) as? FakeArtistFragment ?: FakeArtistFragment()
            if (!artistsFragment.isAdded)
                childFragmentManager.beginTransaction().add(artistsFragment, Stuff.TYPE_ARTISTS.toString()).commitNow()
            albumsFragment = childFragmentManager.findFragmentByTag(Stuff.TYPE_ALBUMS.toString()) as? FakeAlbumFragment ?: FakeAlbumFragment()
            if (!albumsFragment.isAdded)
                childFragmentManager.beginTransaction().add(albumsFragment, Stuff.TYPE_ALBUMS.toString()).commitNow()

            initFragment(artistsFragment, binding.infoExtraFrame3)
            initFragment(albumsFragment, binding.infoExtraFrame2)
            initFragment(tracksFragment, binding.infoExtraFrame1)

            albumsFragment.adapter.showArtists = false
            albumsFragment.adapter.requestAlbumInfo = false
            tracksFragment.adapter.showArtists = false

            binding.infoExtraHeader3.headerText.text = getString(R.string.similar_artists)
            binding.infoExtraHeader3.headerAction.visibility = View.GONE
            binding.infoExtraHeader3.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_mic, 0, 0, 0)
            binding.infoExtraHeader2.headerText.text = getString(R.string.top_albums)
            binding.infoExtraHeader2.headerAction.visibility = View.GONE
            binding.infoExtraHeader2.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_album, 0, 0, 0)
            binding.infoExtraHeader1.headerText.text = getString(R.string.top_tracks)
            binding.infoExtraHeader1.headerAction.visibility = View.GONE
            binding.infoExtraHeader1.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_note, 0, 0, 0)

            binding.infoExtraTitle.text = artist

            if (tracksFragment.viewModel.chartsData.isEmpty()) {
                LFMRequester(
                    context!!,
                    artistsFragment.viewModel.viewModelScope,
                    artistsFragment.viewModel.listReceiver
                ).getSimilarArtists(artist)
                LFMRequester(
                    context!!,
                    albumsFragment.viewModel.viewModelScope,
                    albumsFragment.viewModel.listReceiver
                ).getArtistTopAlbums(artist)
                LFMRequester(
                    context!!,
                    tracksFragment.viewModel.viewModelScope,
                    tracksFragment.viewModel.listReceiver
                ).getArtistTopTracks(artist)
            }
        } else {
            initFragment(tracksFragment, binding.infoExtraFrame1)
            binding.infoExtraHeader1.headerText.text = getString(R.string.similar_tracks)
            binding.infoExtraHeader1.headerAction.visibility = View.GONE
            binding.infoExtraHeader1.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_note, 0, 0, 0)
            binding.infoExtraHeader2.root.visibility = View.GONE
            binding.infoExtraFrame2.root.visibility = View.GONE
            binding.infoExtraHeader3.root.visibility = View.GONE
            binding.infoExtraFrame3.root.visibility = View.GONE

            binding.infoExtraTitle.text = getString(R.string.artist_title, artist, track)

            if (tracksFragment.viewModel.chartsData.isEmpty()) {
                LFMRequester(
                    context!!,
                    tracksFragment.viewModel.viewModelScope,
                    tracksFragment.viewModel.listReceiver
                ).getSimilarTracks(artist, track)
            }
        }
        return binding.root
    }

    private fun initFragment(fragment: ShittyArchitectureFragment, rootViewBinding: FrameChartsListBinding) {
        fragment.viewModel = VMFactory.getVM(fragment, ChartsVM::class.java)

        val adapter = ChartsOverviewAdapter(rootViewBinding)
        adapter.viewModel = fragment.viewModel
        adapter.clickListener = this
        adapter.emptyTextRes = R.string.not_found
        adapter.checkAllForMax = true //top tracks/albums are ordered by listeners and not by play count
        fragment.adapter = adapter

        val itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.shape_divider_chart)!!)
        rootViewBinding.chartsList.addItemDecoration(itemDecor)

        rootViewBinding.chartsList.layoutManager = LinearLayoutManager(context!!, RecyclerView.HORIZONTAL, false)
        (rootViewBinding.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        rootViewBinding.chartsList.adapter = adapter

        fragment.viewModel.listReceiver.observe(viewLifecycleOwner) {
            if (it == null && !Main.isOnline && fragment.viewModel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            fragment.viewModel.reachedEnd = true
            synchronized(fragment.viewModel.chartsData) {
                fragment.viewModel.chartsData.addAll(it)
            }
            adapter.populate()
            fragment.viewModel.listReceiver.value = null
        }

        fragment.viewModel.info.observe(viewLifecycleOwner) {
            it ?: return@observe
            val imgUrl = when (val entry = it.second) {
                is Artist -> entry.getImageURL(ImageSize.EXTRALARGE) ?: ""
                is Album -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                is Track -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                else -> ""
            }
            adapter.setImg(it.first, imgUrl)
            fragment.viewModel.removeInfoTask(it.first)
        }

        if (fragment.viewModel.chartsData.isNotEmpty())
            adapter.populate()
    }

    override fun onStart() {
        super.onStart()
        if (view?.isInTouchMode == false) {
            val bottomSheetView = dialog!!.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheetView).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        if (this::artistsFragment.isInitialized)
            artistsFragment.viewModel.removeAllInfoTasks()
        if (this::albumsFragment.isInitialized)
            albumsFragment.viewModel.removeAllInfoTasks()
        if (this::tracksFragment.isInitialized)
            tracksFragment.viewModel.removeAllInfoTasks()
        super.onDestroyView()
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        val info = InfoFragment()
        val b = Bundle()
//        b.putString(Stuff.ARG_USERNAME, username)

        when(entry) {
            is Artist -> {
                b.putString(NLService.B_ARTIST, entry.name)
            }
            is Album -> {
                b.putString(NLService.B_ARTIST, entry.artist)
                b.putString(NLService.B_ALBUM, entry.name)
            }
            is Track -> {
                b.putString(NLService.B_ARTIST, entry.artist)
                b.putString(NLService.B_ALBUM, entry.album)
                b.putString(NLService.B_TRACK, entry.name)
            }
            else -> return
        }

        info.arguments = b
        info.show(parentFragmentManager, null)
    }
}