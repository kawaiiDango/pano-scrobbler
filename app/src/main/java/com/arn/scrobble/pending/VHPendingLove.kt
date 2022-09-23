package com.arn.scrobble.pending

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.ui.ItemClickListener

class VHPendingLove(
    private val binding: ListItemRecentsBinding,
    isShowingAlbums: Boolean,
    itemClickListener: ItemClickListener,
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.recentsPlaying.visibility = View.GONE
        binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
        binding.recentsImgOverlay.visibility = View.VISIBLE
        if (Stuff.isTv)
            binding.root.setOnClickListener {
                itemClickListener.call(binding.recentsMenu, bindingAdapterPosition)
            }
        binding.recentsMenu.setOnClickListener {
            itemClickListener.call(it, bindingAdapterPosition)
        }
        if (isShowingAlbums) {
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

    fun setItemData(pl: PendingLove) {
        binding.recentsTitle.text = pl.track
        binding.recentsSubtitle.text = pl.artist
        if (pl.shouldLove) {
            binding.recentsImgOverlay.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.vd_heart_stroked)
            binding.recentsDate.text = itemView.context.getString(R.string.loved)
            binding.recentsImgOverlay.contentDescription =
                itemView.context.getString(R.string.loved)
        } else {
            binding.recentsImgOverlay.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.vd_heart_break_stroked)
            binding.recentsDate.text = itemView.context.getString(R.string.unloved)
            binding.recentsImgOverlay.contentDescription =
                itemView.context.getString(R.string.unloved)
        }
    }

}