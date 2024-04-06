package com.arn.scrobble.charts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import coil.load
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.databinding.GridItemChartBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.LoadMoreGetter
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.UiUtils.getTintedDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.util.Objects


open class ChartsAdapter(
    protected val binding: FrameChartsListBinding
) :
    ListAdapter<MusicEntry, ChartsAdapter.VHChart>(
        GenericDiffCallback { o, n ->
            if (o is Artist && n is Artist)
                o.name == n.name
            else if (o is Album && n is Album)
                o.name == n.name && o.artist?.name == n.artist?.name
            else if (o is Track && n is Track)
                o.name == n.name && o.artist.name == n.artist.name
            else
                false
        }
    ), LoadMoreGetter {

    lateinit var clickListener: MusicEntryItemClickListener
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    protected open val isHorizontalList = false
    private var maxCount = -2
    var checkAllForMax = false
    private val skeleton by lazy {
        binding.chartsList.createSkeletonWithFade(
            R.layout.grid_item_chart_skeleton,
        )
    }

    @StringRes
    var emptyTextRes = R.string.charts_no_data
    var showArtists = true
    var showDuration = false

    private val getMaxCount = {
        if (checkAllForMax) {
            if (maxCount == -2)
                maxCount = currentList.maxOfOrNull { it.playcount ?: 0 } ?: -1
            maxCount
        } else
            getItem(0).playcount ?: -1
    }

    private val getSpan = {
        (binding.chartsList.layoutManager as? GridLayoutManager)?.spanCount
    }

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
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

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is Artist -> item.name.hashCode().toLong()
            is Album -> Objects.hash(item.artist!!.name, item.name).toLong()
            is Track -> Objects.hash(item.artist.name, item.name).toLong()
        }
    }

    override fun onBindViewHolder(holder: VHChart, position: Int) {
        holder.setItemData(position, getItem(position), showArtists, showDuration)
    }

    fun progressVisible(visible: Boolean) {
        if (visible && itemCount == 0) {
            skeleton.showSkeleton()
            binding.chartsStatus.isVisible = false
        } else {
            skeleton.showOriginal()
//            binding.root.scheduleLayoutAnimation()

            if (itemCount == 0)
                binding.chartsStatus.isVisible = true
        }
    }

    open fun populate(newList: List<MusicEntry>) {
        val oldCount = itemCount
        submitList(newList) {
            if (oldCount != 0)
                binding.chartsList.scrollToPosition(0)
            binding.chartsList.isVisible = newList.isNotEmpty()
        }
        binding.chartsList.layoutAnimation = null
        if (newList.isEmpty()) {
            if (!Stuff.isOnline)
                binding.chartsStatus.text =
                    binding.root.context.getString(R.string.unavailable_offline)
            else
                binding.chartsStatus.text = binding.root.context.getString(emptyTextRes)
            TransitionManager.beginDelayedTransition(binding.root, Fade())
            binding.chartsStatus.isVisible = true
        } else {
            if (binding.chartsList.visibility != View.VISIBLE) {
                TransitionManager.beginDelayedTransition(binding.root, Fade())
            }
            binding.chartsStatus.isVisible = false
        }
        progressVisible(false)
        loadMoreListener.loading = false
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
            val imgReq = MusicEntryImageReq(
                entry,
                fetchAlbumInfoIfMissing = (entry is Album && entry.webp300 == null) || (entry is Track && entry.album == null)
            )
            when (entry) {
                is Artist -> {
                    binding.chartInfoSubtitle.isVisible = false
                }

                is Album -> {
                    binding.chartInfoSubtitle.text = entry.artist!!.name
                }

                is Track -> binding.chartInfoSubtitle.text = entry.artist.name
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
                R.plurals.num_scrobbles_noti, entry.playcount ?: 0,
                entry.playcount?.format()
            )

            if (showDuration && entry is Track && (entry.duration ?: 0) > 0) {
                numScrobblesString += " • " + Stuff.humanReadableDuration(
                    entry.duration!! * (entry.playcount ?: 0)
                )
            }

            if (BuildConfig.DEBUG && entry.stonksDelta != Int.MAX_VALUE && entry.stonksDelta != null)
                numScrobblesString += " • ${entry.stonksDelta}"

            if (entry.playcount != null) {
                binding.chartInfoScrobbles.visibility = View.VISIBLE
                binding.chartInfoScrobbles.text = numScrobblesString
            } else {
                binding.chartInfoScrobbles.visibility = View.GONE
            }

            if (!showArtists)
                binding.chartInfoSubtitle.visibility = View.GONE

            var progressValue = entry.match?.let { (it * 100).toInt() }

            if (entry.playcount != null && entry.match == null) {
                val maxCount = getMaxCount()
                if (maxCount > 0)
                    progressValue = entry.playcount!! * 100 / maxCount
            }
            if (progressValue != null) {
                binding.chartInfoBar.progress = progressValue
                binding.chartInfoBar.visibility = View.VISIBLE
            } else {
                binding.chartInfoBar.visibility = View.GONE
            }

            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                entry.name.hashCode()
            )

            binding.chartImg.load(imgReq) {
                placeholder(R.drawable.avd_loading)
                error(errorDrawable)
            }
        }
    }
}
