package com.arn.scrobble.edits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemBlockedMetadataBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.ui.ItemClickListener


class BlockedMetadataAdapter(
    private val viewModel: BlockedMetadataVM,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<BlockedMetadataAdapter.VHBlockedTag>() {

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHBlockedTag {
        val inflater = LayoutInflater.from(parent.context)
        return VHBlockedTag(
            ListItemBlockedMetadataBinding.inflate(inflater, parent, false),
            itemClickListener
        )
    }

    override fun onBindViewHolder(holder: VHBlockedTag, position: Int) {
        holder.setItemData(viewModel.blockedMetadata[position])
    }

    fun getItem(idx: Int) = viewModel.blockedMetadata[idx]

    override fun getItemCount() = viewModel.blockedMetadata.size

    override fun getItemId(position: Int) = viewModel.blockedMetadata[position]._id.toLong()

    class VHBlockedTag(
        private val binding: ListItemBlockedMetadataBinding,
        private val itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { itemClickListener.call(it, bindingAdapterPosition) }

            fun setScaledDrawable(textView: TextView, @DrawableRes drawableRes: Int) {
                val scaleFactor = 0.8f
                val size = (textView.lineHeight * scaleFactor).toInt()
                val sd = ContextCompat.getDrawable(itemView.context, drawableRes)!!
                sd.setBounds(0, 0, size, size)
                textView.setCompoundDrawablesRelative(sd, null, null, null)
            }

            setScaledDrawable(binding.track, R.drawable.vd_note)
            setScaledDrawable(binding.artist, R.drawable.vd_mic)
            setScaledDrawable(binding.album, R.drawable.vd_album)
            setScaledDrawable(binding.albumArtist, R.drawable.vd_album_artist)
        }

        fun setItemData(blockedMetadata: BlockedMetadata) {
            binding.track.visibility =
                if (blockedMetadata.track.isEmpty()) View.GONE else View.VISIBLE
            binding.track.text = blockedMetadata.track
            binding.artist.visibility =
                if (blockedMetadata.artist.isEmpty()) View.GONE else View.VISIBLE
            binding.artist.text = blockedMetadata.artist
            binding.album.visibility =
                if (blockedMetadata.album.isEmpty()) View.GONE else View.VISIBLE
            binding.album.text = blockedMetadata.album
            binding.albumArtist.visibility =
                if (blockedMetadata.albumArtist.isEmpty()) View.GONE else View.VISIBLE
            binding.albumArtist.text = blockedMetadata.albumArtist

            if (blockedMetadata.skip) {
                binding.action.visibility = View.VISIBLE
                binding.action.setImageResource(R.drawable.vd_skip)
                binding.action.contentDescription = itemView.context.getString(R.string.skip)
            } else if (blockedMetadata.mute) {
                binding.action.visibility = View.VISIBLE
                binding.action.setImageResource(R.drawable.vd_mute)
                binding.action.contentDescription = itemView.context.getString(R.string.mute)
            } else {
                binding.action.visibility = View.GONE
            }

            binding.delete.setOnClickListener {
                itemClickListener.call(it, bindingAdapterPosition)
            }
        }
    }
}
