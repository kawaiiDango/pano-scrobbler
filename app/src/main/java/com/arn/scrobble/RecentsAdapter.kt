package com.arn.scrobble

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.pending.PendingScrFragment
import com.arn.scrobble.pending.db.PendingScrobble
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.VHHeader
import com.arn.scrobble.ui.VHPending
import com.squareup.picasso.Picasso
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_recents.view.*
import kotlinx.android.synthetic.main.header_pending.view.*
import kotlinx.android.synthetic.main.list_item_recents.view.*
import java.util.*
import kotlin.math.max


/**
 * Created by arn on 10/07/2017.
 */

class RecentsAdapter

(private val fragmentContent: View): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var totalPages:Int = 1
    private var itemClickListener: ItemClickListener? = null
    private var mSetHeroListener: SetHeroTrigger? = null
    private val tracksList = mutableListOf<Track>()
    private val sectionHeaders = mutableMapOf<Int,String>()
    private val pendingScrobbles = mutableMapOf<Int, PendingScrobble>()
    var selectedPos = NP_ID
    private var actionHeaderPos = -1
    private var actionData = -1
    private var nonTrackViewCount = 0
    private var loadMoreListener: EndlessRecyclerViewScrollListener? = null
    private var fm: FragmentManager? = null
    private val myUpdateCallback = MyUpdateCallback(this)

    init {
//        setHasStableIds(true) //causes some opengl OOM and new holders to be created for no reason
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TRACK -> VHTrack(inflater.inflate(R.layout.list_item_recents, parent, false))
            TYPE_PENDING_TRACK -> VHPending(inflater.inflate(R.layout.list_item_recents, parent, false))
            TYPE_HEADER -> VHHeader(inflater.inflate(R.layout.header_default, parent, false))
            TYPE_ACTION -> VHAction(inflater.inflate(R.layout.header_pending, parent, false), fm)
            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder:RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VHTrack -> holder.setItemData(tracksList[position - nonTrackViewCount])
            is VHPending -> holder.setItemData(pendingScrobbles[position] ?: return)
            is VHHeader -> holder.setHeaderText(sectionHeaders[position] ?: "...")
            is VHAction -> holder.setItemData(actionData)
            else -> throw RuntimeException("Invalid view type $holder")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == actionHeaderPos -> TYPE_ACTION
            pendingScrobbles.containsKey(position) -> TYPE_PENDING_TRACK
            position >= nonTrackViewCount -> TYPE_TRACK
            else -> TYPE_HEADER
        }
    }

    fun setLoading(b:Boolean){
        loadMoreListener?.loading = b
//        fragmentContent.footer_progressbar.visibility = if (b) View.VISIBLE else View.GONE
        fragmentContent.recents_swipe_refresh.isRefreshing = false
    }

    fun getLoading() = loadMoreListener?.loading == true

    fun setClickListener(itemClickListener: ItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    fun setHeroListener(l: SetHeroTrigger) {
        mSetHeroListener = l
    }

    fun setLoadMoreReference(lm: EndlessRecyclerViewScrollListener?){
        loadMoreListener = lm
    }

    fun getTrack(pos: Int): Track? {
        return if (getItemViewType(pos) == TYPE_TRACK)
            tracksList[pos - nonTrackViewCount]
        else
            null
    }

    fun removeTrack(track: Track) {
        val idx = tracksList.indexOfFirst { it.playedWhen == track.playedWhen &&
                    it.name == track.name && it.artist == track.artist }
        if (idx != -1) {
            tracksList.removeAt(idx)
            notifyItemRemoved(idx + nonTrackViewCount)
        }
    }

    fun editTrack(artist: String, album: String, title: String, timeMillis: Long) {
        val track = Track(title, null, album, artist)
        track.playedWhen = if (timeMillis != 0L)
                Date(timeMillis)
            else
                null
        val idx = tracksList.indexOfFirst { it.playedWhen == track.playedWhen }
        if (idx != -1) {
            tracksList[idx] = track
            notifyItemChanged(idx + nonTrackViewCount)
        }
    }

    override fun getItemCount() = tracksList.size + nonTrackViewCount

    fun setPendingScrobbles(fm:FragmentManager?, psList: List<PendingScrobble>, count:Int) {
        val headerText = fragmentContent.context.getString(R.string.pending_scrobbles)
        var shift = 0
        val lastPsSize = pendingScrobbles.size
        actionHeaderPos = -1
        if (count == 0 || psList.isEmpty()){
            this.fm = null
            if (sectionHeaders.containsValue(headerText)) {
                val lastval = sectionHeaders[nonTrackViewCount - 1]!!
                sectionHeaders.clear()
                sectionHeaders[0] = lastval
            }
        } else {
            this.fm = fm
            if (count < 3) {
                shift = 1
            } else {
                shift = 2
                actionHeaderPos = 3
                actionData = count - psList.size
            }

            sectionHeaders.keys.forEach {
                if (it != 0 && it != shift + psList.size)
                    sectionHeaders.remove(it)
            }
            if (sectionHeaders[0] !=  headerText) {
                sectionHeaders[shift + psList.size] = sectionHeaders[0]!!
                sectionHeaders[0] = headerText
            }
        }
        pendingScrobbles.clear()
        psList.forEachIndexed { i, ps ->
            pendingScrobbles[1 + i] = ps
        }
        val oldNonTrackViewCount = nonTrackViewCount
        nonTrackViewCount = shift + 1 + psList.size
        selectedPos += nonTrackViewCount - oldNonTrackViewCount
        if (lastPsSize == psList.size)
            notifyItemRangeChanged(0, nonTrackViewCount, 0)
        else
            notifyDataSetChanged()
    }

    fun setStatusHeader(s:String){
        if (nonTrackViewCount == 0)
            nonTrackViewCount++
        if (sectionHeaders[nonTrackViewCount-1] != s) {
            sectionHeaders[nonTrackViewCount - 1] = s
            notifyItemChanged(nonTrackViewCount - 1, 0)
        }
    }

    override fun getItemId(position: Int): Long {
        return if(position < nonTrackViewCount)
            position.toLong()
        else
            tracksList[position - nonTrackViewCount].playedWhen?.time ?: NP_ID.toLong()
    }

    fun populate(res: PaginatedResult<Track>, page: Int, keepSelection: Boolean) {
        val refresh = fragmentContent.recents_swipe_refresh
        if (refresh == null || !refresh.isShown)
            return

        if (Main.isOnline)
            setStatusHeader(fragmentContent.context.getString(R.string.recently_scrobbled))
        else
            setStatusHeader(fragmentContent.context.getString(R.string.offline))

        setLoading(false)
        val selectedId = getItemId(selectedPos - nonTrackViewCount)
        var selectedPos = nonTrackViewCount
        synchronized(tracksList) {
            val oldList = mutableListOf<Track>()
            oldList.addAll(tracksList)
            totalPages = max(1, res.totalPages) //dont let totalpages be 0
            if (page == 1) {
                tracksList.clear()
                if (res.isEmpty) {
                    setStatusHeader(refresh.context.getString(R.string.no_scrobbles))
                }
            }
            res.pageResults.toMutableList()
            res.forEachIndexed { i, it ->
                if (!it.isNowPlaying || page == 1) {
                    tracksList.add(it)
                    if (getItemId(tracksList.size - 1) == selectedId)
                        selectedPos = tracksList.size + nonTrackViewCount - 1
                }
            }
            this.selectedPos = selectedPos
            myUpdateCallback.offset = nonTrackViewCount
            myUpdateCallback.selectedPos = selectedPos
            val diff = DiffUtil.calculateDiff(DiffCallback(tracksList, oldList), false)
            diff.dispatchUpdatesTo(myUpdateCallback)
        }
    }

    class DiffCallback(private var newList: List<Track>, private var oldList: List<Track>) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].playedWhen?.time == newList[newItemPosition].playedWhen?.time
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].name == newList[newItemPosition].name &&
                    oldList[oldItemPosition].artist == newList[newItemPosition].artist &&
                    oldList[oldItemPosition].isLoved == newList[newItemPosition].isLoved
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

    inner class VHTrack(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private val vOverlay = view.recents_img_overlay
        private val vMenu = view.recents_menu
        private val vPlaying = view.recents_playing
        private val vDate = view.recents_date
        private val vTitle = view.recents_title
        private val vSubtitle = view.recents_subtitle
        private val vImg = view.recents_img

        init {
            view.setOnClickListener(this)

            vMenu.setOnClickListener {
                itemClickListener?.onItemClick(it, adapterPosition)
            }
            vOverlay.setOnClickListener {
                itemClickListener?.onItemClick(it, adapterPosition)
            }
            vDate.setOnClickListener {
                vMenu.callOnClick()
            }
        }

        override fun onClick(view: View) {
            itemClickListener?.onItemClick(itemView, adapterPosition)
        }

        fun setSelected(selected:Boolean, track: Track) {
            itemView.isActivated = selected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                vImg.foreground = if (selected) ColorDrawable(vImg.context.getColor(R.color.thumbnailHighlight)) else null
            }
            if (selected)
                mSetHeroListener?.onSetHero(adapterPosition, track, false)
        }

        fun setItemData(track: Track) {
            vTitle.text = track.name
            vSubtitle.text = track.artist

            if (track.isNowPlaying) {
                vDate.visibility = View.INVISIBLE
                Stuff.nowPlayingAnim(vPlaying, true)
            } else {
                vDate.visibility = View.VISIBLE
                vDate.text = Stuff.myRelativeTime(itemView.context, track.playedWhen)
                Stuff.nowPlayingAnim(vPlaying, false)
            }

            if (track.isLoved) {
                if (vOverlay.background == null)
                    vOverlay.background = vOverlay.context.getDrawable(R.drawable.vd_heart_solid)
                vOverlay.visibility = View.VISIBLE
            } else {
                vOverlay.visibility = View.INVISIBLE
            }

            val imgUrl = Stuff.getAlbumOrArtistImg(track, false)

            if (imgUrl != null && imgUrl != "") {
                vImg.clearColorFilter()
                Picasso.get()
                        .load(imgUrl)
                        .fit()
                        .centerInside()
                        .placeholder(R.drawable.vd_wave_simple)
                        .error(R.drawable.vd_wave_simple)
                        .into(vImg)

            } else {
                vImg.setImageResource(R.drawable.vd_wave_simple)
                vImg.setColorFilter(Stuff.getMatColor(vImg.context, "500", track.name.hashCode().toLong()))
            }
            setSelected(adapterPosition == selectedPos, track)
        }
    }

    interface SetHeroTrigger {
        fun onSetHero(position: Int, track: Track, fullSize: Boolean)
    }
}

private const val NP_ID = -5

private const val TYPE_TRACK = 0
private const val TYPE_PENDING_TRACK = 1
private const val TYPE_HEADER = 2
private const val TYPE_ACTION = 3