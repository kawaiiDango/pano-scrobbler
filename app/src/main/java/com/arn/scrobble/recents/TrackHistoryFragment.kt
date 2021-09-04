package com.arn.scrobble.recents

import android.annotation.SuppressLint
import android.icu.text.DateFormat
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.autoNotify
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.databinding.ContentTrackHistoryBinding
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.SimpleHeaderDecoration
import de.umass.lastfm.Track
import java.text.NumberFormat

class TrackHistoryFragment: Fragment(), ItemClickListener {
    private var _binding: ContentTrackHistoryBinding? = null
    private val binding
        get() = _binding!!
    private val username: String?
        get() = arguments?.getString(Stuff.ARG_USERNAME)
    private val scrobbleCount: Int
        get() = arguments!!.getInt(Stuff.ARG_COUNT)
    private val viewModel by lazy { VMFactory.getVM(this, TracksVM::class.java) }
    private lateinit var adapter: TrackHistoryAdapter
    private val argToTrack by lazy {
        Track(
            arguments!!.getString(NLService.B_TRACK),
            null,
            arguments!!.getString(NLService.B_ARTIST)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentTrackHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        setTitle()
    }

    private fun setTitle() {
        val formattedCount = NumberFormat.getInstance().format(scrobbleCount)
        val title = if (username == null)
            getString(R.string.my_scrobbles) + ": " + formattedCount
        else
            "$username: $formattedCount"
        Stuff.setTitle(activity, title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.username = username

        viewModel.tracksReceiver.observe(viewLifecycleOwner) {
            it ?: return@observe
            viewModel.totalPages = it.totalPages
            if (it.page == 1) {
                viewModel.tracks.clear()
                viewModel.loadFirstScrobbleDate(it)
            }
            val oldList = viewModel.tracks.toList()
            viewModel.tracks.addAll(it.pageResults)
            viewModel.tracksReceiver.value = null
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
                    Stuff.myRelativeTime(context!!, it) // this might be grammatically wrong for < 24h
                binding.firstScrobbledOn.visibility = View.VISIBLE
                binding.firstScrobbledOn.text = getString(R.string.first_scrobbled_on, dateStr)
            }
        else
            binding.firstScrobbledOn.visibility = View.GONE

        (activity as Main).mainNotifierViewModel.editData.observe(viewLifecycleOwner) {
            it?.let {
                adapter.editTrack(it)
                (activity as? Main)?.mainNotifierViewModel?.editData?.value = null
            }
        }

        adapter = TrackHistoryAdapter(
            viewModel,
            this,
            MultiPreferences(context!!).getBoolean(Stuff.PREF_SHOW_RECENTS_ALBUM, false)
        )

        val llm = LinearLayoutManager(context!!)
        binding.tracksList.layoutManager = llm
        binding.tracksList.adapter = adapter
        (binding.tracksList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(llm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                if (page <= viewModel.totalPages)
                    viewModel.loadTrackScrobbles(argToTrack, page)
                else
                    isAllPagesLoaded = true
            }
        }
        loadMoreListener.currentPage = viewModel.page
        adapter.loadMoreListener = loadMoreListener

        binding.tracksList.addOnScrollListener(loadMoreListener)
        binding.tracksList.addItemDecoration(SimpleHeaderDecoration(0, 25.dp))

        binding.progress.show()

        if (viewModel.tracks.isEmpty()) {
            viewModel.loadTrackScrobbles(argToTrack, 1)
        } else {
            populate(listOf())
        }
    }

    private fun populate(oldList: List<Track>) {
        binding.progress.hide()
        adapter.loadMoreListener.loading = false
        binding.tracksList.visibility = View.VISIBLE
        if (oldList.isNotEmpty())
            adapter.notifyItemChanged(oldList.size - 1) // for the footer to redraw
        adapter.autoNotify(oldList, viewModel.tracks) { o, n ->
            o.playedWhen.time == n.playedWhen.time
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

    @SuppressLint("RestrictedApi")
    private fun openTrackPopupMenu(anchor: View, position: Int) {
        val track = adapter.getItem(position)

        val menuBuilder = MenuBuilder(context)
        val inflater = SupportMenuInflater(context)
        inflater.inflate(R.menu.recents_item_menu, menuBuilder)

        menuBuilder.removeItem(R.id.menu_love)

        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.menu_edit -> PopupMenuUtils.editScrobble(activity!!, track)
                    R.id.menu_delete -> PopupMenuUtils.deleteScrobble(activity!!, track) { succ ->
                        if (succ) {
                            val wasInList = viewModel.tracks.remove(track)
                            if (wasInList) {
                                adapter.notifyItemRemoved(position)
                                arguments!!.putInt(
                                    Stuff.ARG_COUNT,
                                    scrobbleCount - 1
                                )
                                setTitle()
                            }
                        }
                    }
                    else -> return false
                }
                return true
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })

        val popupMenu = MenuPopupHelper(context!!, menuBuilder, anchor)
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }
}