package com.arn.scrobble.info

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.charts.*
import com.arn.scrobble.ui.EntryItemClickListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.umass.lastfm.*
import kotlinx.android.synthetic.main.content_info_extra.*
import kotlinx.android.synthetic.main.frame_charts_list.view.*
import kotlinx.android.synthetic.main.header_with_action.view.*


class InfoExtraFragment: BottomSheetDialogFragment(), EntryItemClickListener {

    private lateinit var artistsFragment: FakeArtistFragment
    private lateinit var albumsFragment: FakeAlbumFragment
    private lateinit var tracksFragment: FakeTrackFragment
    private val username: String?
        get() = arguments?.getString(Stuff.ARG_USERNAME)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val width = resources.getDimension(R.dimen.bottom_sheet_width)
            if (width > 0)
                dialog.window!!.setLayout(width.toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_info_extra, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val artist = arguments!!.getString(NLService.B_ARTIST)!!
        val track = arguments!!.getString(NLService.B_TITLE)

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

            initFragment(artistsFragment, info_extra_frame3)
            initFragment(albumsFragment, info_extra_frame2)
            initFragment(tracksFragment, info_extra_frame1)

            albumsFragment.adapter.showArtists = false
            albumsFragment.adapter.requestAlbumInfo = false
            tracksFragment.adapter.showArtists = false

            info_extra_header3.header_text.text = getString(R.string.similar_artists)
            info_extra_header3.header_action.visibility = View.GONE
            info_extra_header3.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_mic, 0, 0, 0)
            info_extra_header2.header_text.text = getString(R.string.top_albums)
            info_extra_header2.header_action.visibility = View.GONE
            info_extra_header2.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_album, 0, 0, 0)
            info_extra_header1.header_text.text = getString(R.string.top_tracks)
            info_extra_header1.header_action.visibility = View.GONE
            info_extra_header1.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_note, 0, 0, 0)

            info_extra_title.text = artist

            if (tracksFragment.viewModel.chartsData.isEmpty()) {
                LFMRequester(context!!).getSimilarArtists(artist).asAsyncTask(artistsFragment.viewModel.listReceiver)
                LFMRequester(context!!).getArtistTopAlbums(artist).asAsyncTask(albumsFragment.viewModel.listReceiver)
                LFMRequester(context!!).getArtistTopTracks(artist).asAsyncTask(tracksFragment.viewModel.listReceiver)
            }
        } else {
            initFragment(tracksFragment, info_extra_frame1)
            info_extra_header1.header_text.text = getString(R.string.similar_tracks)
            info_extra_header1.header_action.visibility = View.GONE
            info_extra_header1.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_note, 0, 0, 0)
            info_extra_header2.visibility = View.GONE
            info_extra_frame2.visibility = View.GONE
            info_extra_header3.visibility = View.GONE
            info_extra_frame3.visibility = View.GONE

            info_extra_title.text = "$artist — $track"

            if (tracksFragment.viewModel.chartsData.isEmpty()) {
                LFMRequester(context!!).getSimilarTracks(artist, track).asAsyncTask(tracksFragment.viewModel.listReceiver)
            }
        }
    }

    private fun initFragment(fragment: ShittyArchitectureFragment, rootView: View) {
        fragment.viewModel = VMFactory.getVM(fragment, ChartsVM::class.java)

        val adapter = ChartsOverviewAdapter(rootView)
        adapter.viewModel = fragment.viewModel
        adapter.clickListener = this
        adapter.emptyTextRes = R.string.not_found
        adapter.checkAllForMax = true //top tracks/albums are ordered by listeners and not by play count
        fragment.adapter = adapter

        val itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.shape_divider_chart)!!)
        rootView.charts_list.addItemDecoration(itemDecor)

        rootView.charts_list.layoutManager = LinearLayoutManager(context!!, RecyclerView.HORIZONTAL, false)
        (rootView.charts_list.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        rootView.charts_list.adapter = adapter

        fragment.viewModel.listReceiver.observe(viewLifecycleOwner, {
            if (it == null && !Main.isOnline && fragment.viewModel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            fragment.viewModel.reachedEnd = true
            synchronized(fragment.viewModel.chartsData) {
                fragment.viewModel.chartsData.addAll(it)
            }
            adapter.populate()
            fragment.viewModel.listReceiver.value = null
        })

        fragment.viewModel.info.observe(viewLifecycleOwner, {
            it ?: return@observe
            val imgUrl = when (val entry = it.second) {
                is Artist -> entry.getImageURL(ImageSize.EXTRALARGE) ?: ""
                is Album -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                is Track -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                else -> ""
            }
            adapter.setImg(it.first, imgUrl)
            fragment.viewModel.removeInfoTask(it.first)
        })

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
                b.putString(NLService.B_TITLE, entry.name)
            }
            else -> return
        }

        info.arguments = b
        info.show(parentFragmentManager, null)
    }
}