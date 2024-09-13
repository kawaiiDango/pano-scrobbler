package com.arn.scrobble.charts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentRandomBinding
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.putData
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlin.random.Random


/**
 * Created by arn on 06/09/2017.
 */
class RandomFragment : ChartsPeriodFragment() {

    override val viewModel by viewModels<RandomVM>()
    private var _binding: ContentRandomBinding? = null
    private val binding get() = _binding!!

    override val periodChipsBinding: ChipsChartsPeriodBinding
        get() = _periodChipsBinding!!
    private var _periodChipsBinding: ChipsChartsPeriodBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        _binding = ContentRandomBinding.inflate(inflater, container, false)
        _periodChipsBinding = binding.chipsChartsPeriod
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        _periodChipsBinding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        postInit()

        if (!activityViewModel.currentUser.isSelf) {
            UiUtils.loadSmallUserPic(
                requireContext(),
                activityViewModel.currentUser,
                activityViewModel.drawerData.value,
            ) {
                binding.randomType.iconTint = null
                binding.randomType.icon = it
            }
        }

        binding.randomType.setOnClickListener {
            PopupMenu(requireContext(), it).apply {
                inflate(R.menu.random_type_menu)
                setOnMenuItemClickListener { item ->
                    val type = when (item.itemId) {
                        R.id.type_track -> Stuff.TYPE_TRACKS
                        R.id.type_loved -> Stuff.TYPE_LOVES
                        R.id.type_album -> Stuff.TYPE_ALBUMS
                        R.id.type_artist -> Stuff.TYPE_ARTISTS
                        else -> return@setOnMenuItemClickListener false
                    }

                    load(type, true)
                    true
                }
            }.showWithIcons()
        }

        collectLatestLifecycleFlow(viewModel.musicEntry.filterNotNull()) {
            setData(it)
        }

        collectLatestLifecycleFlow(viewModel.error) {
            TransitionManager.beginDelayedTransition(
                binding.root,
                MaterialSharedAxis(MaterialSharedAxis.Z, true)
            )

            if (it == null) {
                binding.randomStatus.isVisible = false
                binding.randomItemGroup.visibility = View.VISIBLE
                return@collectLatestLifecycleFlow
            } else {
                binding.randomStatus.text = getString(R.string.charts_no_data)
                binding.randomItemGroup.visibility = View.INVISIBLE
                binding.randomStatus.isVisible = true
                delay(10)
                binding.randomBigImg.load(null)
            }
        }

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            if (it) {
                val (stringRes, iconRes) = when (viewModel.input.value!!.type) {
                    Stuff.TYPE_TRACKS -> R.string.track to R.drawable.vd_note
                    Stuff.TYPE_LOVES -> R.string.loved to R.drawable.vd_heart
                    Stuff.TYPE_ALBUMS -> R.string.album to R.drawable.vd_album
                    Stuff.TYPE_ARTISTS -> R.string.artist to R.drawable.vd_mic
                    else -> throw IllegalArgumentException("Unknown type")
                }
                binding.randomType.text = getString(stringRes) + " ▾"

                if (activityViewModel.currentUser.isSelf)
                    binding.randomType.setIconResource(iconRes)

                if (viewModel.error.value == null) {
                    TransitionManager.beginDelayedTransition(
                        binding.root,
                        MaterialSharedAxis(MaterialSharedAxis.Z, false)
                    )
                    binding.randomItemGroup.visibility = View.VISIBLE
                }
            } else {
                binding.randomStatus.isVisible = false
                binding.randomItemGroup.visibility = View.INVISIBLE
                binding.randomBigImg.load(R.drawable.avd_loading)
            }
        }
    }

    override fun loadFirstPage(networkOnly: Boolean) {
        _binding ?: return

        val type = viewModel.input.value?.type ?: Stuff.TYPE_TRACKS
        load(type)
    }

    private fun load(type: Int, force: Boolean = false) {
        periodChipsBinding.root.isInvisible = type == Stuff.TYPE_LOVES

        if (!Stuff.isOnline) {
            binding.randomStatus.text = getString(R.string.unavailable_offline)
            binding.randomStatus.isVisible = true
        } else {
            viewModel.setInput(
                MusicEntryLoaderInput(
                    user = activityViewModel.currentUser,
                    timePeriod = null,
                    type = type,
                    cacheBuster = if (force)
                        Random.nextInt()
                    else viewModel.input.value?.cacheBuster ?: 0
                )
            )
        }
    }

    private fun setData(musicEntry: MusicEntry) {
        binding.itemName.text = musicEntry.name

        val imageReq: MusicEntryImageReq

        when (musicEntry) {
            is Track -> {
                binding.itemArtist.visibility = View.VISIBLE
                binding.itemArtist.text = musicEntry.artist.name

                imageReq = MusicEntryImageReq(musicEntry, true)
            }

            is Album -> {
                binding.itemArtist.visibility = View.VISIBLE
                binding.itemArtist.text = musicEntry.artist!!.name

                imageReq = MusicEntryImageReq(
                    musicEntry,
                    true,
                    fetchAlbumInfoIfMissing = musicEntry.webp300 == null
                )
            }

            is Artist -> {
                binding.itemArtist.visibility = View.GONE

                imageReq = MusicEntryImageReq(musicEntry)
            }
        }

        binding.randomItem.setOnClickListener {
            if (musicEntry.url != null) {
                val args = Bundle().putData(musicEntry)
                findNavController().navigate(R.id.infoFragment, args)
            }
        }

        val count = musicEntry.userplaycount ?: musicEntry.playcount ?: -1

        if (count > 0) {
            binding.trackCount.isVisible = true

            var scrobblesCount = resources.getQuantityString(
                R.plurals.num_scrobbles_noti,
                count,
                count.format()
            )

            if (musicEntry is Artist || musicEntry is Album)
                scrobblesCount += " • " + viewModel.selectedPeriod.value?.name
            else if (musicEntry is Track && musicEntry.date?.takeIf { it > 0 } != null)
                scrobblesCount += " • " + Stuff.myRelativeTime(requireContext(), musicEntry.date)

            binding.trackCount.text = scrobblesCount
        } else {
            binding.trackCount.isVisible = false
        }

        binding.randomPlay.setOnClickListener {
            Stuff.launchSearchIntent(musicEntry, null)
        }

        binding.randomBigImg.load(imageReq) {
            placeholder(R.drawable.avd_loading)
            error(R.drawable.vd_wave_simple_filled)
        }
    }
}