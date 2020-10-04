package com.arn.scrobble

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.grid_item_recents.view.*
import kotlinx.android.synthetic.main.header_default.view.*

/**
 * Created by arn on 29/12/2017.
 */
class SimilarTracksAdapter (private val fragmentContent: View, private val viewModel: TracksVM) : RecyclerView.Adapter<SimilarTracksAdapter.VHSimilar>() {

    lateinit var clickListener: ItemClickListener
    lateinit var focusChangeListener: FocusChangeListener
    var itemSizeDp = 150

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHSimilar{
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.grid_item_recents, parent, false)
        return VHSimilar(view, itemSizeDp, clickListener, focusChangeListener)
    }

    fun getItem(id: Int) = viewModel.tracks[id]

    fun setImg(pos:Int, imgMapp: Map<ImageSize, String>?){
        viewModel.imgMap[pos] = imgMapp ?: mapOf()
        notifyItemChanged(pos)
    }

    override fun getItemCount() = viewModel.tracks.size

    override fun onBindViewHolder(holder:VHSimilar, position: Int) {
        holder.setItemData(viewModel.tracks[position], viewModel.imgMap[position]?.get(ImageSize.EXTRALARGE))
    }

    fun populate(tracks: List<Track>){
        if (tracks.isEmpty())
            fragmentContent.header_text?.text = fragmentContent.context.getString(R.string.no_similar_tracks)
        else {
            if (tracks != viewModel.tracks) {
                viewModel.tracks.clear()
                viewModel.tracks.addAll(tracks)
            }
            notifyDataSetChanged()
        }
    }

    class VHSimilar(view: View, sizeDp: Int, private val clickListener: ItemClickListener,
                    private val focusChangeListener: FocusChangeListener) :
            RecyclerView.ViewHolder(view), View.OnClickListener, View.OnFocusChangeListener {
        private val vTitle = view.recents_title
        private val vSubtitle = view.recents_subtitle
        private val vImg = view.recents_img

        init {
            itemView.setOnClickListener(this)
            itemView.onFocusChangeListener = this
            val px = Stuff.dp2px(sizeDp, view.context)
            view.minimumWidth = px
            view.minimumHeight = px
        }

        override fun onClick(view: View) {
            clickListener.onItemClick(view, adapterPosition)
        }

        override fun onFocusChange(view: View?, focused: Boolean) {
            if (view != null && !view.isInTouchMode && focused)
                focusChangeListener.onFocus(itemView, adapterPosition)
        }

        fun setItemData(track: Track, imgUrl:String?) {
            vTitle.text = track.name
            vSubtitle.text = track.artist

            if (imgUrl != null && imgUrl != "") {
                Picasso.get()
                        .load(imgUrl)
                        .noPlaceholder()
                        .error(R.drawable.vd_wave_simple)
                        .into(vImg, object : Callback{
                            override fun onSuccess() {
                                vImg.clearColorFilter()
                            }

                            override fun onError(e: Exception) {
                            }
                        })

            } else {
                vImg.setImageResource(R.drawable.vd_wave_simple)
                vImg.setColorFilter(Stuff.getMatColor(itemView.context, "500", track.name.hashCode().toLong()))
            }
        }
    }
}
