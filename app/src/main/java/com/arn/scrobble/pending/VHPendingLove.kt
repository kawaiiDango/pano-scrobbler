package com.arn.scrobble.pending

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.ui.ItemClickListener

class VHPendingLove(
    private val binding: ListItemRecentsBinding,
    isShowingAlbums: Boolean,
    itemClickListener: ItemClickListener<Any>,
) : RecyclerView.ViewHolder(binding.root) {
    lateinit var pl: PendingLove

    init {
        binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
        binding.recentsImgOverlay.visibility = View.VISIBLE
        binding.recentsMenu.setOnClickListener {
            itemClickListener.call(it, bindingAdapterPosition) { pl }
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
        this.pl = pl
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