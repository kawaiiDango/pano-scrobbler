package com.arn.scrobble.charts

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentChartsOverviewBinding
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.databinding.LayoutCollageFooterBinding
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.RoundedBarChart
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.setProgressCircleColors
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.arn.scrobble.ui.UiUtils.sp
import com.arn.scrobble.ui.UiUtils.toast
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle
import com.faltenreich.skeletonlayout.SkeletonLayout
import com.faltenreich.skeletonlayout.createSkeleton
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import com.kennycason.kumo.CollisionMode
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import com.kennycason.kumo.bg.CircleBackground
import com.kennycason.kumo.font.KumoFont
import com.kennycason.kumo.font.scale.LinearFontScalar
import com.kennycason.kumo.image.AngleGenerator
import com.kennycason.kumo.palette.LinearGradientColorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


open class ChartsOverviewFragment : ChartsPeriodFragment() {

    override val viewModel by viewModels<ChartsOverviewVM>()

    private var listeningActivityChartInited = false
    private var _binding: ContentChartsOverviewBinding? = null
    private val binding
        get() = _binding!!
    private var _periodChipsBinding: ChipsChartsPeriodBinding? = null
    override val periodChipsBinding
        get() = _periodChipsBinding!!

    private var listeningActivitySkeletonJob: Job? = null
    private var tagCloudSkeletonJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            Stuff.ARG_HIDDEN_TAGS_CHANGED,
            this
        ) { requestKey, bundle ->
            if (requestKey == Stuff.ARG_HIDDEN_TAGS_CHANGED && bundle.getBoolean(Stuff.ARG_HIDDEN_TAGS_CHANGED)) {
//                viewModel.tagCloudRequested = false
//                loadMoreSectionsIfNeeded()
                // todo reload
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentChartsOverviewBinding.inflate(inflater, container, false)
        binding.chartsOverviewScrollview.setupInsets()
        listeningActivityChartInited = false
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
        setTitle(R.string.charts)
        if (!listeningActivityChartInited)
            initListeningActivityChart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postInit()
    }

    override fun loadFirstPage(networkOnly: Boolean) {
        val input = MusicEntryLoaderInput(
            type = 0,
            page = 1,
            timePeriod = viewModel.selectedPeriod.value,
            user = activityViewModel.currentUser,
        )

        viewModel.setInput(input)
    }

    override fun postInit() {
        initSection(binding.chartsArtistsFrame, Stuff.TYPE_ARTISTS)
        initSection(binding.chartsAlbumsFrame, Stuff.TYPE_ALBUMS)
        initSection(binding.chartsTracksFrame, Stuff.TYPE_TRACKS)

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

        binding.chartsListeningActivityHeader.headerAction.visibility = View.GONE
        binding.chartsListeningActivityHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_line_chart,
            0,
            0,
            0
        )

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

                    R.id.menu_share -> {
                        viewModel.viewModelScope.launch {
                            val uri = withContext(Dispatchers.IO) { getTagCloudUri() }
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    type = "image/jpeg"
                                }
                                startActivity(intent)
                            }
                        }


                        true
                    }

                    else -> false
                }
            }
            popup.showWithIcons()
        }
        binding.chartsTagCloudHeader.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.vd_tag,
            0,
            0,
            0
        )

        binding.chartsOverviewScrollview.viewTreeObserver.addOnScrollChangedListener {
            setSectionVisibilities()
        }

        binding.swipeRefresh.setProgressCircleColors()
        binding.swipeRefresh.setOnRefreshListener {
            loadFirstPage(true)
        }
        binding.swipeRefresh.isEnabled = false
        // todo: actually fix this not loading the network version

        binding.chartsCreateCollage.setOnClickListener {
            val arguments = Bundle().apply {
                putSingle(viewModel.selectedPeriod.value)
                putInt(Stuff.ARG_TYPE, Stuff.TYPE_ALL)
            }
            findNavController().navigate(R.id.collageGeneratorFragment, arguments)
        }
        binding.chartsListeningActivityHeader.headerText.setText(R.string.listening_activity)

        val listeningActivitySkeleton = binding.chartsListeningActivitySkeleton.createSkeleton(
            UiUtils.mySkeletonConfig(requireContext(), 100f)
        ) as SkeletonLayout

        val tagCloudSkeleton = binding.chartsTagCloudSkeleton.createSkeleton(
            UiUtils.mySkeletonConfig(requireContext(), 1000f)
        )

        collectLatestLifecycleFlow(
            viewModel.listeningActivity.filterNotNull(),
        ) {
            var idx = 0

            if (it.isEmpty()) {
                binding.listeningActivityStatus.isVisible = true
                binding.chartsListeningActivityChart.clear()
            } else {
                binding.listeningActivityStatus.isVisible = false
                val entries = it.map { (timePeriod, count) ->
                    BarEntry((idx++).toFloat(), count.toFloat(), timePeriod.name)
                }
                setListeningActivityData(entries)
            }
        }

        collectLatestLifecycleFlow(viewModel.listeningActivityHasLoaded) {
            if (!it && viewModel.listeningActivity.value.isNullOrEmpty()) {
                binding.listeningActivityStatus.isVisible = false

                listeningActivitySkeletonJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(100)
                    binding.chartsListeningActivityChart.clear()
                    listeningActivitySkeleton.showSkeleton()
                }
            } else {
                listeningActivitySkeletonJob?.cancel()
                listeningActivitySkeleton.showOriginal()
            }
        }


        var lastTagCloudJob: Job? = null

        collectLatestLifecycleFlow(
            viewModel.tagCloudProgress,
        ) {
            if (it == 0.0) {
                binding.chartsTagCloud.visibility = View.INVISIBLE
                binding.chartsTagCloudProgress.progress = 0
                binding.chartsTagCloudStatus.isVisible = false

                tagCloudSkeletonJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(100)
                    tagCloudSkeleton.showSkeleton()
                    delay(5000)
                    binding.chartsTagCloudProgress.show()
                }
            } else if (it == 1.0) {
                tagCloudSkeletonJob?.cancel()
                binding.chartsTagCloudProgress.hide()
            }

            binding.chartsTagCloudProgress.progress =
                (it * binding.chartsTagCloudProgress.max).toInt()
        }

        collectLatestLifecycleFlow(
            viewModel.tagCloud.filterNotNull(),
        ) { tagWeights ->
            lastTagCloudJob?.cancel()
            binding.chartsTagCloudStatus.visibility = View.GONE

            // this fixes java.lang.IllegalArgumentException: width and height must be > 0
            //            at android.graphics.Bitmap.createBitmap(Bitmap.java:1120)
            binding.chartsTagCloud.post {

                lastTagCloudJob = viewLifecycleOwner.lifecycleScope.launch {
                    val bitmap =
                        if (viewModel.tagCloudBitmap?.first == tagWeights.hashCode()) {
                            viewModel.tagCloudBitmap!!.second
                        } else {
                            withContext(Dispatchers.IO) {
                                generateTagCloud(tagWeights, binding.chartsTagCloud.width)
                            }
                        }

                    viewModel.tagCloudBitmap = tagWeights.hashCode() to bitmap

                    binding.chartsTagCloudProgress.hide()
                    tagCloudSkeletonJob?.cancel()
                    tagCloudSkeleton.showOriginal()

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
                    binding.chartsTagCloudNotice.text =
                        getString(R.string.based_on, getString(R.string.artists))
                    binding.chartsTagCloudNotice.visibility = View.VISIBLE
                    crAnimator.start()
                }
            }
        }

        collectLatestLifecycleFlow(
            viewModel.tagCloudError,
        ) {
            if (it is IllegalStateException) {
                binding.chartsTagCloudProgress.hide()
                binding.chartsTagCloudStatus.visibility = View.VISIBLE
                binding.chartsTagCloud.visibility = View.INVISIBLE
                tagCloudSkeleton.showOriginal()
            }
        }

        collectLatestLifecycleFlow(viewModel.allHaveLoaded) {
            if (it) {
                binding.swipeRefresh.isRefreshing = false
            }
        }


        super.postInit()
    }

    private fun initSection(chartFrameBinding: FrameChartsListBinding, type: Int) {
        if (type == Stuff.TYPE_ARTISTS)
            chartFrameBinding.gridItemToReserveSpace.chartInfoSubtitle.visibility = View.GONE

        val sectionFlow = viewModel.getEntries(type).filterNotNull()

        val adapter = ChartsOverviewAdapter(viewLifecycleOwner, chartFrameBinding)
        adapter.clickListener = this

        val itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.shape_divider_chart)!!
        )
        chartFrameBinding.chartsList.addItemDecoration(itemDecor)

        chartFrameBinding.chartsList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        (chartFrameBinding.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =
            false
        chartFrameBinding.chartsList.adapter = adapter

        collectLatestLifecycleFlow(
            sectionFlow,
        ) {
            setHeader(type)
            adapter.populate(it, true)
        }

        collectLatestLifecycleFlow(
            viewModel.getHasLoaded(type),
        ) {
            if (!it) {
                if (adapter.itemCount == 0)
                    adapter.progressVisible(true)
                else
                    binding.swipeRefresh.isRefreshing = true
            } else {
                adapter.progressVisible(false)
            }
        }
    }

    private fun setHeader(type: Int) {
        val count = viewModel.getTotal(type)
        val text: String
        val header: HeaderWithActionBinding

        when (type) {
            Stuff.TYPE_ARTISTS -> {
                text = Stuff.getMusicEntryQString(
                    R.string.artists,
                    R.plurals.num_artists,
                    count,
                    viewModel.periodType.value
                )
                header = binding.chartsArtistsHeader
            }

            Stuff.TYPE_ALBUMS -> {
                text = Stuff.getMusicEntryQString(
                    R.string.albums,
                    R.plurals.num_albums,
                    count,
                    viewModel.periodType.value
                )
                header = binding.chartsAlbumsHeader
            }

            Stuff.TYPE_TRACKS -> {
                text = Stuff.getMusicEntryQString(
                    R.string.tracks,
                    R.plurals.num_tracks,
                    count,
                    viewModel.periodType.value
                )
                header = binding.chartsTracksHeader
            }

            else -> throw IllegalArgumentException("Unknown type: $type")
        }
        header.headerText.text = text
        header.headerAction.isVisible = count != 0
    }

    private fun setSectionVisibilities() {
        _binding ?: return
        val scrollBounds = Rect()
        binding.chartsOverviewScrollview.getHitRect(scrollBounds)
        viewModel.setListeningActivityVisible(
            binding.chartsListeningActivityFrame.getLocalVisibleRect(scrollBounds)
        )

        viewModel.setTagCloudVisible(
            binding.chartsTagCloudFrame.getLocalVisibleRect(scrollBounds)
        )
    }

    private fun launchChartsPager(type: Int) {
        val args = Bundle().apply {
            putInt(Stuff.ARG_TAB, type - 1)
        }
        findNavController().navigate(R.id.chartsPagerFragment, args)
    }

    private fun initListeningActivityChart() {

        val chart = binding.chartsListeningActivityChart
        listeningActivityChartInited = true
        chart.apply {
            setNoDataText("")
            renderer = RoundedBarChart(this, animator, viewPortHandler) // crashes with npe
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)

            description.isEnabled = false
            legend.isEnabled = false
            setMaxVisibleValueCount(60)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setTouchEnabled(false)
            setFitBars(true)

            xAxis.apply {
                position = XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?) =
                        chart.data
                            .dataSets
                            .firstOrNull()
                            ?.getEntriesForXValue(value)
                            ?.firstOrNull()
                            ?.data as? String
                            ?: ""
                }
                textColor = MaterialColors.getColor(
                    chart,
                    com.google.android.material.R.attr.colorControlNormal
                )
                labelCount = 12 // 12 months
            }

            axisRight.isEnabled = false
            axisLeft.textColor = MaterialColors.getColor(
                chart,
                com.google.android.material.R.attr.colorControlNormal
            )
            invalidate()
        }
    }

    private fun setListeningActivityData(entries: List<BarEntry>) {
        val chart = binding.chartsListeningActivityChart

        if (entries.isNotEmpty()) {
            val startColor =
                MaterialColors.getColor(chart, com.google.android.material.R.attr.colorSecondary)
                    .let { ColorUtils.setAlphaComponent(it, 100) }
            val endColor =
                MaterialColors.getColor(chart, com.google.android.material.R.attr.colorSecondary)
            val maxValue = entries.maxOf { it.y }
            val minValue = entries.minOf { it.y }
            val argbEvaluator = ArgbEvaluator()
            val barColors = entries.map {
                argbEvaluator.evaluate(
                    ((it.y - minValue) / (maxValue - minValue)),
                    startColor,
                    endColor,
                ) as Int
            }

            val barDataSet = BarDataSet(entries, "title").apply {
                colors = barColors
                valueTextColor =
                    MaterialColors.getColor(chart, com.google.android.material.R.attr.colorTertiary)
                valueTextSize = 10f
            }
            chart.data = BarData(barDataSet).apply {
                setValueFormatter(
                    object : ValueFormatter() {
                        override fun getBarLabel(barEntry: BarEntry) =
                            barEntry.y.toInt().toString()
                    }
                )
            }
            chart.notifyDataSetChanged()
            chart.animateXY(800, 800, Easing.EaseOutBack) // calls invalidate()
        } else {
            chart.clear()
        }

    }

    private suspend fun generateTagCloud(weights: Map<String, Float>, dimensionPx: Int): Bitmap? {
        val t1 = System.currentTimeMillis()
        val wordFrequenciesFetched = weights.map { (tag, size) ->
            WordFrequency(tag, size.toInt())
        }

        val dimension = Rect(0, 0, dimensionPx, dimensionPx)
        val tintColor = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorSecondary,
            null
        )

        val palette = LinearGradientColorPalette(
            tintColor,
            ContextCompat.getColor(requireContext(), R.color.foreground_pure),
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
                    (currentItem * binding.chartsTagCloudProgress.max / total) + (binding.chartsTagCloudProgress.max * 0.8).toInt()
            }
            build(wordFrequenciesFetched)
        }.bitmap
        val t2 = System.currentTimeMillis()
        if (BuildConfig.DEBUG)
            withContext(Dispatchers.Main) {
                requireContext().toast("Generated in ${t2 - t1}ms")
            }
        return bmp
    }

    private suspend fun getTagCloudUri(): Uri? {
        val tagCloudBitmap = viewModel.tagCloudBitmap?.second ?: return null
        val selectedPeriod = viewModel.selectedPeriod.value ?: return null

        val footerBinding = LayoutCollageFooterBinding.inflate(layoutInflater, null, false)

        if (App.prefs.proStatus) {
            footerBinding.collageFooterBrandingText.visibility = View.GONE
            footerBinding.collageFooterBrandingImage.visibility = View.GONE
            footerBinding.collageFooterBrandingImageBg.visibility = View.GONE
        }

        footerBinding.collageFooterDuration.text = selectedPeriod.name
        footerBinding.collageFooterBrandingText.text =
            getString(R.string.app_name).replace(" ", "\n")
        footerBinding.collageUsernameImage.visibility = View.GONE
        footerBinding.collageTypeImage.setImageResource(R.drawable.vd_tag)

        footerBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(tagCloudBitmap.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        footerBinding.root.layout(
            footerBinding.root.left,
            footerBinding.root.top,
            footerBinding.root.measuredWidth,
            footerBinding.root.measuredHeight
        )
        val footerBitmap = footerBinding.root.drawToBitmap()

        // merge bitmaps
        val bitmap = Bitmap.createBitmap(
            tagCloudBitmap.width,
            tagCloudBitmap.height + footerBinding.root.height,
            Bitmap.Config.ARGB_8888
        )
        Canvas(bitmap).apply {
            // draw opaque bg
            drawColor(
                MaterialColors.getColor(
                    requireContext(),
                    android.R.attr.colorBackground,
                    null
                )
            )
            drawBitmap(tagCloudBitmap, 0f, 0f, null)
            drawBitmap(footerBitmap, 0f, tagCloudBitmap.height.toFloat(), null)
        }

        val tagCloudFile = File(requireContext().filesDir, "tagCloud.jpg")
        withContext(Dispatchers.IO) {
            FileOutputStream(tagCloudFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
        }
        return FileProvider.getUriForFile(
            requireContext(),
            "com.arn.scrobble.fileprovider",
            tagCloudFile
        )
    }

}