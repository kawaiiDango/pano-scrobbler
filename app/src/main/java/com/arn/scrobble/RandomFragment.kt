package com.arn.scrobble

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import coil.load
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp600
import com.arn.scrobble.charts.ChartsPeriodFragment
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentRandomBinding
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.putData
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.MaterialSharedAxis
import io.michaelrocks.bimap.HashBiMap
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

    private val buttonToTypeBimap by lazy {
        HashBiMap.create(
            hashMapOf(
                R.id.get_track to Stuff.TYPE_TRACKS,
                R.id.get_loved to Stuff.TYPE_LOVES,
                R.id.get_album to Stuff.TYPE_ALBUMS,
                R.id.get_artist to Stuff.TYPE_ARTISTS
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                binding.randomizeText.context,
                activityViewModel.currentUser
            ) {
                binding.randomizeText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    it,
                    null,
                    null,
                    null
                )
            }
        }

        binding.randomScrobbleTypeGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->

//            TransitionManager.beginDelayedTransition(group)

            val btn = group.findViewById<MaterialButton>(checkedId)
            if (isChecked) {
                btn.iconPadding = 4.dp
                btn.text = btn.contentDescription
            } else {
                btn.iconPadding = 0
                btn.text = ""
            }
        }

        binding.randomScrobbleTypeGroup.children.forEach {
            it.setOnClickListener {
                val type = buttonToTypeBimap[it.id]!!
                load(type, true)
            }
        }

        val skeleton = binding.randomContentGroup.createSkeletonWithFade(binding.randomSkeleton)

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
                binding.randomContentGroup.visibility = View.VISIBLE
                return@collectLatestLifecycleFlow
            } else {
                binding.randomStatus.text = getString(R.string.charts_no_data)
                binding.randomContentGroup.visibility = View.INVISIBLE
                binding.randomStatus.isVisible = true
            }
        }

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            if (it) {
                skeleton.showOriginal()
                if (viewModel.error.value == null)
                    binding.randomContentGroup.visibility = View.VISIBLE
            } else {
                binding.randomStatus.isVisible = false
                binding.randomContentGroup.visibility = View.INVISIBLE
                skeleton.showSkeleton()
            }
        }

        val type = arguments?.getInt(Stuff.ARG_TYPE) ?: App.prefs.lastRandomType

        buttonToTypeBimap.inverse[type]?.let { id ->
            binding.randomScrobbleTypeGroup.check(id)
        }
    }

    override fun loadFirstPage(networkOnly: Boolean) {
        _binding ?: return

        val type = buttonToTypeBimap[binding.randomScrobbleTypeGroup.checkedButtonId] ?: return
        load(type)
    }

    private fun load(type: Int, force: Boolean = false) {
        periodChipsBinding.root.visibility =
            if (type == Stuff.TYPE_LOVES)
                View.INVISIBLE
            else
                View.VISIBLE

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

        TransitionManager.beginDelayedTransition(
            binding.root,
            MaterialSharedAxis(MaterialSharedAxis.Z, false)
        )
    }

    private fun setData(musicEntry: MusicEntry) {

        val iconRes = when (musicEntry) {
            is Track -> {
                if (musicEntry.userloved == true)
                    R.drawable.vd_heart
                else
                    R.drawable.vd_note
            }

            is Album -> R.drawable.vd_album
            is Artist -> R.drawable.vd_mic
        }

        binding.itemName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            iconRes,
            0,
            0,
            0
        )

        binding.itemName.text = musicEntry.name

        val imageReq: Any

        when (musicEntry) {
            is Track -> {
                binding.itemArtist.visibility = View.VISIBLE
                binding.itemArtist.text = musicEntry.artist.name

                val playedWhenTime = musicEntry.date
                if (playedWhenTime != null && playedWhenTime > 0) {
                    binding.trackDate.visibility = View.VISIBLE
                    binding.trackDate.text = Stuff.myRelativeTime(requireContext(), playedWhenTime)
                } else {
                    binding.trackDate.visibility = View.GONE
                }

                imageReq = musicEntry.webp600 ?: ""
            }

            is Album -> {
                binding.itemArtist.visibility = View.VISIBLE
                binding.itemArtist.text = musicEntry.artist!!.name

                binding.trackDate.visibility = View.GONE

                imageReq = musicEntry.webp600 ?: ""
            }

            is Artist -> {
                binding.itemArtist.visibility = View.GONE
                binding.trackDate.visibility = View.GONE

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
            binding.trackCount.visibility = View.VISIBLE
            binding.trackCount.text = resources.getQuantityString(
                R.plurals.num_scrobbles_noti,
                count,
                count.format()
            )
        } else {
            binding.trackCount.visibility = View.GONE
        }

        arrayOf(binding.randomPlay, binding.randomPlayFiller)
            .forEach {
                it.setOnClickListener {
                    Stuff.launchSearchIntent(musicEntry, null)
                }
            }

        binding.randomBigImg.load(imageReq) {
            allowHardware(false) // because crash on oreo
            placeholder(R.drawable.color_image_loading)
            error(R.drawable.vd_wave_simple_filled)
        }
    }
}