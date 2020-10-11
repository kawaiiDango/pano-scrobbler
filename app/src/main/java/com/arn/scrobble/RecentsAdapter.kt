package com.arn.scrobble

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.pending.PendingScrFragment
import com.arn.scrobble.pending.db.PendingLove
import com.arn.scrobble.pending.db.PendingScrobble
import com.arn.scrobble.ui.*
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_recents.view.*
import kotlinx.android.synthetic.main.header_pending.view.*
import kotlinx.android.synthetic.main.list_item_recents.view.*
import java.lang.ref.WeakReference
import java.util.*


/**
 * Created by arn on 10/07/2017.
 */

class RecentsAdapter

(private val fragmentContent: View): RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        LoadImgInterface, LoadMoreGetter {

    lateinit var itemClickListener: ItemClickListener
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
            TYPE_TRACK -> VHTrack(inflater.inflate(R.layout.list_item_recents, parent, false))
            TYPE_PENDING_SCROBBLE -> VHPendingScrobble(inflater.inflate(R.layout.list_item_recents, parent, false), itemClickListener)
            TYPE_PENDING_LOVE -> VHPendingLove(inflater.inflate(R.layout.list_item_recents, parent, false), itemClickListener)
            TYPE_HEADER -> VHHeader(inflater.inflate(R.layout.header_default, parent, false))
            TYPE_ACTION -> VHAction(inflater.inflate(R.layout.header_pending, parent, false), fm)
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
        fragmentContent.recents_swipe_refresh.isRefreshing = false
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
            viewModel.tracks.removeAt(idx)
            notifyItemRemoved(idx + nonTrackViewCount)
        }
    }

    fun editTrack(artist: String, album: String, title: String, timeMillis: Long) {
        val track = Track(title, null, album, artist)
        track.playedWhen = if (timeMillis != 0L)
                Date(timeMillis)
            else
                null
        val idx = viewModel.tracks.indexOfFirst { it.playedWhen == track.playedWhen }
        if (idx != -1) {
            val prevTrack = viewModel.tracks[idx]
            if (prevTrack.artist == artist && prevTrack.album == album && prevTrack.name == title)
                return
            viewModel.deletedTracksStringSet += prevTrack.toString()
            viewModel.tracks[idx] = track
            notifyItemChanged(idx + nonTrackViewCount)
        }
    }

    override fun getItemCount() = viewModel.tracks.size + nonTrackViewCount

    fun setPending(fm:FragmentManager?, pendingListData: PendingListData) {
        val headerText = fragmentContent.context.getString(R.string.pending_scrobbles)
        var shift = 0
        val lastDisplaySize = psMap.size + plMap.size
        val oldNonTrackViewCount = nonTrackViewCount
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
            fragmentContent.context.getString(R.string.possesion, viewModel.username) + " "
        else
            ""
        val header =  if (isShowingLoves)
            username + fragmentContent.context.getString(R.string.recently_loved)
        else if (viewModel.toTime > 0)
            username + fragmentContent.context.getString(R.string.scrobbles_till,
                    DateFormat.getMediumDateFormat(fragmentContent.context).format(viewModel.toTime))
        else
            username + fragmentContent.context.getString(R.string.recently_scrobbled)
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
        val refresh = fragmentContent.recents_swipe_refresh ?: return

        if (Main.isOnline)
            setStatusHeader()
        else
            setStatusHeader(fragmentContent.context.getString(R.string.offline))

        setLoading(false)
        val selectedId = getItemId(viewModel.selectedPos)
        var selectedPos = nonTrackViewCount
        if (viewModel.tracks.isEmpty()) {
            setStatusHeader(refresh.context.getString(R.string.no_scrobbles))
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
        val diff = DiffUtil.calculateDiff(DiffCallback(viewModel.tracks, oldTracks), false)
        diff.dispatchUpdatesTo(myUpdateCallback)
    }

    override fun loadImg(pos: Int){
        val idx = pos - nonTrackViewCount
        if(idx >= 0 && idx < viewModel.tracks.size){
            viewModel.loadInfo(viewModel.tracks[idx], pos)
        }
    }

    fun setImg(pos: Int, imgMapp: Map<ImageSize, String>?){
        val idx = pos - nonTrackViewCount
        if(idx >= 0 && idx < viewModel.tracks.size){
            val track = viewModel.tracks[idx]
            track.imageUrlsMap = imgMapp
            viewModel.imgMap[Stuff.genHashCode(track.artist, track.name)] = imgMapp ?: mapOf()
            notifyItemChanged(pos)
            if (pos == viewModel.selectedPos)
                setHeroListener.onSetHero(pos, viewModel.tracks[idx], true)
        }
    }

    fun removeHandlerCallbacks(){
        handler.cancelAll()
    }

    class DiffCallback(private var newList: List<Track>, private var oldList: List<Track>) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].playedWhen?.time == newList[newItemPosition].playedWhen?.time
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].name == newList[newItemPosition].name &&
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

    class VHAction(view: View, fm: FragmentManager?) : RecyclerView.ViewHolder(view){
        private val vText = view.pending_more
        private val vAction = view.pending_expand

        init {
            if (fm!= null)
                    view.pending_summary.setOnClickListener {
                    fm.beginTransaction()
                            .replace(R.id.frame, PendingScrFragment())
                            .addToBackStack(null)
                            .commit()
                    }
        }

        fun setItemData(n: Int) {
            vAction.text = itemView.context.getString(R.string.show_all)
            vText.text = itemView.context.getString(R.string.n_more, n)
        }
    }

    inner class VHTrack(view: View) : RecyclerView.ViewHolder(view), View.OnFocusChangeListener {

        private val vOverlay = view.recents_img_overlay
        private val vMenu = view.recents_menu
        private val vPlaying = view.recents_playing
        private val vDate = view.recents_date
        private val vTitle = view.recents_title
        private val vSubtitle = view.recents_subtitle
        private val vImg = view.recents_img

        init {
            view.setOnClickListener {
                itemClickListener.onItemClick(itemView, adapterPosition)
            }
            view.onFocusChangeListener = this
            if (viewModel.username != null && !Main.isTV)
                vMenu.visibility = View.INVISIBLE
            else
                vMenu.setOnClickListener {
                    itemClickListener.onItemClick(it, adapterPosition)
                }
        }

        override fun onFocusChange(view: View?, focused: Boolean) {
            if (view != null && !view.isInTouchMode && focused)
                focusChangeListener.onFocus(itemView, adapterPosition)
        }

        fun setSelected(selected:Boolean, track: Track = viewModel.tracks[viewModel.selectedPos - nonTrackViewCount]) {
            itemView.isActivated = selected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                vImg.foreground = if (selected) ColorDrawable(vImg.context.getColor(R.color.thumbnailHighlight)) else null
            }
            if (selected)
                setHeroListener.onSetHero(adapterPosition, track, false)
        }

        fun setItemData(track: Track) {
            vTitle.text = track.name
            vSubtitle.text = track.artist

            if (track.isNowPlaying) {
                vDate.visibility = View.GONE
                Stuff.nowPlayingAnim(vPlaying, true)
            } else {
                vDate.visibility = View.VISIBLE
                vDate.text = Stuff.myRelativeTime(itemView.context, track.playedWhen?.time ?: 0, true)
                Stuff.nowPlayingAnim(vPlaying, false)
            }

            if (track.isLoved) {
                if (vOverlay.background == null)
                    vOverlay.background = ContextCompat.getDrawable(vOverlay.context, R.drawable.vd_heart_stroked)
                vOverlay.visibility = View.VISIBLE
            } else {
                vOverlay.visibility = View.INVISIBLE
            }

            val imgUrl = track.getWebpImageURL(ImageSize.LARGE)

            if (imgUrl != null && imgUrl != "") {
                vImg.clearColorFilter()
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(R.drawable.vd_wave_simple)
                        .error(R.drawable.vd_wave_simple)
                        .into(vImg)

            } else {
                vImg.setImageResource(R.drawable.vd_wave_simple)
                vImg.setColorFilter(Stuff.getMatColor(vImg.context, "500", Stuff.genHashCode(track.artist, track.name).toLong()))
                if (isShowingLoves){
                    if(viewModel.imgMap[Stuff.genHashCode(track.artist, track.name)] == null)
                        handler.sendMessage(vImg.hashCode(), adapterPosition)
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