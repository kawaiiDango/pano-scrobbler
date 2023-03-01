package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.charts.ChartsAdapter
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.OptionsMenuVM
import com.arn.scrobble.ui.ScalableGrid
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupInsets
import de.umass.lastfm.MusicEntry


open class InfoExtraFullFragment : Fragment(), MusicEntryItemClickListener {

    protected open val type = 0
    private lateinit var adapter: ChartsAdapter
    private lateinit var scalableGrid: ScalableGrid
    private val viewModel by viewModels<ChartsVM>()
    private val artist by lazy {
        arguments?.getString(NLService.B_ARTIST)!!
    }
    private val track by lazy {
        arguments?.getString(NLService.B_TRACK)
    }

    private var _binding: FrameChartsListBinding? = null
    private val binding
        get() = _binding!!
    private val optionsMenuViewModel by activityViewModels<OptionsMenuVM>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FrameChartsListBinding.inflate(inflater, container, false)
        binding.chartsList.isNestedScrollingEnabled = true
        binding.chartsList.setupInsets()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (binding.chartsList.adapter == null)
            postInit()
    }

    private fun postInit() {

        adapter = ChartsAdapter(binding)
        adapter.emptyTextRes = R.string.not_found

        scalableGrid = ScalableGrid(binding.chartsList)
        (binding.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =
            false
        binding.chartsList.adapter = adapter
        binding.chartsList.addItemDecoration(SimpleHeaderDecoration())

        var itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.shape_divider_chart
            )!!
        )
        binding.chartsList.addItemDecoration(itemDecor)

        itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.shape_divider_chart
            )!!
        )
        binding.chartsList.addItemDecoration(itemDecor)

        val loadMoreListener =
            EndlessRecyclerViewScrollListener(binding.chartsList.layoutManager!!) {
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
            viewModel.reachedEnd = true
            synchronized(viewModel.chartsData) {
                viewModel.chartsData.addAll(it)
            }
            adapter.populate()
        }

        optionsMenuViewModel.menuEvent.observe(viewLifecycleOwner) {
            optionsMenuSelected(it)
        }

        if (viewModel.chartsData.isNotEmpty())
            adapter.populate()
        else
            loadCharts()
    }

    private fun loadCharts() {
        _binding ?: return
        if (viewModel.reachedEnd) {
            adapter.loadMoreListener.isAllPagesLoaded = true
            return
        }
        if (viewModel.chartsData.isEmpty()) {
            LFMRequester(
                viewModel.viewModelScope,
                viewModel.listReceiver
            ).apply {
                when (type) {
                    Stuff.TYPE_ARTISTS -> getSimilarArtists(artist)

                    Stuff.TYPE_ALBUMS -> getArtistTopAlbums(artist)

                    Stuff.TYPE_TRACKS -> {

                        if (track != null)
                            getSimilarTracks(artist, track!!)
                        else
                            getArtistTopTracks(artist)
                    }
                }
            }
        }
    }

    private fun optionsMenuSelected(itemId: Int) {
        when (itemId) {
            R.id.menu_grid_size -> {
                scalableGrid.resize(increase = true)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (track != null)
            setTitle(getString(R.string.artist_title, artist, track))
        else
            setTitle(artist)
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        findNavController().navigate(R.id.infoFragment, entry.toBundle())
    }
}