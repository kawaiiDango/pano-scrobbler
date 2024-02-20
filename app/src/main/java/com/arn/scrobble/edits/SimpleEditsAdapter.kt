package com.arn.scrobble.edits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ListItemSimpleEditBinding
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.ItemClickListener


class SimpleEditsAdapter(
    private val itemClickListener: ItemClickListener<SimpleEdit>,
) : ListAdapter<SimpleEdit, SimpleEditsAdapter.VHSimpleEdit>(
    GenericDiffCallback { oldItem, newItem -> oldItem._id == newItem._id }
) {

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHSimpleEdit {
        val inflater = LayoutInflater.from(parent.context)
        return VHSimpleEdit(
            ListItemSimpleEditBinding.inflate(inflater, parent, false),
            itemClickListener
        )
    }

    override fun onBindViewHolder(holder: VHSimpleEdit, position: Int) {
        holder.setItemData(getItem(position))
    }

    override fun getItemId(position: Int) =
        getItem(position)._id.toLong()


    inner class VHSimpleEdit(
        private val binding: ListItemSimpleEditBinding,
        private val itemClickListener: ItemClickListener<SimpleEdit>
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                itemClickListener.call(
                    it,
                    bindingAdapterPosition
                ) { getItem(bindingAdapterPosition) }
            }
            binding.editsDelete.setOnClickListener {
                itemClickListener.call(
                    it, bindingAdapterPosition
                ) { getItem(bindingAdapterPosition) }
            }
        }

        fun setItemData(e: SimpleEdit) {
            binding.editsTrack.text = e.track
            if (e.album.isNotBlank()) {
                binding.editsAlbum.visibility = View.VISIBLE
                binding.editsAlbum.text = e.album
            } else
                binding.editsAlbum.visibility = View.GONE
            binding.editsArtist.text = e.artist

            binding.editsImg.visibility = if (e.legacyHash != null)
                View.INVISIBLE
            else
                View.VISIBLE
        }
    }
}
