package com.arn.scrobble

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.ui.EntryInfoHandler
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.LoadImgInterface
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.*
import kotlinx.android.synthetic.main.content_search.view.*
import kotlinx.android.synthetic.main.header_with_action.view.*
import kotlinx.android.synthetic.main.list_item_recents.view.*
import java.lang.ref.WeakReference
import java.text.NumberFormat
import kotlin.math.min


class SearchResultsAdapter(private val fragmentContent: View):
        RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        LoadImgInterface {
    lateinit var clickListener: ItemClickListener
    lateinit var chartsVM: ChartsVM
    private val data = mutableListOf<Any>()
    var expandType = -1

    private val handler by lazy { EntryInfoHandler(WeakReference(this)) }

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            TYPE_HEADER -> VHSearchHeader(inflater.inflate(R.layout.header_with_action, parent, false))
            TYPE_RESULT -> VHSearchResult(inflater.inflate(R.layout.list_item_recents, parent, false))
            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)) {
            TYPE_HEADER -> (holder as VHSearchHeader).setData(data[position] as Pair<Int, Int>)
            TYPE_RESULT ->
                (holder as VHSearchResult).setData(data[position] as MusicEntry, chartsVM.imgMap[getItem(position).hashCode()])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position] is Pair<*, *>)
            TYPE_HEADER
        else
            TYPE_RESULT
    }

    override fun getItemCount() = data.size

    override fun loadImg(pos:Int){
        if(pos >= 0 && pos < data.size && data[pos] is MusicEntry){
            when (val entry = data[pos]) {
                is Artist -> chartsVM.loadArtistInfo(entry, pos)
                is Track -> chartsVM.loadTrackInfo(entry, pos)
            }
        }
    }

    fun getItem(pos: Int) = data[pos]

    fun setImg(pos: Int, imgUrl: String){
        if(pos >= 0 && pos < data.size && data[pos] is MusicEntry){
            chartsVM.imgMap[getItem(pos).hashCode()] = imgUrl
            notifyItemChanged(pos)
        }
    }

    fun populate(searchResults: SearchVM.SearchResults, expandType: Int, animate: Boolean) {
        this.expandType = expandType
        fragmentContent.search_progress.visibility = View.GONE
        fragmentContent.search_history_list.visibility = View.GONE
        fragmentContent.search_results_list.visibility = View.VISIBLE
        val oldData = data.toList()
        data.clear()
        if (searchResults.artists.isEmpty() && searchResults.albums.isEmpty() && searchResults.tracks.isEmpty()){
            data += TYPE_NOT_FOUND to 0
        } else {
            if (searchResults.artists.isNotEmpty()) {
                data += Stuff.TYPE_ARTISTS to searchResults.artists.size
                if (expandType == Stuff.TYPE_ARTISTS)
                    data.addAll(searchResults.artists)
                else
                    for (i in 0..min(2, searchResults.artists.size - 1))
                        data += searchResults.artists[i]
            }
            if (searchResults.albums.isNotEmpty()) {
                data += Stuff.TYPE_ALBUMS to searchResults.albums.size
                if (expandType == Stuff.TYPE_ALBUMS)
                    data.addAll(searchResults.albums)
                else
                    for (i in 0..min(2, searchResults.albums.size - 1))
                        data += searchResults.albums[i]
            }
            if (searchResults.tracks.isNotEmpty()) {
                data += Stuff.TYPE_TRACKS to searchResults.tracks.size
                if (expandType == Stuff.TYPE_TRACKS)
                    data.addAll(searchResults.tracks)
                else
                    for (i in 0..min(2, searchResults.tracks.size - 1))
                        data += searchResults.tracks[i]
            }
        }
        if (animate) {
            val diff = DiffUtil.calculateDiff(DiffCallback(data, oldData), false)
            diff.dispatchUpdatesTo(this)
        } else
            notifyDataSetChanged()
    }

    fun queueEntryInfo(pos: Int, imageView: ImageView) {
        if (!chartsVM.imgMap.containsKey(getItem(pos).hashCode()))
            handler.sendMessage(imageView.hashCode(), pos)
    }

    class DiffCallback(private var newList: List<Any>, private var oldList: List<Any>) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition] == newList[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition] && !(oldList[oldItemPosition] is Pair<*, *> && newList[newItemPosition] is Pair<*, *>)
        }
    }

    inner class VHSearchResult(view: View): RecyclerView.ViewHolder(view) {
        private val vMenu = view.recents_menu
        private val vDate = view.recents_date
        private val vTitle = view.recents_title
        private val vSubtitle = view.recents_subtitle
        private val vImg = view.recents_img

        init {
            vMenu.visibility = View.INVISIBLE
            view.setOnClickListener { clickListener.onItemClick(itemView, adapterPosition) }
        }

        fun setData(entry: MusicEntry, imgUrlp:String?) {
            var imgUrl = imgUrlp
            vTitle.text = entry.name
            if (entry.listeners > 0)
                vDate.text = itemView.context.resources.getQuantityString(
                        R.plurals.num_listeners, entry.listeners, NumberFormat.getInstance().format(entry.listeners)
                )
            else
                vDate.text = ""
            when (entry) {
                is Album -> {
                    imgUrl = entry.getWebpImageURL(ImageSize.LARGE)
                    vSubtitle.text = entry.artist
                }
                is Track -> vSubtitle.text = entry.artist
                else -> vSubtitle.text = ""
            }
            if (imgUrl != null && imgUrl != "") {
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(R.drawable.vd_wave_simple)
                        .error(R.drawable.vd_wave_simple)
                        .into(vImg, object : Callback {
                            override fun onSuccess() {
                                vImg.clearColorFilter()
                            }

                            override fun onError(e: Exception) {
                            }
                        })

            } else {
                vImg.setImageResource(R.drawable.vd_wave_simple)
                vImg.setColorFilter(Stuff.getMatColor(itemView.context, "500", entry.name.hashCode().toLong()))
                if (entry !is Album)
                    queueEntryInfo(adapterPosition, vImg)
            }
        }
    }

    inner class VHSearchHeader(view: View): RecyclerView.ViewHolder(view) {
        private val vText = view.header_text
        private val vAction = view.header_action

        init {
            vAction.setOnClickListener { clickListener.onItemClick(itemView, adapterPosition) }
        }

        fun setData(headerData: Pair<Int, Int>) {
            val type = headerData.first
            val count = headerData.second
            val drawableRes: Int
            val text: String
            when(type) {
                Stuff.TYPE_ARTISTS -> {
                    drawableRes = R.drawable.vd_mic
                    text = itemView.context.getString(R.string.artists)
                }
                Stuff.TYPE_ALBUMS -> {
                    drawableRes = R.drawable.vd_album
                    text = itemView.context.getString(R.string.albums)
                }
                Stuff.TYPE_TRACKS -> {
                    drawableRes = R.drawable.vd_note
                    text = itemView.context.getString(R.string.tracks)
                }
                else -> {
                    drawableRes = R.drawable.vd_ban
                    text = if (Main.isOnline)
                        itemView.context.getString(R.string.not_found)
                    else
                        itemView.context.getString(R.string.unavailable_offline)
                }
            }
            if (count > 3 ) {
                vAction.visibility = View.VISIBLE
                if (expandType == type)
                    vAction.text = itemView.context.getString(R.string.collapse)
                else
                    vAction.text = itemView.context.getString(R.string.show_all)
            } else {
                vAction.visibility = View.GONE
            }
            vText.setCompoundDrawablesRelativeWithIntrinsicBounds(ContextCompat.getDrawable(itemView.context, drawableRes), null, null, null)
            vText.text = text
        }
    }
}

private const val TYPE_HEADER = 0
private const val TYPE_RESULT = 1
private const val TYPE_NOT_FOUND = 10