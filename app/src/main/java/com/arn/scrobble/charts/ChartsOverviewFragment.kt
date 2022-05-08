package com.arn.scrobble.charts

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.setProgressCircleColors
import com.arn.scrobble.Stuff.sp
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentChartsOverviewBinding
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.recents.SparkLineAdapter
import com.google.android.material.color.MaterialColors
import com.kennycason.kumo.CollisionMode
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import com.kennycason.kumo.bg.CircleBackground
import com.kennycason.kumo.font.KumoFont
import com.kennycason.kumo.font.scale.LinearFontScalar
import com.kennycason.kumo.image.AngleGenerator
import com.kennycason.kumo.palette.LinearGradientColorPalette
import kotlinx.coroutines.*
import java.text.NumberFormat


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

    private val scrobbleCountPeriodTypeMap = mapOf(
        TimePeriodType.WEEK to R.string.graph_weekly,
        TimePeriodType.DAY to R.string.graph_daily,
        TimePeriodType.MONTH to R.string.graph_monthly,
        TimePeriodType.YEAR to R.string.graph_yearly,
    )

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

    override fun loadFirstPage(networkOnly: Boolean) {
        viewModel.periodType.value.let {
            artistsFragment.viewModel.periodType.value = it
            albumsFragment.viewModel.periodType.value = it
            tracksFragment.viewModel.periodType.value = it
        }
        viewModel.selectedPeriod.value.let {
            artistsFragment.viewModel.selectedPeriod.value = it
            albumsFragment.viewModel.selectedPeriod.value = it
            tracksFragment.viewModel.selectedPeriod.value = it
        }
        viewModel.timePeriods.value.let {
            artistsFragment.viewModel.timePeriods.value = it
            albumsFragment.viewModel.timePeriods.value = it
            tracksFragment.viewModel.timePeriods.value = it
        }
        artistsFragment.viewModel.loadCharts(1)
        albumsFragment.viewModel.loadCharts(1)
        tracksFragment.viewModel.loadCharts(1)
        viewModel.resetRequestedState()
        loadMoreSectionsIfNeeded()
    }

    override fun postInit() {
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

        binding.chartsSparklineHeader.headerAction.visibility = View.GONE
        binding.chartsSparklineHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_line_chart,
            0,
            0,
            0
        )

        binding.chartsSparklineLabels.justifyLastLine = true
        binding.chartsSparkline.adapter = SparkLineAdapter().apply { baseline = true }
        binding.chartsSparkline.setScrubListener { intVal, x ->
            if (intVal == null) {
                binding.chartsSparklineScrubInfo.visibility = View.GONE
                return@setScrubListener
            }
            intVal as Int
            binding.chartsSparklineScrubInfo.visibility = View.VISIBLE
            binding.chartsSparklineScrubInfo.text =
                NumberFormat.getInstance().format(intVal)

            if (x != -1f) {
                val infoWidth = binding.chartsSparklineScrubInfo.width
                val infoX = x.toInt()
                    .coerceIn(0, binding.chartsSparkline.width - infoWidth / 2)

                binding.chartsSparklineScrubInfo.x = infoX.toFloat()
            }

            if (binding.chartsScrubMessage.visibility == View.VISIBLE) {
                prefs.scrubLearnt = true
                binding.chartsScrubMessage.visibility = View.GONE
            }
        }

        binding.chartsTagCloudHeader.headerText.text = getString(R.string.tag_cloud)
        binding.chartsTagCloudHeader.headerAction.text = ""
        binding.chartsTagCloudHeader.headerAction.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            R.drawable.vd_more_horiz,
            0
        )
        binding.chartsTagCloudHeader.headerAction.setOnClickListener {
            val popup = PopupMenu(requireContext(), binding.chartsTagCloudHeader.headerAction)
            popup.inflate(R.menu.tag_cloud_menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_hidden_tags -> {
                        HiddenTagsFragment().show(
                            childFragmentManager,
                            null
                        )
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        binding.chartsTagCloudHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_tag,
            0,
            0,
            0
        )

        binding.chartsOverviewScrollview.viewTreeObserver.addOnScrollChangedListener {
            loadMoreSectionsIfNeeded()
        }

        if (!MainActivity.isTV && !prefs.scrubLearnt)
            binding.chartsScrubMessage.visibility = View.VISIBLE

        binding.chartsSwipeRefresh.setProgressCircleColors()
        binding.chartsSwipeRefresh.setOnRefreshListener {
            loadFirstPage(true)
        }

        viewModel.periodCountReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            val labels = StringBuilder()
            val intList = mutableListOf<Int>()
            it.forEach { (timePeriod, count) ->
                labels.append(timePeriod.name).append(" ")
                intList += count
            }

            val sAdapter = binding.chartsSparkline.adapter as SparkLineAdapter
            sAdapter.setData(intList)

            binding.chartsSparklineProgress.hide()
            binding.chartsSparklineLabels.text = labels.trimEnd()
            sAdapter.notifyDataSetChanged()
            binding.chartsSparklineTickTop.text = NumberFormat.getInstance().format(sAdapter.max())
            binding.chartsSparklineTickBottom.text = NumberFormat.getInstance().format(0)

        }

        viewModel.scrobbleCountHeader.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.chartsSparklineHeader.headerText.text = it
        }

        var lastTagCloudJob: Job? = null

        viewModel.tagCloudProgressLd.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.chartsTagCloudProgress.progress =
                (it * binding.chartsTagCloudProgress.max * 0.5).toInt()
        }

        viewModel.tagCloudReceiver.observe(viewLifecycleOwner) { tagWeights ->
            tagWeights ?: return@observe
            lastTagCloudJob?.cancel()

            if (tagWeights.isEmpty()) {
                binding.chartsTagCloudStatus.visibility = View.VISIBLE
                return@observe
            } else {
                binding.chartsTagCloudStatus.visibility = View.GONE
            }

            // this fixes java.lang.IllegalArgumentException: width and height must be > 0
            //            at android.graphics.Bitmap.createBitmap(Bitmap.java:1120)
            binding.chartsTagCloud.post {

                lastTagCloudJob = viewLifecycleOwner.lifecycleScope.launch {
                    val bitmap = if (viewModel.tagCloudBitmap?.first == tagWeights.hashCode()) {
                        viewModel.tagCloudBitmap!!.second
                    } else {
                        withContext(Dispatchers.IO) {
                            generateTagCloud(tagWeights, binding.chartsTagCloud.width)
                        }
                    }

                    viewModel.tagCloudBitmap = tagWeights.hashCode() to bitmap

                    binding.chartsTagCloudProgress.hide()

                    binding.chartsTagCloud.setImageBitmap(bitmap)

                    val center = binding.chartsTagCloud.width / 2

                    delay(10) // gets cancelled if fragment detached

                    val crAnimator = ViewAnimationUtils.createCircularReveal(
                        binding.chartsTagCloud,
                        center,
                        center,
                        0f,
                        center.toFloat()
                    )
                    delay(200)

                    binding.chartsTagCloud.visibility = View.VISIBLE
                    crAnimator.start()
                }
            }
        }

        viewModel.tagCloudError.observe(viewLifecycleOwner) {
            it ?: return@observe
            if (it is IllegalStateException) {
                binding.chartsTagCloudProgress.hide()
                binding.chartsTagCloudStatus.visibility = View.VISIBLE
            }
        }

        viewModel.tagCloudRefresh.observe(viewLifecycleOwner) {
            it ?: return@observe
            viewModel.tagCloudRequested = false
            loadMoreSectionsIfNeeded()
        }

        if (BuildConfig.DEBUG) {
            binding.chartsTagCloudHeader.root.visibility = View.VISIBLE
            binding.chartsTagCloudFrame.visibility = View.VISIBLE
        }
        super.postInit()
    }

    private fun initFragment(fragment: ShittyArchitectureFragment, type: Int) {
        val rootView = when (type) {
            Stuff.TYPE_ARTISTS -> {
                binding.chartsArtistsFrame.gridItemToReserveSpace.chartInfoSubtitle.visibility =
                    View.GONE
                binding.chartsArtistsFrame
            }
            Stuff.TYPE_ALBUMS -> binding.chartsAlbumsFrame
            Stuff.TYPE_TRACKS -> binding.chartsTracksFrame
            else -> throw IllegalArgumentException("Unknown type $type")
        }
        if (!fragment.isAdded)
            childFragmentManager.beginTransaction().add(fragment, type.toString()).commitNow()

        fragment.viewModel = viewModels<ChartsVM>({ fragment }).value
        fragment.viewModel.username = username
        fragment.viewModel.chartsType = type

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

            binding.chartsSwipeRefresh.isRefreshing = false

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

            if (type == Stuff.TYPE_ARTISTS)
                loadMoreSectionsIfNeeded() // load tag cloud
        }

        if (fragment.viewModel.chartsData.isNotEmpty())
            adapter.populate()
    }

    private fun setHeader(type: Int) {
        var count = 0
        val text: String
        val header: HeaderWithActionBinding

        fun getQString(@StringRes zeroStrRes: Int, @PluralsRes pluralRes: Int): String {
            val plus =
                if (count == 1000 && viewModel.periodType.value != TimePeriodType.CONTINUOUS) "+" else ""

            return if (count <= 0)
                getString(zeroStrRes)
            else
                resources.getQuantityString(
                    pluralRes,
                    count,
                    NumberFormat.getInstance().format(count) + plus
                )
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
            else -> throw IllegalArgumentException("Unknown type: $type")
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
                genSparklineDurations()
        }

        if (!viewModel.tagCloudRequested && artistsFragment.viewModel.hasLoaded(Stuff.TYPE_ARTISTS)) {
            val partiallyVisible = binding.chartsTagCloudFrame.getLocalVisibleRect(scrollBounds)
            if (partiallyVisible) {
                showTagCloudProgress()
                viewModel.loadTagCloud(artistsFragment.viewModel.chartsData)
            }
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

    private fun genSparklineDurations() {
        val (timePeriods, periodType) = TimePeriodsGenerator.getSparklinePeriods(
            viewModel.selectedPeriod.value ?: return,
            registeredTime
        )
        viewModel.periodCountRequested = true

        viewModel.loadScrobbleCounts(timePeriods)
        viewModel.scrobbleCountHeader.value = getString(scrobbleCountPeriodTypeMap[periodType]!!)
    }

    private fun showTagCloudProgress() {
        binding.chartsTagCloud.visibility = View.INVISIBLE
        binding.chartsTagCloudProgress.progress = 0
        binding.chartsTagCloudProgress.show()
    }

    private suspend fun generateTagCloud(weights: Map<String, Float>, dimensionPx: Int): Bitmap? {
        val t1 = System.currentTimeMillis()
        val wordFrequenciesFetched = weights.map { (tag, size) ->
            WordFrequency(tag, size.toInt())
        }

        val dimension = Rect(0, 0, dimensionPx, dimensionPx)
        val tintColor = MaterialColors.getColor(context!!, R.attr.colorSecondary, null)

        val palette = LinearGradientColorPalette(
            tintColor,
            ContextCompat.getColor(context!!, R.color.foreground_pure),
            7
        )
        val bmp = WordCloud(dimension, CollisionMode.PIXEL_PERFECT).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setKumoFont(KumoFont(Typeface.DEFAULT))
            setColorPalette(palette)
            setAngleGenerator(AngleGenerator(0.0, 90.0, 2))
            setBackground(CircleBackground(dimensionPx / 2))
            setPadding(4.sp)
            setFontScalar(LinearFontScalar(12.sp, 48.sp))
            setProgressCallback { currentItem, placed, total ->
                _binding ?: return@setProgressCallback

                binding.chartsTagCloudProgress.progress =
                    (currentItem * binding.chartsTagCloudProgress.max / total) + (binding.chartsTagCloudProgress.max * 0.5).toInt()

                if (BuildConfig.DEBUG)
                    Stuff.log("Tag cloud progress: $currentItem/$total ${wordFrequenciesFetched[currentItem].word} ${if (placed) "" else "skipped"}")
            }
            build(wordFrequenciesFetched)
        }.bitmap
        val t2 = System.currentTimeMillis()
        if (BuildConfig.DEBUG)
            withContext(Dispatchers.Main) {
                Stuff.toast(context!!, "Generated in ${t2 - t1}ms")
            }
        return bmp
    }

}