package com.arn.scrobble.recents

import android.icu.text.DateFormat
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.databinding.ContentTrackHistoryBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.getData
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setTitle
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class TrackHistoryFragment : Fragment(), MusicEntryItemClickListener {
    private var _binding: ContentTrackHistoryBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<TracksVM>()
    private val mainNotifierViewModel by activityViewModels<MainViewModel>()
    private lateinit var adapter: TrackHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.Y, MaterialSharedAxis.X)

        _binding = ContentTrackHistoryBinding.inflate(inflater, container, false)
        binding.tracksList.setupInsets()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        setTitleWithCount()
    }

    private fun setTitleWithCount(
        count: Int = requireArguments().getData<Track>()!!.userplaycount ?: 0
    ) {
        val formattedCount = count.format()
        val title = if (mainNotifierViewModel.currentUser.isSelf) {
            getString(R.string.my_scrobbles) + ": " + formattedCount
        } else {
            val username = mainNotifierViewModel.currentUser.name
            "$username: $formattedCount"
        }
        setTitle(title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainPrefs = PlatformStuff.mainPrefs

        // todo make async
        val showScrobbleSources =
            runBlocking { mainPrefs.data.map { it.showScrobbleSources }.first() }
        val showAlbumInRecents =
            runBlocking { mainPrefs.data.map { it.showAlbumsInRecents }.first() }

        val isShowingPlayers =
            mainNotifierViewModel.currentUser.isSelf && Stuff.billingRepository.isLicenseValid && showScrobbleSources

        adapter = TrackHistoryAdapter(
            viewModel,
            this,
            showAlbumInRecents,
            isShowingPlayers,
        )

        val llm = LinearLayoutManager(requireContext())
        binding.tracksList.layoutManager = llm
        binding.tracksList.adapter = adapter
        (binding.tracksList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false

        val loadMoreListener = EndlessRecyclerViewScrollListener(llm) { page ->
            if (page <= viewModel.totalPages)
                viewModel.setInput(viewModel.input.value!!.copy(page = page))
            else
                adapter.loadMoreListener.isAllPagesLoaded = true
        }
        loadMoreListener.currentPage = viewModel.input.value?.page ?: 1
        adapter.loadMoreListener = loadMoreListener

        binding.tracksList.addOnScrollListener(loadMoreListener)
        binding.tracksList.addItemDecoration(SimpleHeaderDecoration())

        val skeleton = binding.tracksList.createSkeletonWithFade(
            R.layout.list_item_recents_skeleton,
        )

        collectLatestLifecycleFlow(viewModel.tracks.filterNotNull()) {
            adapter.loadMoreListener.loading = false
//            if (oldList.isNotEmpty())
//                adapter.notifyItemChanged(oldList.size - 1) // for the footer to redraw
            adapter.submitList(it)

            binding.tracksList.isVisible = it.isNotEmpty()

        }

//        binding.firstScrobbledOn.visibility = View.GONE

        collectLatestLifecycleFlow(viewModel.firstScrobbledTime.filterNotNull()) {
            val millis = it
            val dateStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT,
                ).format(millis)
            else
                Stuff.myRelativeTime(
                    requireContext(),
                    it
                ) // this might be grammatically wrong for < 24h
            binding.firstScrobbledOn.visibility = View.VISIBLE
            binding.firstScrobbledOn.text = getString(R.string.first_scrobbled_on, dateStr)
        }

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            if (it) {
                skeleton.showOriginal()
            } else if (viewModel.input.value?.page == 1) {
                skeleton.showSkeleton()
            }
        }

        collectLatestLifecycleFlow(mainNotifierViewModel.editData) {
            viewModel.editTrack(it)
        }

        val trackInput = requireArguments().getData<Track>()!!

        setTitleWithCount()

        viewModel.setInput(
            MusicEntryLoaderInput(
                user = mainNotifierViewModel.currentUser,
                timePeriod = null,
                type = Stuff.TYPE_TRACKS,
                entry = trackInput,
                page = 1,
            ), true
        )
    }

    override fun onItemClick(view: View, entry: MusicEntry) {
        when (view.id) {
            R.id.recents_menu -> openTrackPopupMenu(view, entry)
        }
    }

    private fun openTrackPopupMenu(anchor: View, entry: MusicEntry) {
        val track = entry as Track

        val popup = PopupMenu(requireContext(), anchor)
        val menuRes = if (mainNotifierViewModel.currentUser.isSelf)
            R.menu.recents_item_menu
        else
            R.menu.recents_item_friends_menu
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.menu.removeItem(R.id.menu_love)
        val moreMenu: Menu = popup.menu.findItem(R.id.menu_more)?.subMenu ?: popup.menu

        moreMenu.removeItem(R.id.menu_hate)
        moreMenu.removeItem(R.id.menu_share)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> PopupMenuUtils.editScrobble(findNavController(), track)
                R.id.menu_delete -> {
                    if (!Stuff.isOnline)
                        requireActivity().toast(R.string.unavailable_offline)
                    else {
                        viewModel.removeTrack(track)
                        PopupMenuUtils.deleteScrobble(
                            findNavController(),
                            viewModel.viewModelScope,
                            track
                        ) { succ ->
                            if (!succ)
                                requireActivity().toast(R.string.network_error)
                        }
                    }
                }

                R.id.menu_play -> {
                    val pkgName = if (track.date != null)
                        viewModel.pkgMap[track.date]
                    else if (track.isNowPlaying)
                        viewModel.pkgMap[0]
                    else
                        null
                    Stuff.launchSearchIntent(track, pkgName)
                }

                R.id.menu_block_track, R.id.menu_block_album, R.id.menu_block_artist -> {
                    val blockedMetadata = when (menuItem.itemId) {
                        R.id.menu_block_track -> BlockedMetadata(
                            artist = track.artist.name,
                            album = track.album?.name ?: "",
                            track = track.name,
                            skip = true
                        )

                        R.id.menu_block_album -> BlockedMetadata(
                            artist = track.artist.name,
                            album = track.album?.name ?: "",
                            skip = true
                        )

                        R.id.menu_block_artist -> BlockedMetadata(
                            artist = track.artist.name,
                            skip = true
                        )

                        else -> return@setOnMenuItemClickListener false
                    }

                    val args = Bundle().putSingle(blockedMetadata)
                    findNavController().navigate(R.id.blockedMetadataAddDialogFragment, args)
                }
            }
            true
        }

        popup.showWithIcons()
    }
}