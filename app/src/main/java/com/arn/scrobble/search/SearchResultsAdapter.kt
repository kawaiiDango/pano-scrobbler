package com.arn.scrobble.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.charts.ChartsVM
import com.arn.scrobble.databinding.ContentSearchBinding
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.ui.EntryInfoHandler
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.LoadImgInterface
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.*
import java.lang.ref.WeakReference
import java.text.NumberFormat
import kotlin.math.min


class SearchResultsAdapter(private val fragmentBinding: ContentSearchBinding):
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
            TYPE_HEADER -> VHSearchHeader(HeaderWithActionBinding.inflate(inflater, parent, false))
            TYPE_RESULT -> VHSearchResult(ListItemRecentsBinding.inflate(inflater, parent, false))
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

    fun populate(searchResults: SearchVM.SearchResults, expandType: Int, diffAnimate: Boolean) {
        this.expandType = expandType
        fragmentBinding.searchProgress.hide()
        fragmentBinding.searchResultsList.visibility = View.VISIBLE
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
        if (diffAnimate) {
            val diff = DiffUtil.calculateDiff(DiffCallback(data, oldData), false)
            diff.dispatchUpdatesTo(this)
        } else {
            fragmentBinding.searchResultsList.scheduleLayoutAnimation()
            notifyDataSetChanged()
        }
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

    inner class VHSearchResult(private val binding: ListItemRecentsBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.recentsMenuText.visibility = View.GONE
            binding.recentsMenu.visibility = View.INVISIBLE
            itemView.setOnClickListener { clickListener.onItemClick(itemView, adapterPosition) }
        }

        fun setData(entry: MusicEntry, imgUrlp:String?) {
            var imgUrl = imgUrlp
            binding.recentsTitle.text = entry.name
            if (entry.listeners > 0)
                binding.recentsDate.text = itemView.context.resources.getQuantityString(
                    R.plurals.num_listeners, entry.listeners, NumberFormat.getInstance().format(entry.listeners)
                )
            else
                binding.recentsDate.text = ""
            when (entry) {
                is Album -> {
                    imgUrl = entry.getWebpImageURL(ImageSize.LARGE)
                    binding.recentsSubtitle.text = entry.artist
                }
                is Track -> binding.recentsSubtitle.text = entry.artist
                else -> binding.recentsSubtitle.text = ""
            }
            if (imgUrl != null && imgUrl != "") {
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(R.drawable.vd_wave_simple_filled)
                        .error(R.drawable.vd_wave_simple_filled)
                        .into(binding.recentsImg, object : Callback {
                            override fun onSuccess() {
                                binding.recentsImg.clearColorFilter()
                            }

                            override fun onError(e: Exception) {
                            }
                        })

            } else {
                binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
                binding.recentsImg.setColorFilter(
                    Stuff.getMatColor(
                        itemView.context,
                        entry.name.hashCode().toLong()
                    )
                )
                if (entry !is Album)
                    queueEntryInfo(adapterPosition, binding.recentsImg)
            }
        }
    }

    inner class VHSearchHeader(private val binding: HeaderWithActionBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.headerAction.setOnClickListener { clickListener.onItemClick(itemView, adapterPosition) }
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
                    text = if (MainActivity.isOnline)
                        itemView.context.getString(R.string.not_found)
                    else
                        itemView.context.getString(R.string.unavailable_offline)
                }
            }
            if (count > 3 ) {
                binding.headerAction.visibility = View.VISIBLE
                if (expandType == type)
                    binding.headerAction.text = itemView.context.getString(R.string.collapse)
                else
                    binding.headerAction.text = itemView.context.getString(R.string.show_all)
            } else {
                binding.headerAction.visibility = View.GONE
            }
            binding.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(ContextCompat.getDrawable(itemView.context, drawableRes), null, null, null)
            binding.headerText.text = text
        }
    }
}

private const val TYPE_HEADER = 0
private const val TYPE_RESULT = 1
private const val TYPE_NOT_FOUND = 10