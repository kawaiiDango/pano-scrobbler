package com.arn.scrobble.recents

import android.icu.text.DateFormat
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.arn.scrobble.App
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.databinding.ContentTrackHistoryBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.getData
import com.arn.scrobble.utils.Stuff.putSingle
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.filterNotNull

class TrackHistoryFragment : Fragment(), MusicEntryItemClickListener {
    private var _binding: ContentTrackHistoryBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<TracksVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private lateinit var adapter: TrackHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.Y, MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        val prefs = App.prefs
        val isShowingPlayers =
            mainNotifierViewModel.currentUser.isSelf && prefs.proStatus && prefs.showScrobbleSources

        adapter = TrackHistoryAdapter(
            viewModel,
            this,
            prefs.showAlbumInRecents,
            isShowingPlayers,
            mainNotifierViewModel.currentUser.isSelf
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
            } else {
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
        val dateFrame = (view.parent as ViewGroup).findViewById<FrameLayout>(R.id.date_frame)
        when (view.id) {
            R.id.recents_menu -> openTrackPopupMenu(dateFrame, entry)
            else -> {
                if (!view.isInTouchMode)
                    openTrackPopupMenu(dateFrame, entry)
            }
        }
    }

    private fun openTrackPopupMenu(anchor: View, entry: MusicEntry) {
        val track = entry as Track

        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.recents_item_menu, popup.menu)

        popup.menu.removeItem(R.id.menu_love)
        val moreMenu: Menu = popup.menu.findItem(R.id.menu_more)?.subMenu ?: popup.menu

        moreMenu.removeItem(R.id.menu_hate)
        moreMenu.removeItem(R.id.menu_share)
        moreMenu.removeItem(R.id.menu_info)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> PopupMenuUtils.editScrobble(findNavController(), track)
                R.id.menu_delete -> PopupMenuUtils.deleteScrobble(
                    findNavController(),
                    viewModel.viewModelScope,
                    track
                ) { succ ->
                    if (succ) {
                        viewModel.removeTrack(track)
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