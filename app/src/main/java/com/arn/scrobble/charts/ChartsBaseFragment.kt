package com.arn.scrobble.charts

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentChartsBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.HtmlImageResGetter
import com.arn.scrobble.ui.ScalableGrid
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.setProgressCircleColors
import com.arn.scrobble.ui.UiUtils.showIcons
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Period
import de.umass.lastfm.Track


open class ChartsBaseFragment : ChartsPeriodFragment() {

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
        setHasOptionsMenu(true)
        val binding = ContentChartsBinding.inflate(inflater, container, false)
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
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.charts_menu, menu)
        inflater.inflate(R.menu.grid_size_menu, menu)
        (menu as? MenuBuilder)?.showIcons()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> share()
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

                val spanned = Html.fromHtml(text, HtmlImageResGetter(context!!), null)

                MaterialAlertDialogBuilder(context!!)
                    .setMessage(spanned)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            R.id.menu_grid_size -> {
                scalableGrid.resize(increase = true)
            }
        }
        return super.onOptionsItemSelected(item)
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

        var itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                context!!,
                R.drawable.shape_divider_chart
            )!!
        )
        chartsBinding.frameChartsList.chartsList.addItemDecoration(itemDecor)

        itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                context!!,
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
            if (it == null && !Stuff.isOnline && viewModel.chartsData.size == 0)
                adapter.populate()

            chartsBinding.chartsSwipeRefresh.isRefreshing = false

            it ?: return@observe
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
        }

        if (viewModel.chartsData.isNotEmpty())
            adapter.populate()
    }

    private fun loadCharts(page: Int) {
        _chartsBinding ?: return
        if (viewModel.reachedEnd && page != 1) {
            adapter.loadMoreListener.isAllPagesLoaded = true
            return
        }
        viewModel.loadCharts(page)
    }

    private fun share() {
        val entries = viewModel.chartsData
        if (entries.isEmpty() || viewModel.selectedPeriod.value == null)
            return
        val topType = when (entries[0]) {
            is Artist -> getString(R.string.top_artists)
            is Album -> getString(R.string.top_albums)
            else -> getString(R.string.top_tracks)
        }
        val period = when (viewModel.selectedPeriod.value!!.period) {
            Period.WEEK -> getString(R.string.weekly)
            Period.ONE_MONTH -> getString(R.string.monthly)
            else -> viewModel.selectedPeriod.value?.name ?: return
        }
        var pos = 1
        val list = entries.take(10).joinToString(separator = "\n") {
            when (it) {
                is Track -> getString(
                    R.string.charts_num_text,
                    pos++,
                    getString(R.string.artist_title, it.artist, it.name)
                )
                is Album -> getString(
                    R.string.charts_num_text,
                    pos++,
                    getString(R.string.artist_title, it.artist, it.name)
                )
                else -> getString(R.string.charts_num_text, pos++, it.name)
            }
        }

        var shareText = if (!activityViewModel.userIsSelf) {
            "$topType • $period • ${activityViewModel.peekUser().name}:\n\n$list"
        } else {
            "$topType • $period:\n\n$list"
        }

        if ((activity as MainActivity).billingViewModel.proStatus.value != true)
            shareText += "\n\n" + getString(R.string.share_sig)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareText)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share_this_chart)))
    }
}