package com.arn.scrobble.charts

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentChartsBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.HtmlImageResGetter
import com.arn.scrobble.ui.OptionsMenuVM
import com.arn.scrobble.ui.ScalableGrid
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.setProgressCircleColors
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder


open class ChartsBaseFragment : ChartsPeriodFragment() {

    private val optionsMenuViewModel by activityViewModels<OptionsMenuVM>()
    private lateinit var adapter: ChartsAdapter
    private lateinit var scalableGrid: ScalableGrid
    private var _chartsBinding: ContentChartsBinding? = null
    private val chartsBinding
        get() = _chartsBinding!!
    private var _periodChipsBinding: ChipsChartsPeriodBinding? = null
    override val periodChipsBinding
        get() = _periodChipsBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ContentChartsBinding.inflate(inflater, container, false)
        binding.frameChartsList.chartsList.setupInsets()
        _chartsBinding = binding
        _periodChipsBinding = binding.chipsChartsPeriod
        return binding.root
    }

    override fun onDestroyView() {
        _chartsBinding = null
        _periodChipsBinding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (chartsBinding.frameChartsList.chartsList.adapter == null)
            postInit()
        updateTitle()
    }

    private fun optionsMenuSelected(menuItemId: Int) {
        when (menuItemId) {
            R.id.menu_collage -> {
                val arguments = Bundle().apply {
                    putSingle(viewModel.selectedPeriod.value ?: return)
                    putInt(Stuff.ARG_TYPE, chartsType)
                }
                findNavController().navigate(R.id.collageGeneratorFragment, arguments)
            }

            R.id.menu_legend -> {

                var text = ""
                text += "<img src='vd_stonks_up_double' /> " + getString(
                    R.string.rank_change,
                    "> +5"
                ) + "<br>"
                text += "<img src='vd_stonks_up' /> " + getString(
                    R.string.rank_change,
                    "+1 — +5"
                ) + "<br>"
                text += "<img src='vd_stonks_no_change' /> " + getString(
                    R.string.rank_change,
                    "0"
                ) + "<br>"
                text += "<img src='vd_stonks_down' /> " + getString(
                    R.string.rank_change,
                    "-1 — -5"
                ) + "<br>"
                text += "<img src='vd_stonks_down_double' /> " + getString(
                    R.string.rank_change,
                    "< -5"
                ) + "<br>"
                text += "<img src='vd_stonks_new' /> " + getString(R.string.rank_change_new)

                val spanned = Html.fromHtml(text, HtmlImageResGetter(requireContext()), null)

                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(spanned)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }

            R.id.menu_grid_size -> {
                scalableGrid.resize(increase = true)
            }
        }
    }

    override fun loadFirstPage(networkOnly: Boolean) {
        _chartsBinding ?: return
        viewModel.loadCharts(networkOnly = networkOnly)
    }

    override fun postInit() {
        super.postInit()

        chartsBinding.frameChartsList.chartsList.isNestedScrollingEnabled = true
        adapter = ChartsAdapter(chartsBinding.frameChartsList)
        scalableGrid = ScalableGrid(chartsBinding.frameChartsList.chartsList)

        (chartsBinding.frameChartsList.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =
            false
        chartsBinding.frameChartsList.chartsList.adapter = adapter
        chartsBinding.frameChartsList.chartsList.addItemDecoration(SimpleHeaderDecoration())

        var itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.shape_divider_chart
            )!!
        )
        chartsBinding.frameChartsList.chartsList.addItemDecoration(itemDecor)

        itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.shape_divider_chart
            )!!
        )
        chartsBinding.frameChartsList.chartsList.addItemDecoration(itemDecor)

        val loadMoreListener =
            EndlessRecyclerViewScrollListener(chartsBinding.frameChartsList.chartsList.layoutManager!!) {
                loadCharts(it)
            }
        loadMoreListener.currentPage = viewModel.page
        chartsBinding.frameChartsList.chartsList.addOnScrollListener(loadMoreListener)
        adapter.loadMoreListener = loadMoreListener
        adapter.clickListener = this
        adapter.viewModel = viewModel

        chartsBinding.chartsSwipeRefresh.isEnabled = false
        chartsBinding.chartsSwipeRefresh.setProgressCircleColors()
        chartsBinding.chartsSwipeRefresh.setOnRefreshListener {
            loadFirstPage(true)
        }

        viewModel.chartsReceiver.observe(viewLifecycleOwner) {
            chartsBinding.chartsSwipeRefresh.isRefreshing = false

            viewModel.totalCount = it.total
            if (it.page >= it.totalPages)
                viewModel.reachedEnd = true
            synchronized(viewModel.chartsData) {
                if (it.page == 1)
                    viewModel.chartsData.clear()
                viewModel.chartsData.addAll(it.pageResults)
            }
            loadMoreListener.currentPage = it.page
            adapter.populate()

            // sometimes does somersaults
//            if (it.page == 1)
//                chartsBinding.frameChartsList.chartsList.smoothScrollToPosition(0)
            updateTitle()
        }

        if (viewModel.chartsData.isNotEmpty())
            adapter.populate()

        optionsMenuViewModel.menuEvent.observe(viewLifecycleOwner) {
            if (!isResumed)
                return@observe

            optionsMenuSelected(it)
        }

        updateTitle()

    }

    private fun updateTitle() {
        setTitle(
            Stuff.getMusicEntryQString(
                when (chartsType) {
                    Stuff.TYPE_TRACKS -> R.string.tracks
                    Stuff.TYPE_ALBUMS -> R.string.albums
                    Stuff.TYPE_ARTISTS -> R.string.artists
                    else -> return
                },
                when (chartsType) {
                    Stuff.TYPE_TRACKS -> R.plurals.num_tracks
                    Stuff.TYPE_ALBUMS -> R.plurals.num_albums
                    Stuff.TYPE_ARTISTS -> R.plurals.num_artists
                    else -> return
                },
                viewModel.totalCount,
                viewModel.periodType.value
            )
        )
    }

    private fun loadCharts(page: Int) {
        _chartsBinding ?: return
        if (viewModel.reachedEnd && page != 1) {
            adapter.loadMoreListener.isAllPagesLoaded = true
            return
        }
        viewModel.loadCharts(page)
    }
}