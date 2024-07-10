package com.arn.scrobble.recents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.dispose
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.LoadMoreGetter
import com.arn.scrobble.ui.MusicEntryItemClickListener
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.getTintedDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects


/**
 * Created by arn on 10/07/2017.
 */

class TrackHistoryAdapter(
    private val viewModel: TracksVM,
    private val itemClickListener: MusicEntryItemClickListener,
    private val isShowingAlbums: Boolean,
    private val isShowingPlayers: Boolean,
) : ListAdapter<Track, TrackHistoryAdapter.VHTrackHistoryItem>(
    GenericDiffCallback { old, new -> old.date == new.date }
), LoadMoreGetter {

    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener
    private val scrobbleSourcesDao = PanoDb.db.getScrobbleSourcesDao()

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
        holder.setItemData(getItem(position))
    }

    override fun getItemId(position: Int) = getItem(position).date ?: 0L

    inner class VHTrackHistoryItem(
        private val binding: ListItemRecentsBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var job: Job? = null

        init {
            binding.recentsPlaying.visibility = View.GONE
            binding.recentsImgFrame.background = null
            binding.recentsTrackLl.background = null
            binding.recentsMenu.isInvisible = true
            binding.root.isFocusable = true

            binding.root.setOnClickListener {
                itemClickListener.onItemClick(
                    binding.recentsMenu,
                    getItem(bindingAdapterPosition)
                )
            }
        }

        fun setItemData(track: Track) {
            binding.recentsTitle.text = track.name
            binding.recentsSubtitle.text = track.artist.name

            if (isShowingAlbums) {
                if (track.album != null) {
                    binding.recentsAlbum.text = track.album.name
                    binding.recentsAlbum.visibility = View.VISIBLE
                } else {
                    binding.recentsAlbum.visibility = View.GONE
                }
            }

            if (isShowingPlayers) {
                setPlayerIcon(track)
            }

            binding.recentsDate.visibility = View.VISIBLE
            binding.recentsDate.text =
                Stuff.myRelativeTime(itemView.context, track.date ?: 0)

            if (track.userloved == true) {
                if (binding.recentsImgOverlay.background == null)
                    binding.recentsImgOverlay.background = ContextCompat.getDrawable(
                        binding.recentsImgOverlay.context,
                        R.drawable.vd_heart_stroked
                    )
                binding.recentsImgOverlay.visibility = View.VISIBLE
            } else {
                binding.recentsImgOverlay.visibility = View.INVISIBLE
            }

            val imgUrl = track.webp300

            val errorDrawable = itemView.context.getTintedDrawable(
                R.drawable.vd_wave_simple_filled,
                Objects.hash(track.artist.name, track.name)
            )

            if (!imgUrl.isNullOrEmpty()) {
                binding.recentsImg.load(imgUrl) {
                    placeholder(R.drawable.avd_loading)
                    error(errorDrawable)
                }
            } else {
                binding.recentsImg.load(R.drawable.vd_wave_simple_filled)
            }
        }

        private fun setPlayerIcon(track: Track) {
            val timeMillis = track.date
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