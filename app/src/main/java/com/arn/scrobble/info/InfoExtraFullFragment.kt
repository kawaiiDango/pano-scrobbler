package com.arn.scrobble.info

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.charts.ChartsAdapter
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.databinding.ContentInfoExtraFullBinding
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.EntryItemClickListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.google.android.material.appbar.AppBarLayout
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import kotlin.math.roundToInt


open class InfoExtraFullFragment : Fragment(), EntryItemClickListener {

    open val type = 0
    lateinit var adapter: ChartsAdapter
    private val viewModel by lazy { VMFactory.getVM(this, ChartsVM::class.java) }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val glm = chartsBinding.chartsList.layoutManager as GridLayoutManager?
        glm?.spanCount = getNumColumns()
    }

    private fun postInit() {
        adapter = ChartsAdapter(chartsBinding)
        adapter.emptyTextRes = R.string.not_found

        val glm = GridLayoutManager(context!!, getNumColumns())
        chartsBinding.chartsList.layoutManager = glm
        (chartsBinding.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =
            false
        chartsBinding.chartsList.adapter = adapter
        chartsBinding.chartsList.addItemDecoration(SimpleHeaderDecoration(0, 25.dp))

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

        val loadMoreListener = EndlessRecyclerViewScrollListener(glm) {
            loadCharts()
        }
        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener
        adapter.clickListener = this
        adapter.viewModel = viewModel

        viewModel.listReceiver.observe(viewLifecycleOwner) {
            if (it == null && !MainActivity.isOnline && viewModel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            viewModel.reachedEnd = true
            synchronized(viewModel.chartsData) {
                viewModel.chartsData.addAll(it)
            }
            adapter.populate()
            viewModel.listReceiver.value = null
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

    override fun onStart() {
        super.onStart()
        if (track != null)
            Stuff.setTitle(activity!!, R.string.similar_tracks)
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        when (entry) {
            is Artist -> {
                val info = InfoFragment()
                info.arguments = Bundle().apply {
                    putString(NLService.B_ARTIST, entry.name)
                }
                info.show(activity!!.supportFragmentManager, null)
            }
            is Album -> {
                val info = InfoFragment()
                info.arguments = Bundle().apply {
                    putString(NLService.B_ARTIST, entry.artist)
                    putString(NLService.B_ALBUM, entry.name)
                }
                info.show(activity!!.supportFragmentManager, null)
            }
            is Track -> {
                val info = InfoFragment()
                info.arguments = Bundle().apply {
                    putString(NLService.B_ARTIST, entry.artist)
                    putString(NLService.B_ALBUM, entry.album)
                    putString(NLService.B_TRACK, entry.name)
                }
                info.show(activity!!.supportFragmentManager, null)
            }
        }
    }

    private fun getNumColumns(): Int {
        return resources.displayMetrics.widthPixels /
                resources.getDimension(R.dimen.big_grid_size).roundToInt()
    }
}