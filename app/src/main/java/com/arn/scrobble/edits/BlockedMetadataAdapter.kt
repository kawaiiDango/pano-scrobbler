package com.arn.scrobble.edits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemSimpleEditBinding
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
        return VHBlockedTag(ListItemSimpleEditBinding.inflate(inflater, parent, false), itemClickListener)
    }

    override fun onBindViewHolder(holder: VHBlockedTag, position: Int) {
        holder.setItemData(viewModel.blockedMetadata[position])
    }

    fun getItem(idx: Int) = viewModel.blockedMetadata[idx]

    override fun getItemCount() = viewModel.blockedMetadata.size

    override fun getItemId(position: Int) = viewModel.blockedMetadata[position]._id.toLong()

    class VHBlockedTag(private val binding: ListItemSimpleEditBinding, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { itemClickListener.onItemClick(it, adapterPosition) }
            binding.editsImg.setImageResource(R.drawable.vd_ban)
        }

        fun setItemData(blockedMetadata: BlockedMetadata) {
            binding.editsTrack.text = blockedMetadata.track
            binding.editsArtist.text = if (blockedMetadata.artist.isNotEmpty())
                blockedMetadata.artist
            else
                blockedMetadata.albumArtist

            if (blockedMetadata.album.isNotEmpty()) {
                binding.editsAlbum.visibility = View.VISIBLE
                binding.editsAlbum.text = blockedMetadata.album
            } else
                binding.editsAlbum.visibility = View.GONE

            binding.editsDelete.setOnClickListener {
                itemClickListener.onItemClick(it, adapterPosition)
            }
        }
    }
}
