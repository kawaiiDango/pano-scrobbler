package com.arn.scrobble.recents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track


/**
 * Created by arn on 10/07/2017.
 */

class TrackHistoryAdapter(
    private val viewModel: TracksVM,
    private val itemClickListener: ItemClickListener,
    private val isShowingAlbums: Boolean
    ): RecyclerView.Adapter<TrackHistoryAdapter.VHTrackHistoryItem>(),
        LoadMoreGetter {

    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHTrackHistoryItem {
        val inflater = LayoutInflater.from(parent.context)

        return VHTrackHistoryItem(
            ListItemRecentsBinding.inflate(inflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VHTrackHistoryItem, position: Int) {
        holder.setItemData(viewModel.tracks[position])
    }

    fun editTrack(track: Track) {
        val idx = viewModel.tracks.indexOfFirst { it.playedWhen == track.playedWhen }
        if (idx != -1) {
            val prevTrack = viewModel.tracks[idx]
            if (prevTrack.artist == track.artist && prevTrack.album == track.album && prevTrack.name == track.name)
                return
            viewModel.tracks[idx] = track
            notifyItemChanged(idx)
        }
    }

    fun getItem(idx: Int) = viewModel.tracks[idx]

    override fun getItemCount() = viewModel.tracks.size

    override fun getItemId(position: Int) = viewModel.tracks[position].playedWhen?.time ?: 0L

    inner class VHTrackHistoryItem(
        private val binding: ListItemRecentsBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                itemClickListener.onItemClick(itemView, adapterPosition)
            }
            binding.recentsPlaying.visibility = View.GONE
            if (viewModel.username != null) {
                binding.recentsMenu.visibility = View.GONE
                binding.recentsMenuText.visibility = View.GONE
            } else {
                binding.recentsMenu.setOnClickListener {
                    itemClickListener.onItemClick(it, adapterPosition)
                }
            }
        }

        fun setItemData(track: Track) {
            binding.recentsTitle.text = track.name
            binding.recentsSubtitle.text = track.artist

            if (isShowingAlbums) {
                if (!track.album.isNullOrEmpty()) {
                    binding.recentsAlbum.text = track.album
                    binding.recentsAlbum.visibility = View.VISIBLE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        0,
                        0,
                        0
                    )
                } else {
                    val albumHeight = itemView.context.resources.getDimension(R.dimen.album_text_height).toInt()
                    binding.recentsAlbum.visibility = View.GONE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        albumHeight/2,
                        0,
                        albumHeight/2
                    )
                }
            }

            binding.recentsDate.visibility = View.VISIBLE
            binding.recentsDate.text =
                Stuff.myRelativeTime(itemView.context, track.playedWhen?.time ?: 0)

            if (track.isLoved) {
                if (binding.recentsImgOverlay.background == null)
                    binding.recentsImgOverlay.background = ContextCompat.getDrawable(binding.recentsImgOverlay.context,
                        R.drawable.vd_heart_stroked
                    )
                binding.recentsImgOverlay.visibility = View.VISIBLE
            } else {
                binding.recentsImgOverlay.visibility = View.INVISIBLE
            }

            val imgUrl = track.getWebpImageURL(ImageSize.LARGE)

            if (imgUrl != null && imgUrl != "") {
                binding.recentsImg.clearColorFilter()
                Picasso.get()
                        .load(imgUrl)
                        .placeholder(R.drawable.vd_wave_simple_filled)
                        .error(R.drawable.vd_wave_simple_filled)
                        .into(binding.recentsImg)

            } else {
                binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
                binding.recentsImg.setColorFilter(
                    Stuff.getMatColor(
                        binding.recentsImg.context,
                        Stuff.genHashCode(track.artist, track.name).toLong()
                    )
                )
            }
        }
    }
}