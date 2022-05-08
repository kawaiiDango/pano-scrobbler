package com.arn.scrobble.info

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.showIcons
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.charts.ChartsAdapter
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.databinding.ContentInfoExtraFullBinding
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.ScalableGrid
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.google.android.material.appbar.AppBarLayout
import de.umass.lastfm.MusicEntry


open class InfoExtraFullFragment : Fragment(), MusicEntryItemClickListener {

    protected open val type = 0
    private lateinit var adapter: ChartsAdapter
    private lateinit var scalableGrid: ScalableGrid
    private val viewModel by viewModels<ChartsVM>()
    private val artist by lazy {
        arguments?.getString(NLService.B_ARTIST)
            ?: parentFragment!!.arguments!!.getString(NLService.B_ARTIST)!!
    }
    private val track by lazy {
        arguments?.getString(NLService.B_TRACK)
            ?: parentFragment?.arguments?.getString(NLService.B_TRACK)
    }

    private var _chartsBinding: FrameChartsListBinding? = null
    private val chartsBinding
        get() = _chartsBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val binding = ContentInfoExtraFullBinding.inflate(inflater, container, false)
        binding.title.text = if (track == null)
            artist
        else
            getString(R.string.artist_title, artist, track)
        _chartsBinding = binding.frameChartsList
        chartsBinding.root.layoutParams = CoordinatorLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
        }
        chartsBinding.chartsList.isNestedScrollingEnabled = true
        return binding.root
    }

    override fun onDestroyView() {
        _chartsBinding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (chartsBinding.chartsList.adapter == null)
            postInit()
    }

    private fun postInit() {
        adapter = ChartsAdapter(chartsBinding)
        adapter.emptyTextRes = R.string.not_found

        scalableGrid = ScalableGrid(chartsBinding.chartsList)
        (chartsBinding.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =
            false
        chartsBinding.chartsList.adapter = adapter
        chartsBinding.chartsList.addItemDecoration(SimpleHeaderDecoration())

        var itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                context!!,
                R.drawable.shape_divider_chart
            )!!
        )
        chartsBinding.chartsList.addItemDecoration(itemDecor)

        itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                context!!,
                R.drawable.shape_divider_chart
            )!!
        )
        chartsBinding.chartsList.addItemDecoration(itemDecor)

        val loadMoreListener =
            EndlessRecyclerViewScrollListener(chartsBinding.chartsList.layoutManager!!) {
                loadCharts()
            }
        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener
        adapter.clickListener = this
        adapter.viewModel = viewModel

        if (type == Stuff.TYPE_ALBUMS || type == Stuff.TYPE_TRACKS) {
            adapter.showArtists = false
        }

        viewModel.listReceiver.observe(viewLifecycleOwner) {
            if (it == null && !MainActivity.isOnline && viewModel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            viewModel.reachedEnd = true
            synchronized(viewModel.chartsData) {
                viewModel.chartsData.addAll(it)
            }
            adapter.populate()
        }

        if (viewModel.chartsData.isNotEmpty())
            adapter.populate()
        else
            loadCharts()
    }

    private fun loadCharts() {
        _chartsBinding ?: return
        if (viewModel.reachedEnd) {
            adapter.loadMoreListener.isAllPagesLoaded = true
            return
        }
        if (viewModel.chartsData.isEmpty()) {
            when (type) {
                Stuff.TYPE_ARTISTS ->
                    LFMRequester(
                        context!!,
                        viewModel.viewModelScope,
                        viewModel.listReceiver
                    ).getSimilarArtists(artist)
                Stuff.TYPE_ALBUMS ->
                    LFMRequester(
                        context!!,
                        viewModel.viewModelScope,
                        viewModel.listReceiver
                    ).getArtistTopAlbums(artist)
                Stuff.TYPE_TRACKS -> {
                    if (track != null)
                        LFMRequester(
                            context!!,
                            viewModel.viewModelScope,
                            viewModel.listReceiver
                        ).getSimilarTracks(artist, track!!)
                    else
                        LFMRequester(
                            context!!,
                            viewModel.viewModelScope,
                            viewModel.listReceiver
                        ).getArtistTopTracks(artist)
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.grid_size_menu, menu)
        (menu as? MenuBuilder)?.showIcons()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_grid_size -> {
                scalableGrid.resize(increase = true)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (track != null)
            Stuff.setTitle(activity!!, R.string.similar_tracks)
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        val info = InfoFragment()
        info.arguments = entry.toBundle()
        info.show(activity!!.supportFragmentManager, null)
    }
}