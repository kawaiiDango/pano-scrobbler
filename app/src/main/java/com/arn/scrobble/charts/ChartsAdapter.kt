package com.arn.scrobble.charts

import android.transition.Fade
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.databinding.GridItemChartBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.UiUtils.getTintedDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import de.umass.lastfm.*
import java.text.NumberFormat


open class ChartsAdapter(protected val binding: FrameChartsListBinding) :
    RecyclerView.Adapter<ChartsAdapter.VHChart>(), LoadMoreGetter {

    lateinit var clickListener: MusicEntryItemClickListener
    lateinit var viewModel: ChartsVM
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    protected open val isHorizontalList = false
    private var maxCount = -2
    var checkAllForMax = false

    @StringRes
    var emptyTextRes = R.string.charts_no_data
    var showArtists = true
    var showDuration = false

    private val getMaxCount = {
        if (checkAllForMax) {
            if (maxCount == -2)
                maxCount = viewModel.chartsData.maxOfOrNull { it.playcount } ?: -1
            maxCount
        } else
            viewModel.chartsData[0].playcount
    }

    private val getSpan = {
        (binding.chartsList.layoutManager as? GridLayoutManager)?.spanCount
    }

    init {
        super.setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.chartsProgress.show()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHChart {
        val inflater = LayoutInflater.from(parent.context)
        val holderBinding = GridItemChartBinding.inflate(inflater, parent, false)
        if (isHorizontalList) {
            holderBinding.root.updateLayoutParams<RecyclerView.LayoutParams> {
                width =
                    parent.context.resources.getDimensionPixelSize(R.dimen.charts_horizontal_list_width)
            }
        }
        return VHChart(holderBinding, clickListener, getMaxCount, getSpan)
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
        holder.setItemData(position, item, showArtists, showDuration)
    }

    open fun populate() {
        binding.chartsList.layoutAnimation = null
        if (viewModel.chartsData.isEmpty()) {
            if (itemCount == 0) {
                if (!Stuff.isOnline)
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
        private val clickListener: MusicEntryItemClickListener,
        private val getMaxCount: () -> Int,
        private val getSpan: () -> Int?,
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var entryData: MusicEntry? = null
        private var isOneColumn = false

        init {
            Stuff.log(itemView.toString())
            itemView.setOnClickListener(this)
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
            showDuration: Boolean,
        ) {
            val numCols = getSpan()

            if (numCols == 1 && !isOneColumn) {
                ConstraintSet().apply {
                    clone(itemView.context, R.layout.list_item_chart)
                    applyTo(binding.root)
                }
                binding.chartImg.shapeAppearanceModel = ShapeAppearanceModel.builder(
                    itemView.context,
                    R.style.roundedCorners,
                    R.style.roundedCorners
                )
                    .build()
                isOneColumn = true
            } else if (numCols != null && numCols > 1 && isOneColumn) {
                ConstraintSet().apply {
                    clone(itemView.context, R.layout.grid_item_chart)
                    applyTo(binding.root)
                }
                binding.chartImg.shapeAppearanceModel = ShapeAppearanceModel.builder(
                    itemView.context,
                    R.style.topRoundedCorners,
                    R.style.topRoundedCorners
                )
                    .build()
                isOneColumn = false
            }

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

            binding.chartInfoTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                Stuff.stonksIconForDelta(entry.stonksDelta),
                0,
                0,
                0
            )

            var numScrobblesString = itemView.context.resources.getQuantityString(
                R.plurals.num_scrobbles_noti, entry.playcount,
                NumberFormat.getInstance().format(entry.playcount)
            )

            if (showDuration && entry is Track && entry.duration > 0) {
                numScrobblesString += " • " + Stuff.humanReadableDuration(entry.duration * entry.playcount)
            }

            if (BuildConfig.DEBUG && entry.stonksDelta != Int.MAX_VALUE && entry.stonksDelta != null)
                numScrobblesString += " • ${entry.stonksDelta}"

            binding.chartInfoScrobbles.text = numScrobblesString
            if (!showArtists)
                binding.chartInfoSubtitle.visibility = View.GONE

            val maxCount = getMaxCount()
            if (maxCount <= 0) {
                binding.chartInfoBar.visibility = View.GONE
                binding.chartInfoScrobbles.visibility = View.GONE
            } else {
                binding.chartInfoBar.progress = entry.playcount * 100 / maxCount
                binding.chartInfoBar.visibility = View.VISIBLE
                binding.chartInfoScrobbles.visibility = View.VISIBLE
            }

            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                entry.name.hashCode()
            )

            binding.chartImg.load(imgUrl ?: entry) {
                placeholder(R.drawable.vd_wave_simple_filled)
                error(errorDrawable)
            }
        }
    }
}
