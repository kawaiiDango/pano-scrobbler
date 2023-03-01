package com.arn.scrobble

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.charts.ChartsPeriodFragment
import com.arn.scrobble.databinding.ChipsChartsPeriodBinding
import com.arn.scrobble.databinding.ContentRandomBinding
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.platform.MaterialSharedAxis
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.ImageSize
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import io.michaelrocks.bimap.HashBiMap
import java.text.NumberFormat


/**
 * Created by arn on 06/09/2017.
 */
class RandomFragment : ChartsPeriodFragment() {

    private val randomViewModel by viewModels<RandomVM>()
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
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
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

        viewModel.username = activityViewModel.currentUser.name
        randomViewModel.username = activityViewModel.currentUser.name

        if (!activityViewModel.userIsSelf) {
            UiUtils.loadSmallUserPic(
                binding.randomizeText.context,
                activityViewModel.currentUser
            ) {
                binding.randomizeText.setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
            }
        }

        if (!BuildConfig.DEBUG)
            periodChipsBinding.root.visibility = View.GONE

        binding.randomScrobbleTypeGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->

            TransitionManager.beginDelayedTransition(group)

            val btn = group.findViewById<MaterialButton>(checkedId)
            if (isChecked) {
                btn.iconPadding = 4.dp
                btn.text = btn.contentDescription
            } else {
                btn.iconPadding = 0
                btn.text = ""
            }

            if (BuildConfig.DEBUG) {
                periodChipsBinding.root.visibility = (isChecked && checkedId == R.id.get_loved)
                    .let { if (it) View.INVISIBLE else View.VISIBLE }
            }
        }

        binding.randomScrobbleTypeGroup.children.forEach {
            it.setOnClickListener {
                val type = buttonToTypeBimap[it.id]!!
                load(type)
            }
        }

        randomViewModel.data.observe(viewLifecycleOwner) {
            it ?: return@observe
            randomViewModel.error.value = null
            randomViewModel.setTotal(it.type, it.total)
            viewModel.totalCount = it.total

            if (it.entry != null) {
                prefs.lastRandomType = it.type
                setData(it.entry)
            } else {
                doneLoading(R.string.charts_no_data)
            }
        }

        randomViewModel.error.observe(viewLifecycleOwner) {
            it ?: return@observe
            it.printStackTrace()
            doneLoading(R.string.network_error)
        }

        val type = arguments?.getInt(Stuff.ARG_TYPE) ?: prefs.lastRandomType

        buttonToTypeBimap.inverse[type]?.let { id ->
            binding.randomScrobbleTypeGroup.check(id)
        }
    }

    override fun loadFirstPage(networkOnly: Boolean) {
        _binding ?: return

        val type = buttonToTypeBimap[binding.randomScrobbleTypeGroup.checkedButtonId]!!
        if (BuildConfig.DEBUG)
            viewModel.selectedPeriod.value?.let { randomViewModel.timePeriod = it }
        load(type)
    }

    private fun load(type: Int) {
        if (randomViewModel.isLoading)
            return
        randomViewModel.loadRandom(type)
        randomViewModel.isLoading = true
        if (!Stuff.isOnline) {
            binding.randomStatus.text = getString(R.string.unavailable_offline)
            binding.randomStatus.visibility = View.VISIBLE
            binding.randomProgress.hide()
            randomViewModel.isLoading = false
        } else {
            binding.randomStatus.visibility = View.GONE
            binding.randomProgress.show()
        }

        TransitionManager.beginDelayedTransition(
            binding.root,
            MaterialSharedAxis(MaterialSharedAxis.Z, false)
        )
        binding.randomContentGroup.visibility = View.INVISIBLE
        binding.randomScrobbleTypeGroup.children.forEach {
            it.isClickable = false
        }
    }

    private fun doneLoading(@StringRes errorMessageRes: Int? = null) {
        randomViewModel.isLoading = false
        binding.randomProgress.hide()

        TransitionManager.beginDelayedTransition(
            binding.root,
            MaterialSharedAxis(MaterialSharedAxis.Z, true)
        )

        binding.randomScrobbleTypeGroup.children.forEach {
            it.isClickable = true
        }

        if (errorMessageRes == null) {
            binding.randomContentGroup.visibility = View.VISIBLE
            binding.randomStatus.visibility = View.GONE
        } else {
            binding.randomStatus.text = getString(errorMessageRes)
            binding.randomContentGroup.visibility = View.INVISIBLE
            binding.randomStatus.visibility = View.VISIBLE
        }
    }

    private fun setData(musicEntry: MusicEntry) {
        doneLoading(null)

        val iconRes = when (musicEntry) {
            is Track -> {
                if (musicEntry.isLoved)
                    R.drawable.vd_heart
                else
                    R.drawable.vd_note
            }

            is Album -> R.drawable.vd_album
            is Artist -> R.drawable.vd_mic
            else -> throw IllegalArgumentException("Unknown music entry type")
        }

        binding.itemName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            iconRes,
            0,
            0,
            0
        )

        binding.itemName.text = musicEntry.name

        when (musicEntry) {
            is Track -> {
                binding.itemArtist.visibility = View.VISIBLE
                binding.itemArtist.text = musicEntry.artist

                val playedWhenTime = musicEntry.playedWhen?.time
                if (playedWhenTime != null && playedWhenTime > 0) {
                    binding.trackDate.visibility = View.VISIBLE
                    binding.trackDate.text = Stuff.myRelativeTime(requireContext(), playedWhenTime)
                } else {
                    binding.trackDate.visibility = View.GONE
                }
            }

            is Album -> {
                binding.itemArtist.visibility = View.VISIBLE
                binding.itemArtist.text = musicEntry.artist

                binding.trackDate.visibility = View.GONE
            }

            is Artist -> {
                binding.itemArtist.visibility = View.GONE
                binding.trackDate.visibility = View.GONE
            }
        }

        binding.randomItem.setOnClickListener {
            if (musicEntry.url != null) {
                val args = musicEntry.toBundle()
                findNavController().navigate(R.id.infoFragment, args)
            }
        }

        val count = if (musicEntry.userPlaycount > 0)
            musicEntry.userPlaycount
        else if (musicEntry.playcount > 0)
            musicEntry.playcount
        else
            -1

        if (count > 0) {
            binding.trackCount.visibility = View.VISIBLE
            binding.trackCount.text = resources.getQuantityString(
                R.plurals.num_scrobbles_noti,
                count,
                NumberFormat.getInstance().format(count)
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
        val imgUrl =
            musicEntry.getWebpImageURL(ImageSize.EXTRALARGE)?.replace("300x300", "600x600") ?: ""

        val imgLoadData: Any = if (randomViewModel.data.value!!.type == Stuff.TYPE_ARTISTS) {
            MusicEntryImageReq(musicEntry, ImageSize.EXTRALARGE, true)
        } else {
            imgUrl
        }

        binding.randomBigImg.load(imgLoadData) {
            allowHardware(false) // because crash on oreo
            placeholder(R.drawable.vd_wave_simple_filled)
            error(R.drawable.vd_wave_simple_filled)
        }
    }
}