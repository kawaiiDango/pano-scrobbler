package com.arn.scrobble.pending

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.ui.ItemClickListener

class VHPendingScrobble(
    private val binding: ListItemRecentsBinding,
    private val isShowingAlbums: Boolean,
    itemClickListener: ItemClickListener,
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.recentsPlaying.visibility = View.GONE
        binding.recentsImgOverlay.background = ContextCompat.getDrawable(itemView.context, R.drawable.vd_hourglass)
        binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
        binding.recentsImgOverlay.visibility = View.VISIBLE
        binding.recentsImgOverlay.contentDescription = itemView.context.getString(R.string.pending_scrobble)
        if (MainActivity.isTV)
            binding.root.setOnClickListener {
                itemClickListener.call(binding.recentsMenu, bindingAdapterPosition)
            }
        binding.recentsMenu.setOnClickListener {
            itemClickListener.call(it, bindingAdapterPosition)
        }
    }

    fun setItemData(ps: PendingScrobble) {
        binding.recentsTitle.text = ps.track
        binding.recentsSubtitle.text = ps.artist
        binding.recentsDate.text = Stuff.myRelativeTime(itemView.context, ps.timestamp)

        if (isShowingAlbums) {
            if (ps.album.isNotEmpty()) {
                binding.recentsAlbum.text = ps.album
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
    }

}