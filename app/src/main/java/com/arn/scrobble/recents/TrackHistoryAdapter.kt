package com.arn.scrobble.recents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import coil.size.Scale
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.getTintedDrwable
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.ScrobbleSourcesDao
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.arn.scrobble.ui.PackageName
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Created by arn on 10/07/2017.
 */

class TrackHistoryAdapter(
    private val viewModel: TracksVM,
    private val itemClickListener: ItemClickListener,
    private val isShowingAlbums: Boolean,
    private val isShowingPlayers: Boolean,
    private val scrobbleSourcesDao: ScrobbleSourcesDao
) : RecyclerView.Adapter<TrackHistoryAdapter.VHTrackHistoryItem>(),
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

        private var job: Job? = null

        init {
            binding.root.setOnClickListener {
                itemClickListener.call(itemView, bindingAdapterPosition)
            }
            binding.recentsPlaying.visibility = View.GONE
            if (viewModel.username != null) {
                binding.recentsMenu.visibility = View.GONE
                binding.recentsMenuText.visibility = View.GONE
            } else {
                binding.recentsMenu.setOnClickListener {
                    itemClickListener.call(it, bindingAdapterPosition)
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
                    val albumHeight =
                        itemView.context.resources.getDimension(R.dimen.album_text_height).toInt()
                    binding.recentsAlbum.visibility = View.GONE
                    binding.recentsTrackLl.setPaddingRelative(
                        0,
                        albumHeight / 2,
                        0,
                        albumHeight / 2
                    )
                }
            }

            if (isShowingPlayers) {
                setPlayerIcon(track)
            }

            binding.recentsDate.visibility = View.VISIBLE
            binding.recentsDate.text =
                Stuff.myRelativeTime(itemView.context, track.playedWhen?.time ?: 0)

            if (track.isLoved) {
                if (binding.recentsImgOverlay.background == null)
                    binding.recentsImgOverlay.background = ContextCompat.getDrawable(
                        binding.recentsImgOverlay.context,
                        R.drawable.vd_heart_stroked
                    )
                binding.recentsImgOverlay.visibility = View.VISIBLE
            } else {
                binding.recentsImgOverlay.visibility = View.INVISIBLE
            }

            val imgUrl = track.getWebpImageURL(ImageSize.LARGE)

            val errorDrawable = itemView.context.getTintedDrwable(
                R.drawable.vd_wave_simple_filled,
                Stuff.genHashCode(track.artist, track.name)
            )

            if (!imgUrl.isNullOrEmpty()) {
                binding.recentsImg.load(imgUrl) {
                    placeholder(R.drawable.vd_wave_simple_filled)
                    error(errorDrawable)
                }
            } else {
                binding.recentsImg.load(R.drawable.vd_wave_simple_filled)
            }
        }

        private fun setPlayerIcon(track: Track) {
            val timeMillis = track.playedWhen?.time
            binding.playerIcon.visibility = View.VISIBLE

            fun fetchIcon(pkgName: String) {
                binding.playerIcon.load(PackageName(pkgName)) {
                    scale(Scale.FIT)
                    listener(onSuccess = { _, _ ->
                        binding.playerIcon.contentDescription = pkgName
                    })
                }
            }

            if (timeMillis != null && viewModel.pkgMap[timeMillis] != null) {
                fetchIcon(viewModel.pkgMap[timeMillis]!!)
            } else {
                binding.playerIcon.dispose()
                binding.playerIcon.load(null)
                binding.playerIcon.contentDescription = null
                job?.cancel()

                if (timeMillis != null) {
                    job = viewModel.viewModelScope.launch(Dispatchers.IO) {
                        delay(100)
                        scrobbleSourcesDao.findPlayer(timeMillis)?.pkg?.let { pkgName ->
                            viewModel.pkgMap[timeMillis] = pkgName
                            fetchIcon(pkgName)
                        }
                    }
                }
            }
        }

    }
}