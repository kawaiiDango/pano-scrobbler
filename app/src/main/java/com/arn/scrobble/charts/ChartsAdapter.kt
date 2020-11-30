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
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.databinding.GridItemChartBinding
import com.arn.scrobble.ui.*
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.umass.lastfm.*
import java.lang.ref.WeakReference
import java.text.NumberFormat


open class ChartsAdapter (protected val binding: FrameChartsListBinding) :
        RecyclerView.Adapter<ChartsAdapter.VHChart>(), LoadImgInterface, LoadMoreGetter {

    lateinit var clickListener: EntryItemClickListener
    lateinit var viewModel: ChartsVM
    private val handler by lazy { EntryInfoHandler(WeakReference(this)) }
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    open val itemSizeDp = 185
    open val forceDimensions = false
    open val roundCorners = false
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
        val holderBinding = GridItemChartBinding.inflate(inflater, parent, false)
        if (forceDimensions) {
            val lp = holderBinding.root.layoutParams
            lp.width = Stuff.dp2px(itemSizeDp, parent.context)
            lp.height = Stuff.dp2px(itemSizeDp, parent.context)
            holderBinding.root.layoutParams = lp
        }
        return VHChart(
                holderBinding,
                itemSizeDp,
                roundCorners,
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
                    binding.chartsStatus.text = binding.root.context.getString(R.string.unavailable_offline)
                else
                    binding.chartsStatus.text = binding.root.context.getString(emptyTextRes)
                binding.chartsStatus.visibility = View.VISIBLE
                binding.chartsProgress.visibility = View.GONE
            }
        } else {
            binding.chartsStatus.visibility = View.GONE
            binding.chartsProgress.visibility = View.GONE
        }
        loadMoreListener.loading = false
        notifyDataSetChanged()
    }

    fun removeHandlerCallbacks(){
        handler.cancelAll()
    }

    class VHChart(
            private val binding: GridItemChartBinding,
            itemSizeDp: Int,
            roundCorners: Boolean,
            private val clickListener: EntryItemClickListener,
            private val queueEntryInfo: (Int, ImageView) -> Unit,
            private val getMaxCount: () -> Int
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var entryData: MusicEntry? = null

        init {
            itemView.setOnClickListener(this)
            val px = Stuff.dp2px(itemSizeDp, itemView.context)
            itemView.minimumWidth = px
            itemView.minimumHeight = px
            if (roundCorners)
                binding.chartImg.shapeAppearanceModel = binding.chartImg
                        .shapeAppearanceModel
                        .toBuilder()
                        .setAllCornerSizes(itemView.context.resources.getDimension(R.dimen.charts_corner_radius))
                        .build()
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
                    binding.chartInfoSubtitle.visibility = View.GONE
                }
                is Album -> {
                    binding.chartInfoSubtitle.text = entry.artist
                    if (imgUrlp == null)
                        imgUrl = entry.getWebpImageURL(ImageSize.EXTRALARGE)
                }
                is Track -> binding.chartInfoSubtitle.text = entry.artist
            }

            binding.chartInfoTitle.text = itemView.context.getString(R.string.charts_num_text, (pos + 1), entry.name)
            binding.chartInfoScrobbles.text = itemView.context.resources.
                    getQuantityString(R.plurals.num_scrobbles_noti, entry.playcount,
                            NumberFormat.getInstance().format(entry.playcount))
            if (!showArtists)
                binding.chartInfoSubtitle.visibility = View.GONE

            val maxCount = getMaxCount()
            if (maxCount <= 0) {
                binding.chartInfoBar.visibility = View.INVISIBLE //so that it acts as a padding
                binding.chartInfoScrobbles.visibility = View.GONE
            } else {
                binding.chartInfoBar.progress = entry.playcount*100 / maxCount
                binding.chartInfoBar.visibility = View.VISIBLE
                binding.chartInfoScrobbles.visibility = View.VISIBLE
            }
            if (imgUrl != null && imgUrl != "") {
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(R.drawable.vd_wave_simple)
                        .error(R.drawable.vd_wave_simple)
                        .into(binding.chartImg, object : Callback{
                            override fun onSuccess() {
                                binding.chartImg.clearColorFilter()
                            }

                            override fun onError(e: Exception) {
                            }
                        })

            } else {
                binding.chartImg.setImageResource(R.drawable.vd_wave_simple)
                binding.chartImg.setColorFilter(Stuff.getMatColor(itemView.context, "500", entry.name.hashCode().toLong()))
                if (!(entry is Album && !requestAlbumInfo))
                    queueEntryInfo(adapterPosition, binding.chartImg)
            }
        }
    }
}
