package com.arn.scrobble.ui

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.pending.db.PendingLove

class VHPendingLove(private val binding: ListItemRecentsBinding, itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.recentsPlaying.visibility = View.GONE
        binding.recentsImg.setImageResource(R.drawable.vd_wave_simple_filled)
        binding.recentsImgOverlay.visibility = View.VISIBLE
        if (Main.isTV)
            binding.root.setOnClickListener {
                itemClickListener.onItemClick(binding.recentsMenu, adapterPosition)
            }
        binding.recentsMenu.setOnClickListener {
            itemClickListener.onItemClick(it, adapterPosition)
        }
    }

    fun setItemData(pl: PendingLove) {
        binding.recentsTitle.text = pl.track
        binding.recentsSubtitle.text = pl.artist
        if (pl.shouldLove) {
            binding.recentsImgOverlay.background = ContextCompat.getDrawable(itemView.context, R.drawable.vd_heart_stroked)
            binding.recentsDate.text = itemView.context.getString(R.string.loved)
            binding.recentsImgOverlay.contentDescription = itemView.context.getString(R.string.loved)
        } else {
            binding.recentsImgOverlay.background = ContextCompat.getDrawable(itemView.context, R.drawable.vd_heart_break_stroked)
            binding.recentsDate.text = itemView.context.getString(R.string.unloved)
            binding.recentsImgOverlay.contentDescription = itemView.context.getString(R.string.unloved)
        }
    }

}