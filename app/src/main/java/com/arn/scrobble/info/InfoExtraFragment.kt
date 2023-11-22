package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainDialogActivity
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.TrackFeatures
import com.arn.scrobble.charts.ChartsOverviewAdapter
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.databinding.ContentInfoExtraBinding
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.databinding.LayoutSpotifyTrackFeaturesBinding
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.startFadeLoop
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import de.umass.lastfm.MusicEntry
import java.text.DateFormat
import kotlin.math.roundToInt


class InfoExtraFragment : BottomSheetDialogFragment(), MusicEntryItemClickListener {

    private val trackFeaturesVM by viewModels<TrackFeaturesVM>()
    private val infoExtraVM by viewModels<InfoExtraVM>()

    private val track by lazy {
        requireArguments().getString(NLService.B_TRACK)
    }
    private val artist by lazy {
        requireArguments().getString(NLService.B_ARTIST)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ContentInfoExtraBinding.inflate(inflater, container, false)

        if (track == null) {
            initSection(binding.infoExtraFrame3, infoExtraVM.artistsVM, true)
            initSection(binding.infoExtraFrame2, infoExtraVM.albumsVM, false)
            initSection(binding.infoExtraFrame1, infoExtraVM.tracksVM, false)

            binding.infoExtraHeader3.headerText.text = getString(R.string.similar_artists)
            binding.infoExtraHeader3.headerAction.setOnClickListener {
                showFullFragment(R.id.infoPagerFragment, Stuff.TYPE_ARTISTS)
            }
            binding.infoExtraHeader3.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.vd_mic,
                0,
                0,
                0
            )
            binding.infoExtraHeader2.headerText.text = getString(R.string.top_albums)
            binding.infoExtraHeader2.headerAction.setOnClickListener {
                showFullFragment(R.id.infoPagerFragment, Stuff.TYPE_ALBUMS)
            }
            binding.infoExtraHeader2.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.vd_album,
                0,
                0,
                0
            )
            binding.infoExtraHeader1.headerText.text = getString(R.string.top_tracks)
            binding.infoExtraHeader1.headerAction.setOnClickListener {
                showFullFragment(R.id.infoPagerFragment, Stuff.TYPE_TRACKS)
            }
            binding.infoExtraHeader1.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.vd_note,
                0,
                0,
                0
            )

            binding.infoExtraTitle.text = artist

            binding.infoExtraFrame1.gridItemToReserveSpace.chartInfoSubtitle.visibility = View.GONE
            binding.infoExtraFrame2.gridItemToReserveSpace.chartInfoSubtitle.visibility = View.GONE
            binding.infoExtraFrame3.gridItemToReserveSpace.chartInfoSubtitle.visibility = View.GONE
            binding.infoExtraFrame3.gridItemToReserveSpace.chartInfoScrobbles.visibility = View.GONE

            if (infoExtraVM.tracksVM.chartsData.isEmpty()) {
                LFMRequester(
                    infoExtraVM.artistsVM.viewModelScope,
                    infoExtraVM.artistsVM.listReceiver
                ).getSimilarArtists(artist)
                LFMRequester(
                    infoExtraVM.albumsVM.viewModelScope,
                    infoExtraVM.albumsVM.listReceiver
                ).getArtistTopAlbums(artist)
                LFMRequester(
                    infoExtraVM.tracksVM.viewModelScope,
                    infoExtraVM.tracksVM.listReceiver
                ).getArtistTopTracks(artist)
            }
        } else {
            // spotify info
            trackFeaturesVM.spotifyTrackWithFeatures.observe(viewLifecycleOwner) { spotifyTrack ->
                if (spotifyTrack?.features == null) {
                    binding.root.clearAnimation()
                    return@observe
                }
                val features = spotifyTrack.features!!

                this@InfoExtraFragment.scheduleTransition()
                val spotifyFeaturesBinding = LayoutSpotifyTrackFeaturesBinding.bind(
                    binding.infoSpotifyFeatures.inflate()
                )
                drawRadarChart(spotifyFeaturesBinding.featuresRadarChart, features)

                spotifyFeaturesBinding.featuresPopularityProgress.progress = spotifyTrack.popularity
                spotifyFeaturesBinding.featuresPopularity.text =
                    getString(R.string.popularity, spotifyTrack.popularity)

                spotifyFeaturesBinding.featuresReleaseDate.text = DateFormat
                    .getDateInstance(DateFormat.MEDIUM)
                    .format(spotifyTrack.getReleaseDateDate()!!)
                spotifyFeaturesBinding.featuresKey.text = features.getKeyString()
                spotifyFeaturesBinding.featuresBpm.text =
                    "${features.tempo.roundToInt()} bpm • ${features.time_signature}/4"
                spotifyFeaturesBinding.featuresLoudness.text =
                    String.format("%.2f dB", features.loudness)

                binding.root.clearAnimation()
            }

            if (trackFeaturesVM.spotifyTrackWithFeatures.value == null)
                trackFeaturesVM.loadTrackFeatures(artist, track!!)

            initSection(binding.infoExtraFrame1, infoExtraVM.tracksVM, true)
            binding.infoExtraHeader1.headerText.text = getString(R.string.similar_tracks)
            binding.infoExtraHeader1.headerAction.setOnClickListener {
                showFullFragment(R.id.trackExtraFragment, Stuff.TYPE_TRACKS)
            }

            binding.infoExtraHeader1.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.vd_note,
                0,
                0,
                0
            )
            binding.infoExtraHeader2.root.visibility = View.GONE
            binding.infoExtraFrame2.root.visibility = View.GONE
            binding.infoExtraHeader3.root.visibility = View.GONE
            binding.infoExtraFrame3.root.visibility = View.GONE

            binding.infoExtraTitle.text = getString(R.string.artist_title, artist, track)

            if (infoExtraVM.tracksVM.chartsData.isEmpty()) {
                LFMRequester(
                    infoExtraVM.tracksVM.viewModelScope,
                    infoExtraVM.tracksVM.listReceiver
                ).getSimilarTracks(artist, track!!)
            }
            binding.root.startFadeLoop()
        }

        return binding.root
    }

    private fun initSection(
        rootViewBinding: FrameChartsListBinding,
        sectionVM: ChartsVM,
        showArtists: Boolean
    ) {
        val adapter = ChartsOverviewAdapter(rootViewBinding)
        adapter.viewModel = sectionVM
        adapter.clickListener = this
        adapter.emptyTextRes = R.string.not_found
        //top tracks/albums are ordered by listeners and not by play count
        adapter.checkAllForMax = true
        adapter.showArtists = showArtists

        val itemDecor = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        itemDecor.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.shape_divider_chart
            )!!
        )
        rootViewBinding.chartsList.addItemDecoration(itemDecor)

        rootViewBinding.chartsList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        (rootViewBinding.chartsList.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =
            false
        rootViewBinding.chartsList.adapter = adapter

        sectionVM.listReceiver.observe(viewLifecycleOwner) {
            sectionVM.reachedEnd = true
            synchronized(sectionVM.chartsData) {
                sectionVM.chartsData.addAll(it)
            }
            adapter.populate()
        }

        if (sectionVM.chartsData.isNotEmpty())
            adapter.populate()
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        val args = entry.toBundle()
        findNavController().navigate(R.id.infoFragment, args)
    }

    private fun showFullFragment(@IdRes fragmentId: Int, type: Int) {
        val args = requireArguments().clone() as Bundle
        args.putInt(Stuff.ARG_TYPE, type)

        if (activity is MainDialogActivity) {
            NavDeepLinkBuilder(requireContext())
                .setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.nav_graph)
                .setDestination(fragmentId)
                .setArguments(args)
                .createPendingIntent()
                .send()
        } else if (activity is MainActivity)
            findNavController().navigate(fragmentId, args)
    }

    private fun drawRadarChart(chart: RadarChart, features: TrackFeatures) {
        chart.description.isEnabled = false
        chart.webColor =
            MaterialColors.getColor(chart, com.google.android.material.R.attr.colorControlNormal)
        chart.webColorInner =
            MaterialColors.getColor(chart, com.google.android.material.R.attr.colorControlNormal)
        chart.webAlpha = 100
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)


        val entries = listOf(
            RadarEntry(features.acousticness, getString(R.string.acoustic)),
            RadarEntry(features.danceability, getString(R.string.danceable)),
            RadarEntry(features.energy, getString(R.string.energetic)),
            RadarEntry(features.instrumentalness, getString(R.string.instrumental)),
            RadarEntry(features.valence, getString(R.string.valence))
        )

        chart.yAxis.apply {
            xOffset = 0f
            yOffset = 0f
            axisMinimum = 0f
            axisMaximum = 1f

            setLabelCount(entries.size, false)
            setDrawLabels(false)
        }

        chart.xAxis.apply {
            xOffset = 0f
            yOffset = 0f

            textSize = 11f
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?) =
                    entries.getOrNull(value.toInt())?.data as? String ?: ""
            }
            textColor = MaterialColors.getColor(
                chart,
                com.google.android.material.R.attr.colorControlNormal
            )
        }

        val set = RadarDataSet(entries, "").apply {
            color = MaterialColors.getColor(chart, com.google.android.material.R.attr.colorPrimary)
            fillColor =
                MaterialColors.getColor(chart, com.google.android.material.R.attr.colorSecondary)
            setDrawFilled(true)
            fillAlpha = 180
            lineWidth = 1.5f
            isDrawHighlightCircleEnabled = false
            setDrawHighlightIndicators(false)
        }

        chart.data = RadarData(set).apply {
            setDrawValues(false)
            setValueTextColor(
                MaterialColors.getColor(
                    chart,
                    com.google.android.material.R.attr.colorControlNormal
                )
            )
        }
    }
}