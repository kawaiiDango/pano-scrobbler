package com.arn.scrobble.recents

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import coil.size.Scale
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentRecentsBinding
import com.arn.scrobble.databinding.HeaderDefaultBinding
import com.arn.scrobble.databinding.HeaderPendingBinding
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.pending.PendingListData
import com.arn.scrobble.pending.PendingScrFragment
import com.arn.scrobble.pending.VHPendingLove
import com.arn.scrobble.pending.VHPendingScrobble
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.ItemLongClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.getTintedDrawable
import com.arn.scrobble.ui.VHHeader
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Created by arn on 10/07/2017.
 */

class RecentsAdapter(
    private val fragmentBinding: ContentRecentsBinding
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), LoadMoreGetter {

    lateinit var itemClickListener: ItemClickListener
    lateinit var itemLongClickListener: ItemLongClickListener
    lateinit var focusChangeListener: FocusChangeListener
    lateinit var setHeroListener: SetHeroTrigger
    private val sectionHeaders = mutableMapOf<Int, String>()
    private val psMap = mutableMapOf<Int, PendingScrobble>()
    private val plMap = mutableMapOf<Int, PendingLove>()
    private var actionHeaderPos = -1
    private var actionData = -1
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    private var fm: FragmentManager? = null
    private val myUpdateCallback = MyUpdateCallback(this)
    lateinit var viewModel: TracksVM
    lateinit var trackBundleLd: LiveData<Bundle>
    var isShowingLoves = false
    var isShowingAlbums = false
    var isShowingPlayers = false
    private var lastPopulateTime = System.currentTimeMillis()
    private val nonTrackViewCount: Int
        get() = sectionHeaders.size + psMap.size + plMap.size +
                if (actionHeaderPos == -1) 0 else 1
    private val playerDao = PanoDb.getDb(fragmentBinding.root.context).getScrobbleSourcesDao()

    init {
//        setHasStableIds(true) //causes some opengl OOM and new holders to be created for no reason
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TRACK -> VHTrack(ListItemRecentsBinding.inflate(inflater, parent, false))
            TYPE_PENDING_SCROBBLE -> VHPendingScrobble(
                ListItemRecentsBinding.inflate(
                    inflater,
                    parent,
                    false
                ), isShowingAlbums, itemClickListener
            )
            TYPE_PENDING_LOVE -> VHPendingLove(
                ListItemRecentsBinding.inflate(
                    inflater,
                    parent,
                    false
                ), isShowingAlbums, itemClickListener
            )
            TYPE_HEADER -> VHHeader(HeaderDefaultBinding.inflate(inflater, parent, false))
            TYPE_ACTION -> VHAction(HeaderPendingBinding.inflate(inflater, parent, false), fm)
            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VHTrack -> holder.setItemData(viewModel.tracks[position - nonTrackViewCount])
            is VHPendingScrobble -> holder.setItemData(psMap[position] ?: return)
            is VHPendingLove -> holder.setItemData(plMap[position] ?: return)
            is VHHeader -> holder.setHeaderText(sectionHeaders[position] ?: "...")
            is VHAction -> holder.setItemData(actionData)
            else -> throw RuntimeException("Invalid view type $holder")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == actionHeaderPos -> TYPE_ACTION
            psMap.containsKey(position) -> TYPE_PENDING_SCROBBLE
            plMap.containsKey(position) -> TYPE_PENDING_LOVE
            position >= nonTrackViewCount -> TYPE_TRACK
            else -> TYPE_HEADER
        }
    }

    fun setLoading(b: Boolean) {
        loadMoreListener.loading = b
        fragmentBinding.recentsSwipeRefresh.isRefreshing = false
    }

    fun getItem(pos: Int): Any? {
        return when {
            getItemViewType(pos) == TYPE_TRACK -> viewModel.tracks[pos - nonTrackViewCount]
            getItemViewType(pos) == TYPE_PENDING_SCROBBLE -> psMap[pos]
            getItemViewType(pos) == TYPE_PENDING_LOVE -> plMap[pos]
            else -> null
        }
    }

    fun removeTrack(track: Track) {
        val idx = viewModel.tracks.indexOfFirst {
            it.playedWhen == track.playedWhen &&
                    it.name == track.name && it.artist == track.artist
        }
        if (idx != -1) {
            viewModel.deletedTracksStringSet += track.toString()
            synchronized(viewModel.tracks) {
                viewModel.tracks.removeAt(idx)
            }
            notifyItemRemoved(idx + nonTrackViewCount)
        }
    }

    fun editTrack(track: Track) {
        val idx = viewModel.tracks.indexOfFirst { it.playedWhen == track.playedWhen }
        if (idx != -1) {
            val prevTrack = viewModel.tracks[idx]
            if (prevTrack.artist == track.artist && prevTrack.album == track.album && prevTrack.name == track.name)
                return
            viewModel.deletedTracksStringSet += prevTrack.toString()
            viewModel.tracks[idx] = track
            notifyItemChanged(idx + nonTrackViewCount)
        }
    }

    override fun getItemCount() = viewModel.tracks.size + nonTrackViewCount

    fun setPending(fm: FragmentManager?, pendingListData: PendingListData) {
        val headerText = fragmentBinding.root.context.getString(R.string.pending_scrobbles)
        var shift = 0
        val lastDisplaySize = psMap.size + plMap.size
        var oldNonTrackViewCount = nonTrackViewCount
        val totalCount = pendingListData.psCount + pendingListData.plCount
        var displayCount = 0
        if (pendingListData.psCount > 0)
            displayCount = pendingListData.psList.size
        if (pendingListData.plCount > 0)
            displayCount += pendingListData.plList.size

        actionHeaderPos = -1
        if (totalCount == 0) {
            this.fm = null
            if (sectionHeaders.containsValue(headerText)) {
                val lastval = sectionHeaders[nonTrackViewCount - 1] ?: return
                sectionHeaders.clear()
                sectionHeaders[0] = lastval
            }
        } else {
            if (sectionHeaders.isEmpty()) {
                setStatusHeader()
                oldNonTrackViewCount = nonTrackViewCount
            }
            this.fm = fm
            if (totalCount < 3) {
                shift = 1
                if (sectionHeaders[shift + displayCount] == null)
                    sectionHeaders[shift + displayCount] =
                        sectionHeaders[sectionHeaders.keys.last()]!!
            } else {
                shift = 2
                actionHeaderPos = 3
                actionData = totalCount - displayCount
            }
            sectionHeaders[shift + displayCount] = sectionHeaders[sectionHeaders.keys.last()]!!
            sectionHeaders[0] = headerText
            sectionHeaders.keys.toIntArray().forEach {
                if (it != 0 && it != shift + displayCount)
                    sectionHeaders.remove(it)
            }
        }
        psMap.clear()
        plMap.clear()
        if (pendingListData.plCount > 0)
            pendingListData.plList.forEachIndexed { i, pl ->
                plMap[1 + i] = pl
            }
        if (pendingListData.psCount > 0)
            pendingListData.psList.forEachIndexed { i, ps ->
                psMap[1 + plMap.size + i] = ps
            }
        viewModel.selectedPos += nonTrackViewCount - oldNonTrackViewCount
        if (lastDisplaySize == displayCount)
            notifyItemRangeChanged(0, nonTrackViewCount, 0)
        else
            notifyDataSetChanged()
    }

    fun setStatusHeader() {
        val username = if (viewModel.username != null)
            " • " + viewModel.username
        else
            ""
        val header = if (isShowingLoves)
            fragmentBinding.root.context.getString(R.string.recently_loved) + username
        else if (viewModel.toTime != null)
             fragmentBinding.root.context.getString(
                R.string.scrobbles_till,
                DateFormat.getMediumDateFormat(fragmentBinding.root.context)
                    .format(viewModel.toTime)
            ) + username
        else
            fragmentBinding.root.context.getString(R.string.recently_scrobbled) + username
        setStatusHeader(header)
    }

    fun setStatusHeader(s: String) {
        var idx = nonTrackViewCount
        if (idx == 0)
            idx++
        if (sectionHeaders[idx - 1] != s) {
            sectionHeaders[idx - 1] = s
            notifyItemChanged(idx - 1, 0)
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position < nonTrackViewCount)
            position.toLong()
        else if (position < viewModel.tracks.size)
            viewModel.tracks[position - nonTrackViewCount].playedWhen?.time ?: Stuff.NP_ID.toLong()
        else
            Stuff.NP_ID.toLong()
    }

    fun populate(oldTracks: MutableList<Track>) {
        if (Stuff.isOnline)
            setStatusHeader()
        else
            setStatusHeader(fragmentBinding.root.context.getString(R.string.offline))

        val selectedId = getItemId(viewModel.selectedPos)
        var selectedPos = nonTrackViewCount
        if (viewModel.tracks.isEmpty()) {
            setStatusHeader(fragmentBinding.root.context.getString(R.string.no_scrobbles))
        }
        if (selectedPos >= nonTrackViewCount)
            for (i in 0 until viewModel.tracks.size) {
                if (viewModel.tracks[i].playedWhen?.time == selectedId) {
                    selectedPos = i + nonTrackViewCount
                    break
                }
            }

        viewModel.selectedPos = selectedPos

        myUpdateCallback.offset = nonTrackViewCount
        myUpdateCallback.selectedPos = viewModel.selectedPos
        val diff = DiffUtil.calculateDiff(
            DiffCallback(viewModel.tracks, oldTracks, lastPopulateTime),
            false
        )
        diff.dispatchUpdatesTo(myUpdateCallback)
        if (oldTracks.isEmpty() && viewModel.tracks.isNotEmpty() || fragmentBinding.recentsSwipeRefresh.isRefreshing) {
            fragmentBinding.recentsList.scheduleLayoutAnimation()
            if (isShowingLoves)
                notifyItemChanged(0, 0) //animation gets delayed otherwise
        }
        setLoading(false)

        lastPopulateTime = System.currentTimeMillis()
    }

    class DiffCallback(
        private val newList: List<Track>,
        private val oldList: List<Track>,
        private val lastPopulateTime: Long
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].playedWhen?.time == newList[newItemPosition].playedWhen?.time
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return System.currentTimeMillis() - lastPopulateTime < 60 * 60 * 1000 && // populate time
                    oldList[oldItemPosition].name == newList[newItemPosition].name &&
                    oldList[oldItemPosition].album == newList[newItemPosition].album &&
                    oldList[oldItemPosition].artist == newList[newItemPosition].artist &&
                    oldList[oldItemPosition].isLoved == newList[newItemPosition].isLoved &&
                    oldList[oldItemPosition].getImageURL(ImageSize.MEDIUM) == newList[newItemPosition].getImageURL(
                ImageSize.MEDIUM
            )
        }
    }

    class MyUpdateCallback(private val adapter: RecyclerView.Adapter<*>) : ListUpdateCallback {
        var offset = 0
        var selectedPos = 0

        override fun onInserted(position: Int, count: Int) {
            if (position > offset)
                adapter.notifyItemChanged(position - 1 + offset, 0)
            adapter.notifyItemChanged(selectedPos, 0)
            adapter.notifyItemRangeInserted(position + offset, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapter.notifyItemRangeRemoved(position + offset, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapter.notifyItemMoved(fromPosition + offset, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter.notifyItemRangeChanged(position + offset, count, payload)
        }
    }

    class VHAction(private val binding: HeaderPendingBinding, fm: FragmentManager?) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            if (fm != null)
                binding.root.setOnClickListener {
                    fm.beginTransaction()
                        .replace(R.id.frame, PendingScrFragment())
                        .addToBackStack(null)
                        .commit()
                }
        }

        fun setItemData(n: Int) {
            binding.pendingAction.text = itemView.context.getString(R.string.show_all)
            binding.pendingText.text = itemView.context.getString(R.string.n_more, n)
        }
    }

    inner class VHTrack(private val binding: ListItemRecentsBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnFocusChangeListener {
        private var job: Job? = null

        init {
            binding.root.setOnClickListener {
                itemClickListener.call(itemView, bindingAdapterPosition)
            }
            binding.root.setOnLongClickListener {
                itemLongClickListener.call(itemView, bindingAdapterPosition)
                true
            }
            binding.root.onFocusChangeListener = this
            if (viewModel.username != null && !Stuff.isTv) {
                binding.recentsMenu.visibility = View.INVISIBLE
                binding.recentsMenuText.visibility = View.GONE
            } else
                binding.recentsMenu.setOnClickListener {
                    itemClickListener.call(it, bindingAdapterPosition)
                }
        }

        override fun onFocusChange(view: View?, focused: Boolean) {
            if (view != null && !view.isInTouchMode && focused)
                focusChangeListener.call(itemView, bindingAdapterPosition)
        }

        private fun setSelected(
            selected: Boolean,
            track: Track = viewModel.tracks[viewModel.selectedPos - nonTrackViewCount]
        ) {
            itemView.isActivated = selected
            if (selected)
                setHeroListener.onSetHero(bindingAdapterPosition, track, false)
        }

        private fun setPlayerIcon(track: Track) {
            val timeMillis = track.playedWhen?.time
            binding.playerIcon.visibility = View.VISIBLE

            fun fetchIcon(pkgName: String) {
                binding.playerIcon.load(PackageName(pkgName)) {
                    scale(Scale.FIT)
                    listener(onSuccess = { _, _ ->
                        binding.playerIcon.contentDescription = pkgName
                    })
                }
            }

//            if (track.isNowPlaying && trackBundleLd.value?.getBoolean(NLService.B_IS_SCROBBLING) == true) {
//                trackBundleLd.value?.getString(NLService.B_PACKAGE_NAME)?.let {
//                    viewModel.pkgMap[0] = it
//                    fetchIcon(it)
//                }
//            } else
                if (timeMillis != null && viewModel.pkgMap[timeMillis] != null) {
                fetchIcon(viewModel.pkgMap[timeMillis]!!)
            } else {
                binding.playerIcon.dispose()
                binding.playerIcon.load(null)
                binding.playerIcon.contentDescription = null
                job?.cancel()

                if (timeMillis != null) {
                    job = viewModel.viewModelScope.launch(Dispatchers.IO) {
                        delay(100)
                        playerDao.findPlayer(timeMillis)?.pkg?.let { pkgName ->
                            viewModel.pkgMap[timeMillis] = pkgName
                            fetchIcon(pkgName)
                        }
                    }
                }
            }
        }

        fun setItemData(track: Track) {
            binding.recentsTitle.text = track.name
            binding.recentsSubtitle.text = track.artist

            if (isShowingAlbums) {
                if (!track.album.isNullOrEmpty()) {
                    binding.recentsAlbum.text = track.album
                    binding.recentsAlbum.visibility = View.VISIBLE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        0,
                        0,
                        0
                    )
                } else {
                    val albumHeight =
                        itemView.context.resources.getDimension(R.dimen.album_text_height).toInt()
                    binding.recentsAlbum.visibility = View.GONE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        albumHeight / 2,
                        0,
                        albumHeight / 2
                    )
                }
            }

            if (track.isNowPlaying) {
                binding.recentsDate.visibility = View.GONE
                UiUtils.nowPlayingAnim(binding.recentsPlaying, true)
            } else {
                binding.recentsDate.visibility = View.VISIBLE
                binding.recentsDate.text =
                    Stuff.myRelativeTime(itemView.context, track.playedWhen?.time ?: 0)
                UiUtils.nowPlayingAnim(binding.recentsPlaying, false)
            }

            if (track.isLoved) {
                if (binding.recentsImgOverlay.background == null)
                    binding.recentsImgOverlay.background = ContextCompat.getDrawable(
                        binding.recentsImgOverlay.context,
                        R.drawable.vd_heart_stroked
                    )
                binding.recentsImgOverlay.visibility = View.VISIBLE
            } else {
                binding.recentsImgOverlay.visibility = View.INVISIBLE
            }

            if (isShowingPlayers) {
                setPlayerIcon(track)
            }

            val imgUrl = track.getWebpImageURL(ImageSize.LARGE)
            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                Stuff.genHashCode(track.artist, track.name)
            )

            if (!isShowingLoves) {
                binding.recentsImg.load(imgUrl ?: "") {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
                }
            } else {
                val musicEntryImageReq = MusicEntryImageReq(track, ImageSize.LARGE, true)
                binding.recentsImg.load(musicEntryImageReq) {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
//                    transitionFactory { _, _ -> NoCrossfadeOnErrorTransition() }

                    listener(onSuccess = { request, _ ->
                        (request.data as? String)?.let {
                            if (bindingAdapterPosition == viewModel.selectedPos) {
                                val idx = bindingAdapterPosition - nonTrackViewCount
                                setHeroListener.onSetHero(
                                    bindingAdapterPosition,
                                    viewModel.tracks[idx],
                                    false
                                )
                            }
                        }
                    })
                }
            }
            setSelected(bindingAdapterPosition == viewModel.selectedPos, track)
        }
    }

    interface SetHeroTrigger {
        fun onSetHero(position: Int, track: Track, fullSize: Boolean)
    }
}

private const val TYPE_TRACK = 0
private const val TYPE_PENDING_SCROBBLE = 1
private const val TYPE_PENDING_LOVE = 2
private const val TYPE_HEADER = 3
private const val TYPE_ACTION = 4