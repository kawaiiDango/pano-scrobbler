package com.arn.scrobble.charts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.ui.*
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.*
import kotlinx.android.synthetic.main.content_charts.view.*
import kotlinx.android.synthetic.main.grid_item_chart.view.*
import java.lang.ref.WeakReference


class ChartsAdapter (private val fragmentContent: View) :
        RecyclerView.Adapter<ChartsAdapter.VHChart>(), LoadImgInterface, LoadMoreGetter {

    lateinit var clickListener: ItemClickListener
    lateinit var viewModel: ChartsVM
    private val handler by lazy { EntryInfoHandler(WeakReference(this)) }
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    var itemSizeDp = 180

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHChart {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.grid_item_chart, parent, false)
        return VHChart(view)
    }

    fun getItem(pos: Int) = viewModel.chartsData[pos]

    private fun getImgUrl(pos: Int): String? {
        val id = getItemId(pos).toInt()
        return viewModel.imgMap[id]
    }

    fun getMaxCount() = getItem(0).playcount

    override fun loadImg(pos:Int){
        if(pos >= 0 && pos < viewModel.chartsData.size){
            val entry = viewModel.chartsData[pos]
            when (entry) {
                is Artist -> viewModel.loadArtistInfo(entry, pos)
                is Album -> viewModel.loadAlbumInfo(entry, pos)
                is Track -> viewModel.loadTrackInfo(entry, pos)
            }
        }
    }

    fun setImg(pos: Int, imgUrl: String){
        if(pos >= 0 && pos < viewModel.chartsData.size){
            viewModel.imgMap[getItemId(pos).toInt()] = imgUrl
            notifyItemChanged(pos)
        }
    }

    override fun getItemCount() = viewModel.chartsData.size

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is Artist -> item.name.hashCode().toLong()
            is Album -> Stuff.genHashCode(item.artist, item.name).toLong()
            is Track -> Stuff.genHashCode(item.artist, item.name).toLong()
            else -> 0L
        }
    }

    override fun onBindViewHolder(holder: VHChart, position: Int) {
        val item =  getItem(position)
        val imgUrl =  getImgUrl(position)
        holder.setItemData(position, item, imgUrl)
    }

    fun populate(){
        if (viewModel.chartsData.isEmpty()) {
            if (itemCount == 0) {
                if (!Main.isOnline)
                    fragmentContent.charts_status?.text = fragmentContent.context.getString(R.string.unavailable_offline)
                else
                    fragmentContent.charts_status?.text = fragmentContent.context.getString(R.string.charts_no_data)
                fragmentContent.charts_status?.visibility = View.VISIBLE
                fragmentContent.charts_progress?.visibility = View.GONE
            }
        } else {
            fragmentContent.charts_status?.visibility = View.GONE
            fragmentContent.charts_progress?.visibility = View.GONE
        }
        loadMoreListener.loading = false
        notifyDataSetChanged()
    }

    fun removeHandlerCallbacks(){
        handler.cancelAll()
    }

    inner class VHChart(view: View) :
            RecyclerView.ViewHolder(view), View.OnClickListener{
        private val vTitle = view.chart_info_title
        private val vSubtitle = view.chart_info_subtitle
        private val vScrobbles = view.chart_info_scrobbles
        private val vBar = view.chart_info_bar
        private val vImg = view.chart_img

        init {
            itemView.setOnClickListener(this)
            val px = Stuff.dp2px(itemSizeDp, view.context)
            view.minimumWidth = px
            view.minimumHeight = px
        }

        override fun onClick(view: View) {
            clickListener.onItemClick(view, adapterPosition)
        }

        fun setItemData(pos: Int, entry: MusicEntry, imgUrlp:String?) {
            var imgUrl = imgUrlp
            when (entry) {
                is Artist -> {
                    vSubtitle.visibility = View.GONE
                }
                is Album -> {
                    vSubtitle.text = entry.artist
                    if (imgUrlp == null)
                        imgUrl = entry.getWebpImageURL(ImageSize.EXTRALARGE)
                }
                is Track -> vSubtitle.text = entry.artist
            }

            vTitle.text = itemView.context.getString(R.string.charts_num_text, (pos + 1), entry.name)
            vScrobbles.text = itemView.context.resources.
                    getQuantityString(R.plurals.num_scrobbles_noti, entry.playcount, entry.playcount)
            val maxCount = getMaxCount()
            if (maxCount == 0)
                vBar.visibility = View.GONE
            else {
                vBar.progress = entry.playcount*100 / maxCount
                vBar.visibility = View.VISIBLE
            }
            if (imgUrl != null && imgUrl != "") {
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(R.drawable.vd_wave_simple)
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
                vImg.setColorFilter(Stuff.getMatColor(itemView.context, "500", entry.name.hashCode().toLong()))
                if (!viewModel.imgMap.containsKey(getItemId(adapterPosition).toInt()))
                    handler.sendMessage(vImg.hashCode(), adapterPosition)
            }
        }
    }
}
