package com.arn.scrobble.charts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.StringRes
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
import java.text.NumberFormat


open class ChartsAdapter (protected val fragmentContent: View) :
        RecyclerView.Adapter<ChartsAdapter.VHChart>(), LoadImgInterface, LoadMoreGetter {

    lateinit var clickListener: EntryItemClickListener
    lateinit var viewModel: ChartsVM
    private val handler by lazy { EntryInfoHandler(WeakReference(this)) }
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    open val itemSizeDp = 185
    open val forceDimensions = false
    private var maxCount = -2
    var checkAllForMax = false
    @StringRes
    var emptyTextRes = R.string.charts_no_data
    var showArtists = true
    var requestAlbumInfo = true

    private val queueEntryInfo = { pos: Int, imageView: ImageView ->
        if (!viewModel.imgMap.containsKey(getItemId(pos).toInt()))
            handler.sendMessage(imageView.hashCode(), pos)
    }
    private val getMaxCount = {
        if (checkAllForMax) {
            if (maxCount == -2)
                maxCount = viewModel.chartsData.maxOfOrNull { it.playcount } ?: -1
            maxCount
        } else
            getItem(0).playcount
    }

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHChart {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.grid_item_chart, parent, false)
        if (forceDimensions) {
            val lp = view.layoutParams
            lp.width = Stuff.dp2px(itemSizeDp, parent.context)
            lp.height = Stuff.dp2px(itemSizeDp, parent.context)
            view.layoutParams = lp
        }
        return VHChart(
                view,
                itemSizeDp,
                clickListener,
                queueEntryInfo,
                getMaxCount
        )
    }

    fun getItem(pos: Int) = viewModel.chartsData[pos]

    private fun getImgUrl(pos: Int): String? {
        val id = getItemId(pos).toInt()
        return viewModel.imgMap[id]
    }

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
        holder.setItemData(position, item, imgUrl, showArtists, requestAlbumInfo)
    }

    open fun populate(){
        if (viewModel.chartsData.isEmpty()) {
            if (itemCount == 0) {
                if (!Main.isOnline)
                    fragmentContent.charts_status?.text = fragmentContent.context.getString(R.string.unavailable_offline)
                else
                    fragmentContent.charts_status?.text = fragmentContent.context.getString(emptyTextRes)
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

    class VHChart(
            view: View,
            itemSizeDp: Int,
            private val clickListener: EntryItemClickListener,
            private val queueEntryInfo: (Int, ImageView) -> Unit,
            private val getMaxCount: () -> Int
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private val vTitle = view.chart_info_title
        private val vSubtitle = view.chart_info_subtitle
        private val vScrobbles = view.chart_info_scrobbles
        private val vBar = view.chart_info_bar
        private val vImg = view.chart_img
        var entryData: MusicEntry? = null

        init {
            itemView.setOnClickListener(this)
            val px = Stuff.dp2px(itemSizeDp, view.context)
            view.minimumWidth = px
            view.minimumHeight = px
        }

        override fun onClick(view: View) {
            entryData?.let {
                clickListener.onItemClick(view, it)
            }
        }

        fun setItemData(pos: Int, entry: MusicEntry, imgUrlp:String?, showArtists: Boolean, requestAlbumInfo: Boolean) {
            entryData = entry
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
                    getQuantityString(R.plurals.num_scrobbles_noti, entry.playcount,
                            NumberFormat.getInstance().format(entry.playcount))
            if (!showArtists)
                vSubtitle.visibility = View.GONE

            val maxCount = getMaxCount()
            if (maxCount <= 0) {
                vBar.visibility = View.INVISIBLE //so that it acts as a padding
                vScrobbles.visibility = View.GONE
            } else {
                vBar.progress = entry.playcount*100 / maxCount
                vBar.visibility = View.VISIBLE
                vScrobbles.visibility = View.VISIBLE
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
                if (!(entry is Album && !requestAlbumInfo))
                    queueEntryInfo(adapterPosition, vImg)
            }
        }
    }
}
