package com.arn.scrobble.charts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import coil.load
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.Stuff.getTintedDrwable
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.databinding.GridItemChartBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.EntryItemClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import de.umass.lastfm.*
import java.text.NumberFormat


open class ChartsAdapter(protected val binding: FrameChartsListBinding) :
    RecyclerView.Adapter<ChartsAdapter.VHChart>(), LoadMoreGetter {

    lateinit var clickListener: EntryItemClickListener
    lateinit var viewModel: ChartsVM
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    open val itemSizeDp = 185.dp
    open val forceDimensions = false
    private var maxCount = -2
    var checkAllForMax = false

    @StringRes
    var emptyTextRes = R.string.charts_no_data
    var showArtists = true
    var requestAlbumInfo = true

    private val getMaxCount = {
        if (checkAllForMax) {
            if (maxCount == -2)
                maxCount = viewModel.chartsData.maxOfOrNull { it.playcount } ?: -1
            maxCount
        } else
            viewModel.chartsData[0].playcount
    }

    init {
        super.setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.chartsProgress.show()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHChart {
        val inflater = LayoutInflater.from(parent.context)
        val holderBinding = GridItemChartBinding.inflate(inflater, parent, false)
        if (forceDimensions) {
            val lp = holderBinding.root.layoutParams
            lp.width = itemSizeDp
            lp.height = itemSizeDp
        }
        return VHChart(
            holderBinding,
            itemSizeDp,
            clickListener,
            getMaxCount
        )
    }

    override fun getItemCount() = viewModel.chartsData.size

    override fun getItemId(position: Int): Long {
        return when (val item = viewModel.chartsData[position]) {
            is Artist -> item.name.hashCode().toLong()
            is Album -> Stuff.genHashCode(item.artist, item.name).toLong()
            is Track -> Stuff.genHashCode(item.artist, item.name).toLong()
            else -> 0L
        }
    }

    override fun onBindViewHolder(holder: VHChart, position: Int) {
        val item = viewModel.chartsData[position]
        holder.setItemData(position, item, showArtists, requestAlbumInfo)
    }

    open fun populate() {
        binding.chartsList.layoutAnimation = null
        if (viewModel.chartsData.isEmpty()) {
            if (itemCount == 0) {
                if (!MainActivity.isOnline)
                    binding.chartsStatus.text =
                        binding.root.context.getString(R.string.unavailable_offline)
                else
                    binding.chartsStatus.text = binding.root.context.getString(emptyTextRes)
                TransitionManager.beginDelayedTransition(binding.root, Fade())
                binding.chartsStatus.visibility = View.VISIBLE
                binding.chartsProgress.hide()
                binding.chartsList.visibility = View.INVISIBLE
            }
        } else {
            if (binding.chartsList.visibility != View.VISIBLE) {
                TransitionManager.beginDelayedTransition(binding.root, Fade())
                binding.chartsList.visibility = View.VISIBLE
            }
            binding.chartsStatus.visibility = View.GONE
            binding.chartsProgress.hide()
        }
        loadMoreListener.loading = false
        notifyDataSetChanged()
    }

    class VHChart(
        private val binding: GridItemChartBinding,
        itemSizeDp: Int,
        private val clickListener: EntryItemClickListener,
        private val getMaxCount: () -> Int
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var entryData: MusicEntry? = null

        init {
            itemView.setOnClickListener(this)
            itemView.minimumWidth = itemSizeDp
            itemView.minimumHeight = itemSizeDp
        }

        override fun onClick(view: View) {
            entryData?.let {
                clickListener.onItemClick(view, it)
            }
        }

        fun setItemData(
            pos: Int,
            entry: MusicEntry,
            showArtists: Boolean,
            requestAlbumInfo: Boolean
        ) {
            entryData = entry
            var imgUrl: String? = null
            when (entry) {
                is Artist -> {
                    binding.chartInfoSubtitle.visibility = View.GONE
                }
                is Album -> {
                    binding.chartInfoSubtitle.text = entry.artist
                    imgUrl = entry.getWebpImageURL(ImageSize.EXTRALARGE)
                }
                is Track -> binding.chartInfoSubtitle.text = entry.artist
            }

            binding.chartInfoTitle.text =
                itemView.context.getString(R.string.charts_num_text, (pos + 1), entry.name)
            binding.chartInfoScrobbles.text = itemView.context.resources.getQuantityString(
                R.plurals.num_scrobbles_noti, entry.playcount,
                NumberFormat.getInstance().format(entry.playcount)
            )
            if (!showArtists)
                binding.chartInfoSubtitle.visibility = View.GONE

            val maxCount = getMaxCount()
            if (maxCount <= 0) {
                binding.chartInfoBar.visibility = View.INVISIBLE //so that it acts as a padding
                binding.chartInfoScrobbles.visibility = View.GONE
            } else {
                binding.chartInfoBar.progress = entry.playcount * 100 / maxCount
                binding.chartInfoBar.visibility = View.VISIBLE
                binding.chartInfoScrobbles.visibility = View.VISIBLE
            }

            val errorDrawable = itemView.context.getTintedDrwable(R.drawable.vd_wave_simple_filled, entry.name.hashCode())

            if (entry is Album && !requestAlbumInfo) {
                binding.chartImg.load(imgUrl ?: "") {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
                }
            } else if (entry !is Album || requestAlbumInfo) {
                binding.chartImg.load(entry) {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
//                    transitionFactory { _, _ -> NoCrossfadeOnErrorTransition() }
                }
            }
        }
    }
}
