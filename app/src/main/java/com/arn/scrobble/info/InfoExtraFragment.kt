package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainDialogActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.spotify.TrackFeatures
import com.arn.scrobble.charts.ChartsOverviewAdapter
import com.arn.scrobble.databinding.ContentInfoExtraBinding
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.databinding.LayoutSpotifyTrackFeaturesBinding
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.arn.scrobble.ui.UiUtils.startFadeLoop
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.copyToClipboard
import com.arn.scrobble.utils.Stuff.getData
import com.arn.scrobble.utils.Stuff.putData
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import java.text.DateFormat
import kotlin.math.roundToInt


class InfoExtraFragment : BottomSheetDialogFragment(), MusicEntryItemClickListener {

    private val trackFeaturesVM by viewModels<TrackFeaturesVM>()
    private val infoExtraVM by viewModels<InfoExtraVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ContentInfoExtraBinding.inflate(inflater, container, false)
        val musicEntry = requireArguments().getData<MusicEntry>()!!

        binding.infoExtraTitle.setOnLongClickListener {
            requireContext().copyToClipboard(binding.infoExtraTitle.text.toString())
            true
        }

        val musicEntryLoaderInput = MusicEntryLoaderInput(
            type = -1,
            entry = musicEntry,
            timePeriod = null,
            user = mainNotifierViewModel.currentUser,
            page = 1
        )

        when (musicEntry) {
            is Artist -> {
                initSection(binding.infoExtraFrame3, infoExtraVM.artistsVM, true)
                initSection(binding.infoExtraFrame2, infoExtraVM.albumsVM, false)
                initSection(binding.infoExtraFrame1, infoExtraVM.tracksVM, false)

                binding.infoExtraHeader3.headerText.text = getString(R.string.similar_artists)
                binding.infoExtraHeader3.headerAction.setOnClickListener {
                    showFullFragment(R.id.infoPagerFragment, 2)
                }
                binding.infoExtraHeader3.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.vd_mic,
                    0,
                    0,
                    0
                )
                binding.infoExtraHeader2.headerText.text = getString(R.string.top_albums)
                binding.infoExtraHeader2.headerAction.setOnClickListener {
                    showFullFragment(R.id.infoPagerFragment, 1)
                }
                binding.infoExtraHeader2.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.vd_album,
                    0,
                    0,
                    0
                )
                binding.infoExtraHeader1.headerText.text = getString(R.string.top_tracks)
                binding.infoExtraHeader1.headerAction.setOnClickListener {
                    showFullFragment(R.id.infoPagerFragment, 0)
                }
                binding.infoExtraHeader1.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.vd_note,
                    0,
                    0,
                    0
                )

                binding.infoExtraTitle.text = musicEntry.name

                binding.infoExtraFrame1.gridItemToReserveSpace.chartInfoSubtitle.visibility =
                    View.GONE
                binding.infoExtraFrame2.gridItemToReserveSpace.chartInfoSubtitle.visibility =
                    View.GONE
                binding.infoExtraFrame3.gridItemToReserveSpace.chartInfoSubtitle.visibility =
                    View.GONE
                binding.infoExtraFrame3.gridItemToReserveSpace.chartInfoScrobbles.visibility =
                    View.GONE


                infoExtraVM.tracksVM.setInput(musicEntryLoaderInput.copy(type = Stuff.TYPE_TRACKS))
                infoExtraVM.albumsVM.setInput(musicEntryLoaderInput.copy(type = Stuff.TYPE_ALBUMS))
                infoExtraVM.artistsVM.setInput(musicEntryLoaderInput.copy(type = Stuff.TYPE_ARTISTS))

                collectLatestLifecycleFlow(
                    combine(
                        infoExtraVM.tracksVM.hasLoaded,
                        infoExtraVM.albumsVM.hasLoaded,
                        infoExtraVM.artistsVM.hasLoaded
                    ) { tracksLoaded, albumsLoaded, artistsLoaded ->
                        tracksLoaded && albumsLoaded && artistsLoaded
                    }
                ) {
                    if (!it)
                        binding.root.startFadeLoop()
                    else
                        binding.root.clearAnimation()
                }
            }

            is Track -> {
                // spotify info

                collectLatestLifecycleFlow(trackFeaturesVM.spotifyTrackWithFeatures.filterNotNull()) { spotifyTrack ->

                    spotifyTrack.features ?: return@collectLatestLifecycleFlow
                    val features = spotifyTrack.features

                    this@InfoExtraFragment.scheduleTransition()
                    val spotifyFeaturesBinding = LayoutSpotifyTrackFeaturesBinding.bind(
                        binding.infoSpotifyFeatures.inflate()
                    )
                    drawRadarChart(spotifyFeaturesBinding.featuresRadarChart, features)

                    spotifyFeaturesBinding.featuresPopularityProgress.progress =
                        spotifyTrack.popularity
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

                }

                trackFeaturesVM.loadTrackFeaturesIfNeeded(musicEntry)

                initSection(binding.infoExtraFrame1, infoExtraVM.tracksVM, true)
                binding.infoExtraHeader1.headerText.text = getString(R.string.similar_tracks)
                binding.infoExtraHeader1.headerAction.setOnClickListener {
                    showFullFragment(R.id.trackExtraFragment, 0)
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

                binding.infoExtraTitle.text =
                    getString(R.string.artist_title, musicEntry.artist.name, musicEntry.name)

                infoExtraVM.tracksVM.setInput(musicEntryLoaderInput.copy(type = Stuff.TYPE_TRACKS))

                collectLatestLifecycleFlow(
                    combine(
                        infoExtraVM.tracksVM.hasLoaded,
                        trackFeaturesVM.hasLoaded
                    ) { tracksLoaded, featuresLoaded ->
                        tracksLoaded && featuresLoaded
                    }
                ) {
                    if (!it)
                        binding.root.startFadeLoop()
                    else
                        binding.root.clearAnimation()
                }
            }

            else -> {
                throw IllegalArgumentException("MusicEntry must be either Artist or Track")
            }
        }

        return binding.root
    }

    private fun initSection(
        rootViewBinding: FrameChartsListBinding,
        sectionVM: InfoExtraFullVM,
        showArtists: Boolean
    ) {
        val adapter = ChartsOverviewAdapter(viewLifecycleOwner, rootViewBinding)
        adapter.clickListener = this
        adapter.emptyTextRes = R.string.not_found
        // top tracks/albums are ordered by listeners and not by play count
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

        adapter.progressVisible(true)

        collectLatestLifecycleFlow(sectionVM.entries.filterNotNull()) {
//            if (sectionVM.hasLoaded.value) {
            sectionVM.reachedEnd = true
            adapter.populate(it, true)
//            }
        }
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        val args = Bundle().putData(entry)
        findNavController().navigate(R.id.infoFragment, args)
    }

    private fun showFullFragment(@IdRes fragmentId: Int, expandedTabIdx: Int) {
        val args = requireArguments().clone() as Bundle
        args.putInt(Stuff.ARG_TAB, expandedTabIdx)

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