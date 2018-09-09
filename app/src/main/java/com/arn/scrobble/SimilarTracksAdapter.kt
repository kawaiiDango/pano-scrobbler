package com.arn.scrobble

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.ui.ItemClickListener
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.grid_item_recents.view.*
import kotlinx.android.synthetic.main.header_default.view.*

/**
 * Created by arn on 29/12/2017.
 */
class SimilarTracksAdapter (private val fragmentContent: View) : RecyclerView.Adapter<SimilarTracksAdapter.VHItem>() {

    private var clickListener: ItemClickListener? = null
    private val tracks = mutableListOf<Track>()
    var itemSizeDp = 150

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHItem{
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.grid_item_recents, parent, false)
        return VHItem(view, itemSizeDp, clickListener)
    }

    fun getItem(id: Int): Track {
        return tracks[id]
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder:VHItem, position: Int) {
        holder.setItemData(tracks[position])
    }

    fun setClickListener(itemClickListener: ItemClickListener) {
        clickListener = itemClickListener
    }

    class VHItem(view: View, sizeDp: Int, private val clickListener: ItemClickListener?) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val vFrame = view.recents_track_container
        private val vDate = view.recents_date
        private val vTitle = view.recents_title
        private val vSubtitle = view.recents_subtitle
        private val vImg = view.recents_img

        init {
            vFrame.setOnClickListener(this)
            vDate.visibility = View.GONE
            val dp = Stuff.dp2px(sizeDp, view.context)
            view.minimumWidth = dp
            view.minimumHeight = dp
        }

        override fun onClick(view: View) {
            clickListener?.onItemClick(view, adapterPosition)
        }

        fun setItemData(track: Track) {
            vTitle.text = track.name
            vSubtitle.text = track.artist


            val imgUrl = track.getImageURL(ImageSize.LARGE)

            if (imgUrl != null && imgUrl != "") {
                vImg.clearColorFilter()
                Picasso.get()
                        .load(imgUrl)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.vd_wave_simple)
                        .error(R.drawable.vd_wave_simple)
                        .into(vImg)

            } else {
                vImg.setImageResource(R.drawable.vd_wave_simple)
                vImg.setColorFilter(Stuff.getMatColor(itemView.context, "500", track.name.hashCode().toLong()))
            }
        }
    }

    fun populate(res: List<Track>){
        if (res.isEmpty())
            fragmentContent.header_text?.text = fragmentContent.context.getString(R.string.no_similar_tracks)
        else {
            tracks.clear()
            tracks.addAll(res)
            notifyDataSetChanged()
        }
    }
}
