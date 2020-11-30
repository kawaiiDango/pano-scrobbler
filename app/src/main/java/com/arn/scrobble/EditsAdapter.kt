package com.arn.scrobble

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ContentEditsBinding
import com.arn.scrobble.databinding.ListItemEditsBinding
import com.arn.scrobble.pending.db.Edit
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.ui.ItemClickListener


class EditsAdapter(context: Context, private val fragmentBinding: ContentEditsBinding) : RecyclerView.Adapter<EditsAdapter.VHEdits>() {

    private val editsList = mutableListOf<Edit>()
    private val filteredIndices = mutableListOf<Int>()
    val dao by lazy { PendingScrobblesDb.getDb(context).getEditsDao() }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    lateinit var itemClickListener: ItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHEdits {
        val inflater = LayoutInflater.from(parent.context)
        return VHEdits(ListItemEditsBinding.inflate(inflater, parent, false), itemClickListener)
    }

    override fun onBindViewHolder(holder:VHEdits, position: Int) {
        holder.setItemData(editsList[filteredIndices[position]])
    }

    fun filter(query: String) {
        val query = query.toLowerCase().trim()
        AsyncTask.SERIAL_EXECUTOR.execute {
            if (query.isEmpty()) {
                filteredIndices.clear()
                filteredIndices.addAll(editsList.indices)
            } else {
                val tmpList = mutableListOf<Int>()
                for (i in 0 until editsList.size) {
                    val it = editsList[i]
                    if (it.artist.toLowerCase().startsWith(query) ||
                            it.albumArtist.toLowerCase().startsWith(query) ||
                            it.album.toLowerCase().startsWith(query) ||
                            it.track.toLowerCase().startsWith(query))
                       tmpList += i
                }
                filteredIndices.clear()
                filteredIndices.addAll(tmpList)
            }

            if (filteredIndices.isEmpty()) {
                handler.post {
                    fragmentBinding.editsEmpty.visibility = View.VISIBLE
                    fragmentBinding.editsEmpty.text = fragmentBinding.root.context.getString(R.string.not_found)
                    fragmentBinding.editsList.visibility = View.GONE
                }
            } else {
                handler.post {
                    fragmentBinding.editsEmpty.visibility = View.GONE
                    fragmentBinding.editsList.visibility = View.VISIBLE
                }
            }
            handler.post { notifyDataSetChanged() }
        }
    }

    fun getItem(pos: Int) = editsList[filteredIndices[pos]]

    fun remove(pos: Int) {
        editsList.removeAt(filteredIndices.removeAt(pos))
        for (i in pos until filteredIndices.size)
            filteredIndices[i]--
        notifyItemRemoved(pos)
    }

    fun update(pos: Int, e: Edit) {
        editsList[filteredIndices[pos]] = e
        notifyItemChanged(pos)
    }

    fun loadAll() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val edits = dao.all.reversed()
            editsList.addAll(edits)
            filteredIndices.addAll(edits.indices)
            handler.post {
                if (editsList.isEmpty()){
                    fragmentBinding.editsEmpty.visibility = View.VISIBLE
                    fragmentBinding.editsEmpty.text = fragmentBinding.root.context.getString(R.string.n_edits, 0)
                    fragmentBinding.editsList.visibility = View.GONE
                    fragmentBinding.searchTerm.visibility = View.GONE
                } else {
                    fragmentBinding.editsEmpty.visibility = View.GONE
                    fragmentBinding.editsList.visibility = View.VISIBLE
                    if (edits.size > 7)
                        fragmentBinding.searchTerm.visibility = View.VISIBLE
                }
                notifyDataSetChanged() }
        }
    }

    fun clear() = filteredIndices.clear()

    override fun getItemCount() = filteredIndices.size

    class VHEdits(private val binding: ListItemEditsBinding, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { itemClickListener.onItemClick(it, adapterPosition) }
        }

        fun setItemData(e: Edit) {
            binding.editsTrack.text = e.track
            if (e.album.isNotBlank()) {
                binding.editsAlbum.visibility = View.VISIBLE
                binding.editsAlbum.text = "(" + e.album + ")"
            } else
                binding.editsAlbum.visibility = View.GONE
            binding.editsArtist.text = e.artist
            binding.editsDelete.setOnClickListener {
                itemClickListener.onItemClick(it, adapterPosition)
            }
            if (e.legacyHash != null)
                binding.editsImg.visibility = View.INVISIBLE
            else
                binding.editsImg.visibility = View.VISIBLE
        }
    }
}
