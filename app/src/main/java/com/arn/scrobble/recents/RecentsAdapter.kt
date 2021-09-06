package com.arn.scrobble.recents

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentRecentsBinding
import com.arn.scrobble.databinding.HeaderDefaultBinding
import com.arn.scrobble.databinding.HeaderPendingBinding
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.pending.PendingScrFragment
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.pending.PendingListData
import com.arn.scrobble.pending.VHPendingLove
import com.arn.scrobble.pending.VHPendingScrobble
import com.arn.scrobble.ui.*
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import java.lang.ref.WeakReference


/**
 * Created by arn on 10/07/2017.
 */

class RecentsAdapter(
    private val fragmentBinding: ContentRecentsBinding
): RecyclerView.Adapter<RecyclerView.ViewHolder>(), LoadImgInterface, LoadMoreGetter {

    lateinit var itemClickListener: ItemClickListener
    lateinit var itemLongClickListener: ItemLongClickListener
    lateinit var focusChangeListener: FocusChangeListener
    lateinit var setHeroListener: SetHeroTrigger
    private val sectionHeaders = mutableMapOf<Int,String>()
    private val psMap = mutableMapOf<Int, PendingScrobble>()
    private val plMap = mutableMapOf<Int, PendingLove>()
    private var actionHeaderPos = -1
    private var actionData = -1
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    private var fm: FragmentManager? = null
    private val myUpdateCallback = MyUpdateCallback(this)
    lateinit var viewModel: TracksVM
    var isShowingLoves = false
    var isShowingAlbums = false
    private var lastPopulateTime = System.currentTimeMillis()
    val handler by lazy { EntryInfoHandler(WeakReference(this)) }
    private val nonTrackViewCount: Int
        get() = sectionHeaders.size + psMap.size + plMap.size +
                if (actionHeaderPos == -1) 0 else 1
    
    init {
//        setHasStableIds(true) //causes some opengl OOM and new holders to be created for no reason
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TRACK -> VHTrack(ListItemRecentsBinding.inflate(inflater, parent, false))
            TYPE_PENDING_SCROBBLE -> VHPendingScrobble(ListItemRecentsBinding.inflate(inflater, parent, false), isShowingAlbums, itemClickListener)
            TYPE_PENDING_LOVE -> VHPendingLove(ListItemRecentsBinding.inflate(inflater, parent, false), isShowingAlbums, itemClickListener)
            TYPE_HEADER -> VHHeader(HeaderDefaultBinding.inflate(inflater, parent, false))
            TYPE_ACTION -> VHAction(HeaderPendingBinding.inflate(inflater, parent, false), fm)
            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder:RecyclerView.ViewHolder, position: Int) {
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

    fun setLoading(b:Boolean){
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
        val idx = viewModel.tracks.indexOfFirst { it.playedWhen == track.playedWhen &&
                    it.name == track.name && it.artist == track.artist }
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

    fun setPending(fm:FragmentManager?, pendingListData: PendingListData) {
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
        if (totalCount == 0){
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
                    sectionHeaders[shift + displayCount] = sectionHeaders[sectionHeaders.keys.last()]!!
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

    fun setStatusHeader(){
        val username = if (viewModel.username != null)
            fragmentBinding.root.context.getString(R.string.possession, viewModel.username) + " "
        else
            ""
        val header =  if (isShowingLoves)
            username + fragmentBinding.root.context.getString(R.string.recently_loved)
        else if (viewModel.toTime > 0)
            username + fragmentBinding.root.context.getString(
                R.string.scrobbles_till,
                    DateFormat.getMediumDateFormat(fragmentBinding.root.context).format(viewModel.toTime))
        else
            username + fragmentBinding.root.context.getString(R.string.recently_scrobbled)
        setStatusHeader(header)
    }

    fun setStatusHeader(s:String){
        var idx = nonTrackViewCount
        if (idx == 0)
            idx++
        if (sectionHeaders[idx - 1] != s) {
            sectionHeaders[idx - 1] = s
            notifyItemChanged(idx - 1, 0)
        }
    }

    override fun getItemId(position: Int): Long {
        return if(position < nonTrackViewCount)
            position.toLong()
        else if (position < viewModel.tracks.size)
            viewModel.tracks[position - nonTrackViewCount].playedWhen?.time ?: Stuff.NP_ID.toLong()
        else
            Stuff.NP_ID.toLong()
    }

    fun populate(oldTracks: MutableList<Track>) {
        if (MainActivity.isOnline)
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
        if (!viewModel.loadedNw || fragmentBinding.recentsSwipeRefresh.isRefreshing) {
            fragmentBinding.recentsList.scheduleLayoutAnimation()
            if (isShowingLoves)
                notifyItemChanged(0, 0) //animation gets delayed otherwise
        }
        setLoading(false)

        lastPopulateTime = System.currentTimeMillis()
    }

    override fun loadImg(pos: Int){
        val idx = pos - nonTrackViewCount
        if(idx >= 0 && idx < viewModel.tracks.size){
            viewModel.loadInfo(viewModel.tracks[idx], pos)
        }
    }

    // only called for loves fragment
    fun setImg(pos: Int, imgMapp: Map<ImageSize, String>?){
        val idx = pos - nonTrackViewCount
        if(idx >= 0 && idx < viewModel.tracks.size){
            val track = viewModel.tracks[idx]
            track.imageUrlsMap = imgMapp
            viewModel.imgMap[Stuff.genHashCode(track.artist, track.name)] = imgMapp ?: mapOf()
            notifyItemChanged(pos)
            if (pos == viewModel.selectedPos)
                setHeroListener.onSetHero(pos, viewModel.tracks[idx], false)
        }
    }

    fun removeHandlerCallbacks(){
        handler.cancelAll()
    }

    class DiffCallback(private val newList: List<Track>, private val oldList: List<Track>, private val lastPopulateTime: Long) : DiffUtil.Callback() {

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
                    oldList[oldItemPosition].getImageURL(ImageSize.MEDIUM) == newList[newItemPosition].getImageURL(ImageSize.MEDIUM)
        }
    }

    class MyUpdateCallback(private val adapter: RecyclerView.Adapter<*>): ListUpdateCallback {
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

    class VHAction(private val binding: HeaderPendingBinding, fm: FragmentManager?) : RecyclerView.ViewHolder(binding.root){
        init {
            if (fm!= null)
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

    inner class VHTrack(private val binding: ListItemRecentsBinding) : RecyclerView.ViewHolder(binding.root), View.OnFocusChangeListener {
        init {
            binding.root.setOnClickListener {
                itemClickListener.onItemClick(itemView, adapterPosition)
            }
            binding.root.setOnLongClickListener {
                itemLongClickListener.onItemLongClick(itemView, adapterPosition)
                true
            }
            binding.root.onFocusChangeListener = this
            if (viewModel.username != null && !MainActivity.isTV) {
                binding.recentsMenu.visibility = View.INVISIBLE
                binding.recentsMenuText.visibility = View.GONE
            } else
                binding.recentsMenu.setOnClickListener {
                    itemClickListener.onItemClick(it, adapterPosition)
                }
        }

        override fun onFocusChange(view: View?, focused: Boolean) {
            if (view != null && !view.isInTouchMode && focused)
                focusChangeListener.onFocus(itemView, adapterPosition)
        }

        private fun setSelected(selected:Boolean, track: Track = viewModel.tracks[viewModel.selectedPos - nonTrackViewCount]) {
            itemView.isActivated = selected
            if (selected)
                setHeroListener.onSetHero(adapterPosition, track, false)
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
                    val albumHeight = itemView.context.resources.getDimension(R.dimen.album_text_height).toInt()
                    binding.recentsAlbum.visibility = View.GONE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        albumHeight/2,
                        0,
                        albumHeight/2
                    )
                }
            }

            if (track.isNowPlaying) {
                binding.recentsDate.visibility = View.GONE
                Stuff.nowPlayingAnim(binding.recentsPlaying, true)
            } else {
                binding.recentsDate.visibility = View.VISIBLE
                binding.recentsDate.text =
                    Stuff.myRelativeTime(itemView.context, track.playedWhen?.time ?: 0)
                Stuff.nowPlayingAnim(binding.recentsPlaying, false)
            }

            if (track.isLoved) {
                if (binding.recentsImgOverlay.background == null)
                    binding.recentsImgOverlay.background = ContextCompat.getDrawable(binding.recentsImgOverlay.context,
                        R.drawable.vd_heart_stroked
                    )
                binding.recentsImgOverlay.visibility = View.VISIBLE
            } else {
                binding.recentsImgOverlay.visibility = View.INVISIBLE
            }

            val imgUrl = track.getWebpImageURL(ImageSize.LARGE)

            if (imgUrl != null && imgUrl != "") {
                binding.recentsImg.clearColorFilter()
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(R.drawable.vd_wave_simple_filled)
                        .error(R.drawable.vd_wave_simple_filled)
                        .into(binding.recentsImg)

            } else {
                binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
                binding.recentsImg.setColorFilter(
                    Stuff.getMatColor(
                        binding.recentsImg.context,
                        Stuff.genHashCode(track.artist, track.name).toLong()
                    )
                )
                if (isShowingLoves){
                    if(viewModel.imgMap[Stuff.genHashCode(track.artist, track.name)] == null)
                        handler.sendMessage(binding.recentsImg.hashCode(), adapterPosition)
                }
            }
            setSelected(adapterPosition == viewModel.selectedPos, track)
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