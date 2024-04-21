package com.arn.scrobble.edits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemSimpleEditBinding
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.utils.UiUtils.showWithIcons


class SimpleEditsAdapter(
    private val onItemClick: (SimpleEdit) -> Unit,
    private val onDelete: (SimpleEdit) -> Unit
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
            onItemClick,
            onDelete
        )
    }

    override fun onBindViewHolder(holder: VHSimpleEdit, position: Int) {
        holder.setItemData(getItem(position))
    }

    override fun getItemId(position: Int) =
        getItem(position)._id.toLong()


    inner class VHSimpleEdit(
        private val binding: ListItemSimpleEditBinding,
        private val onItemClick: (SimpleEdit) -> Unit,
        private val onDelete: (SimpleEdit) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.editsContent.setOnClickListener {
                onItemClick(getItem(bindingAdapterPosition))
            }
            binding.deleteMenu.setOnClickListener { v ->
                PopupMenu(v.context, v).apply {
                    inflate(R.menu.delete_menu)
                    setOnMenuItemClickListener { item ->
                        if (item.itemId == R.id.delete) {
                            onDelete(getItem(bindingAdapterPosition))
                            true
                        } else {
                            false
                        }
                    }
                    showWithIcons()
                }
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
