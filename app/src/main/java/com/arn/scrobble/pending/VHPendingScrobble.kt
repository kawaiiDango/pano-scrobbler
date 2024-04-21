package com.arn.scrobble.pending

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PendingScrobbleWithSource
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.utils.Stuff

class VHPendingScrobble(
    private val binding: ListItemRecentsBinding,
    private val isShowingAlbums: Boolean,
    itemClickListener: ItemClickListener<Any>,
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var ps: PendingScrobbleWithSource

    init {
        binding.recentsPlaying.visibility = View.GONE
        binding.recentsImgOverlay.background =
            ContextCompat.getDrawable(itemView.context, R.drawable.vd_hourglass)
        binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
        binding.recentsImgOverlay.visibility = View.VISIBLE
        binding.recentsImgOverlay.contentDescription =
            itemView.context.getString(R.string.pending_scrobble)
        binding.recentsMenu.setOnClickListener {
            itemClickListener.call(it, bindingAdapterPosition) { ps }
        }
    }

    fun setItemData(ps: PendingScrobbleWithSource) {
        this.ps = ps

        binding.recentsTitle.text = ps.pendingScrobble.track
        binding.recentsSubtitle.text = ps.pendingScrobble.artist
        binding.recentsDate.text = Stuff.myRelativeTime(
            itemView.context,
            ps.pendingScrobble.timestamp
        )

        if (isShowingAlbums) {
            if (ps.pendingScrobble.album.isNotEmpty()) {
                binding.recentsAlbum.text = ps.pendingScrobble.album
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
    }

}