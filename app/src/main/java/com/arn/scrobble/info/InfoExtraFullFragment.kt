package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.ChartsAdapter
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.OptionsMenuVM
import com.arn.scrobble.ui.ScalableGrid
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getData
import com.arn.scrobble.utils.Stuff.putData
import kotlinx.coroutines.flow.filterNotNull


open class InfoExtraFullFragment : Fragment(), MusicEntryItemClickListener {

    protected open val type = 0
    private lateinit var adapter: ChartsAdapter
    private lateinit var scalableGrid: ScalableGrid
    private val viewModel by viewModels<InfoExtraFullVM>()
    private var _binding: FrameChartsListBinding? = null
    private val binding
        get() = _binding!!
    private val optionsMenuViewModel by activityViewModels<OptionsMenuVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private val musicEntry by lazy { requireArguments().getData<MusicEntry>()!! }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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
            EndlessRecyclerViewScrollListener(binding.chartsList.layoutManager!!) { page ->
                if (!viewModel.reachedEnd)
                    viewModel.setInput(viewModel.input.value!!.copy(page = page))

                adapter.loadMoreListener.isAllPagesLoaded = viewModel.reachedEnd
            }
        loadMoreListener.currentPage = viewModel.input.value?.page ?: 1
        adapter.loadMoreListener = loadMoreListener
        adapter.clickListener = this

        binding.chartsList.addOnScrollListener(loadMoreListener)

        if (type == Stuff.TYPE_ALBUMS || type == Stuff.TYPE_TRACKS) {
            adapter.showArtists = false
        }

        adapter.progressVisible(true)

        collectLatestLifecycleFlow(viewModel.entries.filterNotNull()) {
            adapter.populate(it, false)
        }

        collectLatestLifecycleFlow(optionsMenuViewModel.menuEvent) { (_, menuItemId) ->
            optionsMenuSelected(menuItemId)
        }

        viewModel.setInput(
            MusicEntryLoaderInput(
                type = type,
                entry = musicEntry,
                timePeriod = null,
                user = mainNotifierViewModel.currentUser,
                page = 1
            ), true
        )
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
        if (musicEntry is Track)
            setTitle(
                getString(
                    R.string.artist_title,
                    (musicEntry as Track).artist.name,
                    musicEntry.name
                )
            )
        else if (musicEntry is Artist)
            setTitle(musicEntry.name)
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        val args = Bundle().putData(entry)
        findNavController().navigate(R.id.infoFragment, args)
    }
}