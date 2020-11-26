package com.arn.scrobble.charts

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
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
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.chips_charts_period.*
import kotlinx.android.synthetic.main.content_charts_overview.*
import kotlinx.android.synthetic.main.content_charts_overview.view.*
import kotlinx.android.synthetic.main.frame_charts_list.view.*
import kotlinx.android.synthetic.main.header_with_action.view.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


open class ChartsOverviewFragment: ChartsPeriodFragment() {

    private lateinit var artistsFragment: FakeArtistFragment
    private lateinit var albumsFragment: FakeAlbumFragment
    private lateinit var tracksFragment: FakeTrackFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_charts_overview, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (charts_artists_frame.charts_list?.adapter == null)
            postInit()
        Stuff.setTitle(activity, 0)
    }

    override fun onPause() {
        if (charts_artists_frame.charts_list?.adapter == null) {
            artistsFragment.viewModel.removeAllInfoTasks()
            albumsFragment.viewModel.removeAllInfoTasks()
            tracksFragment.viewModel.removeAllInfoTasks()
            removeHandlerCallbacks()
        }
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        charts_sparkline_labels?.invalidate = true
    }

    override fun removeHandlerCallbacks() {
        artistsFragment.adapter.removeHandlerCallbacks()
        albumsFragment.adapter.removeHandlerCallbacks()
        tracksFragment.adapter.removeHandlerCallbacks()
    }

    override fun loadFirstPage() {
        val periodIdx = viewModel.periodIdx
        artistsFragment.viewModel.periodIdx = periodIdx
        albumsFragment.viewModel.periodIdx = periodIdx
        tracksFragment.viewModel.periodIdx = periodIdx
        artistsFragment.viewModel.loadCharts(1)
        albumsFragment.viewModel.loadCharts(1)
        tracksFragment.viewModel.loadCharts(1)
        viewModel.periodCountRequested = false
        loadSparklineIfNeeded()
    }

    override fun loadWeeklyCharts() {
        val periodIdx = viewModel.periodIdx
        artistsFragment.viewModel.periodIdx = periodIdx
        albumsFragment.viewModel.periodIdx = periodIdx
        tracksFragment.viewModel.periodIdx = periodIdx
        val wChart = viewModel.weeklyChart
        artistsFragment.viewModel.weeklyChart = wChart
        albumsFragment.viewModel.weeklyChart = wChart
        tracksFragment.viewModel.weeklyChart = wChart
        artistsFragment.viewModel.loadWeeklyCharts()
        albumsFragment.viewModel.loadWeeklyCharts()
        tracksFragment.viewModel.loadWeeklyCharts()
        viewModel.periodCountRequested = false
        loadSparklineIfNeeded()
    }

    override fun postInit() {
        if (Main.isTV)
            for (i in 0 .. charts_period.childCount)
                charts_period.getChildAt(i)?.nextFocusDownId = R.id.charts_overview_scrollview

        artistsFragment = childFragmentManager.findFragmentByTag(Stuff.TYPE_ARTISTS.toString()) as? FakeArtistFragment ?: FakeArtistFragment()
        albumsFragment = childFragmentManager.findFragmentByTag(Stuff.TYPE_ALBUMS.toString()) as? FakeAlbumFragment ?: FakeAlbumFragment()
        tracksFragment = childFragmentManager.findFragmentByTag(Stuff.TYPE_TRACKS.toString()) as? FakeTrackFragment ?: FakeTrackFragment()
        initFragment(artistsFragment, Stuff.TYPE_ARTISTS)
        initFragment(albumsFragment, Stuff.TYPE_ALBUMS)
        initFragment(tracksFragment, Stuff.TYPE_TRACKS)

        charts_artists_header.header_action.setOnClickListener { launchChartsPager(Stuff.TYPE_ARTISTS) }
        setHeader(Stuff.TYPE_ARTISTS)
        charts_artists_header.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_mic, 0, 0, 0)

        charts_albums_header.header_action.setOnClickListener { launchChartsPager(Stuff.TYPE_ALBUMS) }
        setHeader(Stuff.TYPE_ALBUMS)
        charts_albums_header.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_album, 0, 0, 0)

        charts_tracks_header.header_action.setOnClickListener { launchChartsPager(Stuff.TYPE_TRACKS) }
        setHeader(Stuff.TYPE_TRACKS)
        charts_tracks_header.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_note, 0, 0, 0)

        setHeader(Stuff.TYPE_SC)
        charts_sparkline_header.header_action.visibility = View.GONE
        charts_sparkline_header.header_text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.vd_line_chart, 0, 0, 0)

        charts_sparkline_labels.justifyLastLine = true
        charts_sparkline_frame.charts_sparkline.adapter = SparkLineAdapter().apply { baseline = true }
        charts_sparkline_frame.charts_sparkline.setScrubListener { intVal ->
            if (intVal == null) {
                charts_sparkline_frame.charts_sparkline_scrub_info.visibility = View.GONE
                return@setScrubListener
            }
            intVal as Int
            charts_sparkline_frame.charts_sparkline_scrub_info.visibility = View.VISIBLE
            charts_sparkline_frame.charts_sparkline_scrub_info.text =
                    resources.getQuantityString(R.plurals.num_scrobbles_noti, intVal, NumberFormat.getInstance().format(intVal))
            if (charts_scrub_message.visibility == View.VISIBLE) {
                activity!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(Stuff.PREF_ACTIVITY_SCRUB_LEARNT, true)
                        .apply()
                charts_scrub_message.visibility = View.GONE
            }
        }
        charts_overview_scrollview.viewTreeObserver.addOnScrollChangedListener {
            loadSparklineIfNeeded()
        }

        if (!Main.isTV && !activity!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                        .getBoolean(Stuff.PREF_ACTIVITY_SCRUB_LEARNT, false))
            charts_scrub_message.visibility = View.VISIBLE
        viewModel.periodCountReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            val labels = StringBuilder()
            val intList = mutableListOf<Int>()
            it.reversed().forEach {
                labels.append(it.label).append(" ")
                intList += it.count
            }

            val sAdapter = charts_sparkline_frame.charts_sparkline.adapter as SparkLineAdapter
            sAdapter.setData(intList)

            charts_sparkline_frame.charts_sparkline_progress.visibility = View.GONE
            charts_sparkline_labels.text = labels.trimEnd()
            charts_sparkline_frame.charts_sparkline.adapter.notifyDataSetChanged()
            charts_sparkline_frame.charts_sparkline_tick_top.text = NumberFormat.getInstance().format(sAdapter.max())
            charts_sparkline_frame.charts_sparkline_tick_bottom.text = NumberFormat.getInstance().format(0)
        }
        super.postInit()
    }

    private fun initFragment(fragment: ShittyArchitectureFragment, type: Int) {
        val rootView = when(type) {
            Stuff.TYPE_ARTISTS -> charts_artists_frame
            Stuff.TYPE_ALBUMS -> charts_albums_frame
            else -> charts_tracks_frame
        }
        if (!fragment.isAdded)
            childFragmentManager.beginTransaction().add(fragment, type.toString()).commitNow()

        fragment.viewModel = VMFactory.getVM(fragment, ChartsVM::class.java)
        fragment.viewModel.username = username
        fragment.viewModel.type = type

        val adapter = ChartsOverviewAdapter(rootView)
        adapter.viewModel = fragment.viewModel
        adapter.clickListener = this
        fragment.adapter = adapter

        val itemDecor = DividerItemDecoration(context!!, DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.shape_divider_chart)!!)
        rootView.charts_list.addItemDecoration(itemDecor)

        rootView.charts_list.layoutManager = LinearLayoutManager(context!!, RecyclerView.HORIZONTAL, false)
        (rootView.charts_list.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        rootView.charts_list.adapter = adapter

        fragment.viewModel.chartsReceiver.observe(viewLifecycleOwner, {
            if (it == null && !Main.isOnline && fragment.viewModel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            fragment.viewModel.totalCount = it.total
            setHeader(type)
            fragment.viewModel.reachedEnd = true
            synchronized(fragment.viewModel.chartsData) {
                if (it.page == 1)
                    fragment.viewModel.chartsData.clear()
                fragment.viewModel.chartsData.addAll(it.pageResults)
            }
            adapter.populate()
            fragment.viewModel.chartsReceiver.value = null
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

    private fun setHeader(type: Int) {
        var count = 0
        var text = ""
        lateinit var headerView: View
        when (type) {
            Stuff.TYPE_ARTISTS -> {
                count = artistsFragment.viewModel.totalCount
                text = getString(R.string.artists)
                headerView = charts_artists_header
            }
            Stuff.TYPE_ALBUMS -> {
                count = albumsFragment.viewModel.totalCount
                text = getString(R.string.albums)
                headerView = charts_albums_header
            }
            Stuff.TYPE_TRACKS -> {
                count = tracksFragment.viewModel.totalCount
                text = getString(R.string.tracks)
                headerView = charts_tracks_header
            }
            Stuff.TYPE_SC -> {
                charts_sparkline_header.header_text.text = viewModel.periodCountHeader ?: getString(R.string.menu_charts)
                return
            }
        }
        val plus = if (periodChipIds[viewModel.periodIdx] == R.id.charts_choose_week && count == 100)
            "+"
        else
            ""
        if (count != 0) {
            headerView.header_text.text =
                    NumberFormat.getInstance().format(count) + plus + " " + text.toLowerCase()
            headerView.header_action.visibility = View.VISIBLE
        }
        else {
            headerView.header_text.text = text
            headerView.header_action.visibility = View.GONE
        }
    }

    private fun loadSparklineIfNeeded() {
        charts_overview_scrollview ?: return
        if (!viewModel.periodCountRequested) {
            val scrollBounds = Rect()
            charts_overview_scrollview.getHitRect(scrollBounds)
            val partiallyVisible = charts_sparkline_frame.getLocalVisibleRect(scrollBounds)
            if (partiallyVisible)
                calcSparklineDurations()
        }
    }

    private fun launchChartsPager(type: Int) {
        val pf = ChartsPagerFragment()
        val b = Bundle()
        b.putInt(Stuff.ARG_TYPE, type)
        b.putString(Stuff.ARG_USERNAME, username)
        b.putLong(Stuff.ARG_REGISTERED_TIME, registeredTime)
        pf.arguments = b
        (activity as Main).enableGestures()
        activity!!.supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame, pf, Stuff.TAG_CHART_PAGER)
                .addToBackStack(null)
                .commit()
    }

    private fun calcSparklineDurations() {
        viewModel.periodCountRequested = true
        val scList = mutableListOf<ScrobbleCount>()
        val cal = Calendar.getInstance()
        if (periodChipIds[viewModel.periodIdx] == R.id.charts_choose_week)
            cal.timeInMillis = viewModel.weeklyChart!!.to.time
        var lastTime = cal.timeInMillis
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        if (periodChipIds[viewModel.periodIdx] == R.id.charts_choose_week) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            lastTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        when(periodChipIds[viewModel.periodIdx]) {
            R.id.charts_7day,
            R.id.charts_choose_week -> {
                viewModel.periodCountHeader = getString(R.string.graph_daily)
                val rangeEnd = if (periodChipIds[viewModel.periodIdx] == R.id.charts_choose_week)
                    7
                else
                    6
                val nf = NumberFormat.getInstance()
                for (i in 0..rangeEnd) {
                    val sc = ScrobbleCount()
                    sc.to = lastTime
                    if (i != 0)
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                    lastTime = cal.timeInMillis
                    sc.from = lastTime
                    sc.label = nf.format(cal[Calendar.DAY_OF_MONTH])
                    scList += sc
                }
            }
            R.id.charts_1month -> {
                viewModel.periodCountHeader = getString(R.string.graph_weekly)
                var toDate = cal[Calendar.DAY_OF_MONTH]
                var fromDate = 0
                cal.add(Calendar.DAY_OF_WEEK, -(cal.get(Calendar.DAY_OF_WEEK) - 1))
                for (i in 0..3) {
                    val sc = ScrobbleCount()
                    sc.to = lastTime

                    if (i != 0) {
                        cal.add(Calendar.SECOND, -1)
                        toDate = cal[Calendar.DAY_OF_MONTH]
                        cal.add(Calendar.SECOND, 1)
                        cal.add(Calendar.WEEK_OF_YEAR, -1)
                    }
                    fromDate = cal[Calendar.DAY_OF_MONTH]
                    sc.label = "$fromDate-$toDate"
                    lastTime = cal.timeInMillis
                    sc.from = lastTime
                    scList += sc
                }
            }
            R.id.charts_3month,
            R.id.charts_6month,
            R.id.charts_12month -> {
                viewModel.periodCountHeader = getString(R.string.graph_monthly)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val rangeEnd = when (periodChipIds[viewModel.periodIdx]) {
                    R.id.charts_3month -> 2
                    R.id.charts_6month -> 5
                    R.id.charts_12month -> 11
                    else -> throw RuntimeException("this will never happen")
                }
                for (i in 0..rangeEnd) {
                    val sc = ScrobbleCount()
                    sc.to = lastTime
                    if (i != 0) {
                        cal.add(Calendar.MONTH, -1)
                    }
                    var monthLetter = SimpleDateFormat("MMM", Locale.ENGLISH).format(cal.timeInMillis)
                    if (periodChipIds[viewModel.periodIdx] == R.id.charts_12month)
                        monthLetter = monthLetter[0].toString()
                    sc.label = monthLetter.toString()
                    lastTime = cal.timeInMillis
                    sc.from = lastTime
                    scList += sc
                }
            }
            R.id.charts_overall -> {
                viewModel.periodCountHeader = getString(R.string.graph_yearly)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                for (i in 0..6) {
                    val sc = ScrobbleCount()
                    sc.to = lastTime

                    if (i != 0)
                        cal.add(Calendar.YEAR, -1)

                    sc.label = "'" + (cal[Calendar.YEAR] % 100).toString()
                    lastTime = cal.timeInMillis
                    sc.from = lastTime
                    scList += sc
                }
            }
        }
        viewModel.loadScrobbleCounts(scList)
        setHeader(Stuff.TYPE_SC)
    }

    class ScrobbleCount {
        var from = 0L
        var to = System.currentTimeMillis()
        var count = 0
        var label = ""
    }
}