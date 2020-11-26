package com.arn.scrobble.ui

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.pending.db.PendingScrobble

class VHPendingScrobble(private val binding: ListItemRecentsBinding, itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.recentsPlaying.visibility = View.GONE
        binding.recentsImgOverlay.background = ContextCompat.getDrawable(itemView.context, R.drawable.vd_hourglass)
        binding.recentsImg.setImageResource(R.drawable.vd_wave_simple)
        binding.recentsImgOverlay.visibility = View.VISIBLE
        binding.recentsImgOverlay.contentDescription = itemView.context.getString(R.string.pending_scrobble)
        if (Main.isTV)
            binding.root.setOnClickListener {
                itemClickListener.onItemClick(binding.recentsMenu, adapterPosition)
            }
        binding.recentsMenu.setOnClickListener {
            itemClickListener.onItemClick(it, adapterPosition)
        }
    }

    fun setItemData(ps: PendingScrobble) {
        binding.recentsTitle.text = ps.track
        binding.recentsSubtitle.text = ps.artist
        binding.recentsDate.text = Stuff.myRelativeTime(itemView.context, ps.timestamp)
    }

}