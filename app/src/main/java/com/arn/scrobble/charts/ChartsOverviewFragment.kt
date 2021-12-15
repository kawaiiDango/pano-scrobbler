package com.arn.scrobble.charts

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.VMFactory
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentChartsOverviewBinding
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.recents.SparkLineAdapter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


open class ChartsOverviewFragment : ChartsPeriodFragment() {

    private lateinit var artistsFragment: FakeArtistFragment
    private lateinit var albumsFragment: FakeAlbumFragment
    private lateinit var tracksFragment: FakeTrackFragment

    private var _binding: ContentChartsOverviewBinding? = null
    private val binding
        get() = _binding!!
    private var _periodChipsBinding: ChipsChartsPeriodBinding? = null
    override val periodChipsBinding
        get() = _periodChipsBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentChartsOverviewBinding.inflate(inflater, container, false)
        _periodChipsBinding = binding.chipsChartsPeriod
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        _periodChipsBinding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        if (binding.chartsArtistsFrame.chartsList.adapter == null)
            postInit()
        Stuff.setTitle(activity!!, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.chartsSparklineLabels.invalidate = true
    }

    override fun loadFirstPage() {
        val periodIdx = viewModel.periodIdx
        artistsFragment.viewModel.periodIdx = periodIdx
        albumsFragment.viewModel.periodIdx = periodIdx
        tracksFragment.viewModel.periodIdx = periodIdx
        artistsFragment.viewModel.loadCharts(1)
        albumsFragment.viewModel.loadCharts(1)
        tracksFragment.viewModel.loadCharts(1)
        viewModel.resetRequestedState()
        loadMoreSectionsIfNeeded()
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
        viewModel.resetRequestedState()
        loadMoreSectionsIfNeeded()
    }

    override fun postInit() {
        if (MainActivity.isTV)
            for (i in 0..periodChipsBinding.chartsPeriod.childCount)
                periodChipsBinding.chartsPeriod.getChildAt(i)?.nextFocusDownId =
                    R.id.charts_overview_scrollview

        artistsFragment =
            childFragmentManager.findFragmentByTag(Stuff.TYPE_ARTISTS.toString()) as? FakeArtistFragment
                ?: FakeArtistFragment()
        albumsFragment =
            childFragmentManager.findFragmentByTag(Stuff.TYPE_ALBUMS.toString()) as? FakeAlbumFragment
                ?: FakeAlbumFragment()
        tracksFragment =
            childFragmentManager.findFragmentByTag(Stuff.TYPE_TRACKS.toString()) as? FakeTrackFragment
                ?: FakeTrackFragment()
        initFragment(artistsFragment, Stuff.TYPE_ARTISTS)
        initFragment(albumsFragment, Stuff.TYPE_ALBUMS)
        initFragment(tracksFragment, Stuff.TYPE_TRACKS)

        binding.chartsArtistsHeader.headerAction.setOnClickListener { launchChartsPager(Stuff.TYPE_ARTISTS) }
        setHeader(Stuff.TYPE_ARTISTS)
        binding.chartsArtistsHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_mic,
            0,
            0,
            0
        )

        binding.chartsAlbumsHeader.headerAction.setOnClickListener { launchChartsPager(Stuff.TYPE_ALBUMS) }
        setHeader(Stuff.TYPE_ALBUMS)
        binding.chartsAlbumsHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_album,
            0,
            0,
            0
        )

        binding.chartsTracksHeader.headerAction.setOnClickListener { launchChartsPager(Stuff.TYPE_TRACKS) }
        setHeader(Stuff.TYPE_TRACKS)
        binding.chartsTracksHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_note,
            0,
            0,
            0
        )

        setHeader(Stuff.TYPE_SC)
        binding.chartsSparklineHeader.headerAction.visibility = View.GONE
        binding.chartsSparklineHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_line_chart,
            0,
            0,
            0
        )

        binding.chartsSparklineLabels.justifyLastLine = true
        binding.chartsSparkline.adapter = SparkLineAdapter().apply { baseline = true }
        binding.chartsSparkline.setScrubListener { intVal ->
            if (intVal == null) {
                binding.chartsSparklineScrubInfo.visibility = View.GONE
                return@setScrubListener
            }
            intVal as Int
            binding.chartsSparklineScrubInfo.visibility = View.VISIBLE
            binding.chartsSparklineScrubInfo.text =
                resources.getQuantityString(
                    R.plurals.num_scrobbles_noti,
                    intVal,
                    NumberFormat.getInstance().format(intVal)
                )
            if (binding.chartsScrubMessage.visibility == View.VISIBLE) {
                prefs.scrubLearnt = true
                binding.chartsScrubMessage.visibility = View.GONE
            }
        }
        binding.chartsOverviewScrollview.viewTreeObserver.addOnScrollChangedListener {
            loadMoreSectionsIfNeeded()
        }

        if (!MainActivity.isTV && !prefs.scrubLearnt)
            binding.chartsScrubMessage.visibility = View.VISIBLE
        viewModel.periodCountReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            val labels = StringBuilder()
            val intList = mutableListOf<Int>()
            it.reversed().forEach {
                labels.append(it.label).append(" ")
                intList += it.count
            }

            val sAdapter = binding.chartsSparkline.adapter as SparkLineAdapter
            sAdapter.setData(intList)

            binding.chartsSparklineProgress.hide()
            binding.chartsSparklineLabels.text = labels.trimEnd()
            binding.chartsSparkline.adapter.notifyDataSetChanged()
            binding.chartsSparklineTickTop.text = NumberFormat.getInstance().format(sAdapter.max())
            binding.chartsSparklineTickBottom.text = NumberFormat.getInstance().format(0)
        }
        super.postInit()
    }

    private fun initFragment(fragment: ShittyArchitectureFragment, type: Int) {
        val rootView = when (type) {
            Stuff.TYPE_ARTISTS -> binding.chartsArtistsFrame
            Stuff.TYPE_ALBUMS -> binding.chartsAlbumsFrame
            else -> binding.chartsTracksFrame
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
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                context!!,
                R.drawable.shape_divider_chart
            )!!
        )
        rootView.chartsList.addItemDecoration(itemDecor)

        rootView.chartsList.layoutManager =
            LinearLayoutManager(context!!, RecyclerView.HORIZONTAL, false)
        (rootView.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        rootView.chartsList.adapter = adapter

        fragment.viewModel.chartsReceiver.observe(viewLifecycleOwner) {
            if (it == null && !MainActivity.isOnline && fragment.viewModel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            fragment.viewModel.totalCount = it.total
            if (it.total > 0)
                viewModel.totalCount = it.total
            setHeader(type)
            fragment.viewModel.reachedEnd = true
            synchronized(fragment.viewModel.chartsData) {
                if (it.page == 1)
                    fragment.viewModel.chartsData.clear()
                fragment.viewModel.chartsData.addAll(it.pageResults)
            }
            adapter.populate()
            if (it.page == 1)
                rootView.chartsList.smoothScrollToPosition(0)
            fragment.viewModel.chartsReceiver.value = null
        }

        if (fragment.viewModel.chartsData.isNotEmpty())
            adapter.populate()
    }

    private fun setHeader(type: Int) {
        var count = 0
        var text = ""
        lateinit var header: HeaderWithActionBinding

        fun getQString(@StringRes zeroStrRes: Int, @PluralsRes pluralRes: Int): String {
            return if (count <= 0)
                getString(zeroStrRes)
            else
                resources.getQuantityString(pluralRes, count, count)
        }

        when (type) {
            Stuff.TYPE_ARTISTS -> {
                count = artistsFragment.viewModel.totalCount
                text = getQString(R.string.artists, R.plurals.num_artists)
                header = binding.chartsArtistsHeader
            }
            Stuff.TYPE_ALBUMS -> {
                count = albumsFragment.viewModel.totalCount
                text = getQString(R.string.albums, R.plurals.num_albums)
                header = binding.chartsAlbumsHeader
            }
            Stuff.TYPE_TRACKS -> {
                count = tracksFragment.viewModel.totalCount
                text = getQString(R.string.tracks, R.plurals.num_tracks)
                header = binding.chartsTracksHeader
            }
            Stuff.TYPE_SC -> {
                binding.chartsSparklineHeader.headerText.text = viewModel.periodCountHeader
                    ?: getString(R.string.charts)
                return
            }
        }
        header.headerText.text = text
        header.headerAction.visibility = if (count != 0)
            View.VISIBLE
        else
            View.GONE
    }

    private fun loadMoreSectionsIfNeeded() {
        _binding ?: return
        val scrollBounds = Rect()
        binding.chartsOverviewScrollview.getHitRect(scrollBounds)
        if (!viewModel.periodCountRequested) {
            val partiallyVisible = binding.chartsSparklineFrame.getLocalVisibleRect(scrollBounds)
            if (partiallyVisible)
                calcSparklineDurations()
        }
    }

    private fun launchChartsPager(type: Int) {
        val pf = ChartsPagerFragment()
        pf.arguments = Bundle().apply {
            putInt(Stuff.ARG_TYPE, type)
            putString(Stuff.ARG_USERNAME, username)
            putLong(Stuff.ARG_REGISTERED_TIME, registeredTime)
        }
        (activity as MainActivity).enableGestures()
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
        cal.setMidnight()
        if (periodChipIds[viewModel.periodIdx] == R.id.charts_choose_week) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            lastTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        when (periodChipIds[viewModel.periodIdx]) {
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
                    var monthLetter =
                        SimpleDateFormat("MMM", Locale.ENGLISH).format(cal.timeInMillis)
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