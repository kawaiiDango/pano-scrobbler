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
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.google.android.material.chip.Chip
import de.umass.lastfm.*
import kotlinx.android.synthetic.main.chips_charts_period.*
import kotlinx.android.synthetic.main.content_charts.*
import kotlin.math.roundToInt


open class ChartsBaseFragment: ChartsPeriodFragment() {

    lateinit var adapter: ChartsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.content_charts, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (charts_grid?.adapter == null)
            postInit()
    }

    override fun onPause() {
        if (charts_grid?.adapter != null) {
            adapter.removeHandlerCallbacks()
            viewModel.removeAllInfoTasks()
        }
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val glm = charts_grid?.layoutManager as GridLayoutManager?
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
        adapter = ChartsAdapter(view!!)

        val glm = GridLayoutManager(context!!, getNumColumns())
        charts_grid.layoutManager = glm
        (charts_grid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        charts_grid.adapter = adapter
        charts_grid.addItemDecoration(SimpleHeaderDecoration(0, Stuff.dp2px(25, context!!)))

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(glm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadCharts(page)
            }
        }
        loadMoreListener.currentPage = viewModel.page
        charts_grid.addOnScrollListener(loadMoreListener)
        adapter.loadMoreListener = loadMoreListener
        adapter.clickListener = this
        adapter.viewModel = viewModel

        viewModel.chartsReceiver.observe(viewLifecycleOwner, {
            if (it == null && !Main.isOnline && viewModel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            if (it.page >= it.totalPages)
                viewModel.reachedEnd = true
            synchronized(viewModel.chartsData) {
                if (it.page == 1)
                    viewModel.chartsData.clear()
                viewModel.chartsData.addAll(it.pageResults)
            }
            loadMoreListener.currentPage = it.page
            adapter.populate()
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
        charts_grid ?: return
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
        val checkedChip = charts_period.findViewById<Chip>(charts_period.checkedChipId)
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
        var shareText = getString(R.string.charts_share_text,
                user, period.toLowerCase(), type.toLowerCase(), list)

        shareText += "\n\n" + pref.getString(Stuff.PREF_ACTIVITY_SHARE_SIG,
                getString(R.string.share_sig, getString(R.string.share_link)))
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