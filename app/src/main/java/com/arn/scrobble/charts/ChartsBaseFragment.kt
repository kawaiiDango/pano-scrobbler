package com.arn.scrobble.charts

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentChartsBinding
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.google.android.material.chip.Chip
import de.umass.lastfm.*
import kotlin.math.roundToInt


open class ChartsBaseFragment: ChartsPeriodFragment() {

    lateinit var adapter: ChartsAdapter

    private var _chartsBinding: FrameChartsListBinding? = null
    private val chartsBinding
        get() = _chartsBinding!!
    private var _periodChipsBinding: ChipsChartsPeriodBinding? = null
    override val periodChipsBinding
        get() = _periodChipsBinding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        val binding = ContentChartsBinding.inflate(inflater, container, false)
        _chartsBinding = binding.frameChartsList
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
        if (chartsBinding.chartsList.adapter == null)
            postInit()
    }

    override fun onPause() {
        if (chartsBinding.chartsList.adapter != null) {
            adapter.removeHandlerCallbacks()
            viewModel.removeAllInfoTasks()
        }
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val glm = chartsBinding.chartsList.layoutManager as GridLayoutManager?
        glm?.spanCount = getNumColumns()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.charts_menu, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_share)
            share()
        return super.onOptionsItemSelected(item)
    }

    override fun removeHandlerCallbacks() {
        adapter.removeHandlerCallbacks()
    }

    override fun loadFirstPage() {
        loadCharts(1)
    }

    override fun loadWeeklyCharts() {
        viewModel.loadWeeklyCharts()
    }

    override fun postInit() {
        super.postInit()
        adapter = ChartsAdapter(chartsBinding)

        val glm = GridLayoutManager(context!!, getNumColumns())
        chartsBinding.chartsList.layoutManager = glm
        (chartsBinding.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        chartsBinding.chartsList.adapter = adapter
        chartsBinding.chartsList.addItemDecoration(SimpleHeaderDecoration(0, 25.dp))

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(glm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadCharts(page)
            }
        }
        loadMoreListener.currentPage = viewModel.page
        chartsBinding.chartsList.addOnScrollListener(loadMoreListener)
        adapter.loadMoreListener = loadMoreListener
        adapter.clickListener = this
        adapter.viewModel = viewModel

        viewModel.chartsReceiver.observe(viewLifecycleOwner, {
            if (it == null && !Main.isOnline && viewModel.chartsData.size == 0)
                adapter.populate()
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
            if (it.page == 1)
                chartsBinding.chartsList.smoothScrollToPosition(0)
            viewModel.chartsReceiver.value = null
        })

        viewModel.info.observe(viewLifecycleOwner, {
            it ?: return@observe
            val imgUrl = when (val entry = it.second) {
                is Artist -> entry.getImageURL(ImageSize.EXTRALARGE) ?: ""
                is Album -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                is Track -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                else -> ""
            }
            adapter.setImg(it.first, imgUrl)
            viewModel.removeInfoTask(it.first)
        })

        if (viewModel.chartsData.isNotEmpty())
            adapter.populate()
    }

    private fun loadCharts(page: Int) {
        _chartsBinding ?: return
        if (viewModel.reachedEnd) {
            adapter.loadMoreListener.isAllPagesLoaded = true
            return
        }
        viewModel.loadCharts(page)
    }

    private fun share() {
        val entries = viewModel.chartsData
        if (entries.isNullOrEmpty())
            return
        val pref = context?.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE) ?: return
        val type = when (entries[0]) {
            is Artist -> getString(R.string.artists)
            is Album -> getString(R.string.albums)
            else -> getString(R.string.tracks)
        }
        val checkedChip = periodChipsBinding.chartsPeriod.findViewById<Chip>(periodChipsBinding.chartsPeriod.checkedChipId)
        val period = if (checkedChip.id == R.id.charts_choose_week) {
            viewModel.weeklyChart ?: return
            getString(
                    R.string.weekly_range,
                    DateFormat.getMediumDateFormat(context).format(viewModel.weeklyChart!!.from.time),
                    DateFormat.getMediumDateFormat(context).format(viewModel.weeklyChart!!.to.time)
            )
        } else
            checkedChip.text.toString()
        var pos = 1
        val list = entries.take(10).joinToString(separator = "\n") {
            when (it) {
                is Track -> getString(R.string.charts_num_text, pos++, it.artist + " — " + it.name)
                is Album -> getString(R.string.charts_num_text, pos++, it.artist + " — " + it.name)
                else -> getString(R.string.charts_num_text, pos++, it.name)
            }
        }
        val user = if (username != null)
            getString(R.string.possesion, username)
        else
            getString(R.string.my)

        val multiPrefs = MultiPreferences(context!!)
        var shareText = getString(R.string.charts_share_text,
                user, period.toLowerCase(), type.toLowerCase(), list)

        shareText += "\n\n" + multiPrefs.getString(Stuff.PREF_SHARE_SIG, getString(R.string.share_sig))
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, shareText)
        i.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(i, getString(R.string.share_this_chart)))
    }

    private fun getNumColumns(): Int {
        return resources.displayMetrics.widthPixels /
                resources.getDimension(R.dimen.big_grid_size).roundToInt()
    }
}