package com.arn.scrobble.recents

import android.icu.text.DateFormat
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
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
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.equalsExt
import com.arn.scrobble.databinding.ContentTrackHistoryBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.UiUtils.autoNotify
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.google.android.material.transition.MaterialSharedAxis
import de.umass.lastfm.Track
import java.text.NumberFormat

class TrackHistoryFragment : Fragment(), ItemClickListener {
    private var _binding: ContentTrackHistoryBinding? = null
    private val binding
        get() = _binding!!
    private val scrobbleCount: Int
        get() = requireArguments().getInt(Stuff.ARG_COUNT)
    private val viewModel by viewModels<TracksVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private lateinit var adapter: TrackHistoryAdapter
    private val argToTrack by lazy {
        Track(
            requireArguments().getString(NLService.B_TRACK),
            null,
            requireArguments().getString(NLService.B_ARTIST)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
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

    override fun onStart() {
        super.onStart()
        setTitleWithCount()
    }

    private fun setTitleWithCount() {
        val formattedCount = NumberFormat.getInstance().format(scrobbleCount)
        val title = if (mainNotifierViewModel.userIsSelf) {
            getString(R.string.my_scrobbles) + ": " + formattedCount
        } else {
            val username = mainNotifierViewModel.currentUser.name
            "$username: $formattedCount"
        }
        setTitle(title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!mainNotifierViewModel.userIsSelf)
            viewModel.username = mainNotifierViewModel.currentUser.name

        viewModel.tracksReceiver.observe(viewLifecycleOwner) {
            viewModel.totalPages = it.totalPages
            if (it.page == 1) {
                viewModel.tracks.clear()
                viewModel.loadFirstScrobbleDate(it)
            }
            val oldList = viewModel.tracks.toList()
            viewModel.tracks.addAll(it.pageResults)
            populate(oldList)
        }

        if (scrobbleCount > 1)
            viewModel.firstScrobbledDate.observe(viewLifecycleOwner) {
                it ?: return@observe
                val dateStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT,
                    ).format(it)
                else
                    Stuff.myRelativeTime(
                        requireContext(),
                        it
                    ) // this might be grammatically wrong for < 24h
                binding.firstScrobbledOn.visibility = View.VISIBLE
                binding.firstScrobbledOn.text = getString(R.string.first_scrobbled_on, dateStr)
            }
        else
            binding.firstScrobbledOn.visibility = View.GONE

        mainNotifierViewModel.editData.observe(viewLifecycleOwner) {
            adapter.editTrack(it)
        }

        val prefs = MainPrefs(requireContext())
        val isShowingPlayers =
            mainNotifierViewModel.userIsSelf && prefs.proStatus && prefs.showScrobbleSources

        adapter = TrackHistoryAdapter(
            viewModel,
            this,
            prefs.showAlbumInRecents,
            isShowingPlayers,
            PanoDb.db.getScrobbleSourcesDao()
        )

        val llm = LinearLayoutManager(requireContext())
        binding.tracksList.layoutManager = llm
        binding.tracksList.adapter = adapter
        (binding.tracksList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false

        val loadMoreListener = EndlessRecyclerViewScrollListener(llm) {
            loadTracks(it)
        }
        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener

        binding.tracksList.addOnScrollListener(loadMoreListener)
        binding.tracksList.addItemDecoration(SimpleHeaderDecoration())

        binding.progress.show()

        if (viewModel.tracks.isEmpty()) {
            viewModel.loadTrackScrobbles(argToTrack, 1)
        } else {
            populate(listOf())
        }
    }

    private fun loadTracks(page: Int) {
        if (page <= viewModel.totalPages)
            viewModel.loadTrackScrobbles(argToTrack, page)
        else
            adapter.loadMoreListener.isAllPagesLoaded = true
    }

    private fun populate(oldList: List<Track>) {
        binding.progress.hide()
        adapter.loadMoreListener.loading = false
        binding.tracksList.visibility = View.VISIBLE
        if (oldList.isNotEmpty())
            adapter.notifyItemChanged(oldList.size - 1) // for the footer to redraw
        adapter.autoNotify(oldList, viewModel.tracks) { o, n ->
            o.equalsExt(n)
        }
    }

    override fun onItemClick(view: View, position: Int) {
        val dateFrame = (view.parent as ViewGroup).findViewById<FrameLayout>(R.id.date_frame)
        when (view.id) {
            R.id.recents_menu -> openTrackPopupMenu(dateFrame, position)
            else -> {
                if (!view.isInTouchMode)
                    openTrackPopupMenu(dateFrame, position)
            }
        }
    }

    private fun openTrackPopupMenu(anchor: View, position: Int) {
        val track = adapter.getItem(position)

        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.recents_item_menu, popup.menu)

        popup.menu.removeItem(R.id.menu_love)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> PopupMenuUtils.editScrobble(findNavController(), track)
                R.id.menu_delete -> PopupMenuUtils.deleteScrobble(
                    findNavController(),
                    viewModel.viewModelScope,
                    track
                ) { succ ->
                    if (succ) {
                        val oldList = viewModel.tracks.toList()
                        val wasInList = viewModel.tracks.remove(track)
                        if (wasInList) {
                            adapter.autoNotify(oldList, viewModel.tracks) { o, n ->
                                o.equalsExt(n)
                            }
                            requireArguments().putInt(Stuff.ARG_COUNT, scrobbleCount - 1)
                            setTitleWithCount()
                        }
                    }
                }
            }
            true
        }

        popup.showWithIcons()
    }
}