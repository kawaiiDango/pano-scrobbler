package com.arn.scrobble.edits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ListItemSimpleEditBinding
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.ItemClickListener


class SimpleEditsAdapter(
    private val viewModel: SimpleEditsVM,
    private val itemClickListener: ItemClickListener,
 ) : RecyclerView.Adapter<SimpleEditsAdapter.VHSimpleEdit>() {

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHSimpleEdit {
        val inflater = LayoutInflater.from(parent.context)
        return VHSimpleEdit(ListItemSimpleEditBinding.inflate(inflater, parent, false), itemClickListener)
    }

    override fun onBindViewHolder(holder:VHSimpleEdit, position: Int) {
        holder.setItemData(viewModel.edits[position])
    }

    override fun getItemId(position: Int) = viewModel.edits[position]._id.toLong()

    override fun getItemCount() = viewModel.edits.size


    fun tempUpdate(pos: Int, simpleEdit: SimpleEdit) {
        viewModel.edits[pos] = simpleEdit
        notifyItemChanged(pos)
    }

    class VHSimpleEdit(private val binding: ListItemSimpleEditBinding, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { itemClickListener.call(it, bindingAdapterPosition) }
        }

        fun setItemData(e: SimpleEdit) {
            binding.editsTrack.text = e.track
            if (e.album.isNotBlank()) {
                binding.editsAlbum.visibility = View.VISIBLE
                binding.editsAlbum.text = e.album
            } else
                binding.editsAlbum.visibility = View.GONE
            binding.editsArtist.text = e.artist
            binding.editsDelete.setOnClickListener {
                itemClickListener.call(it, bindingAdapterPosition)
            }
            if (e.legacyHash != null)
                binding.editsImg.visibility = View.INVISIBLE
            else
                binding.editsImg.visibility = View.VISIBLE
        }
    }
}
