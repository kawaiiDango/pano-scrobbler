package com.arn.scrobble

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.pending.db.Edit
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.ui.ItemClickListener
import kotlinx.android.synthetic.main.content_edits.view.*
import kotlinx.android.synthetic.main.list_item_edits.view.*


class EditsAdapter(context: Context, private val fragmentContent: View) : RecyclerView.Adapter<EditsAdapter.VHEdits>() {

    private val editsList = mutableListOf<Edit>()
    private val filteredIndices = mutableListOf<Int>()
    val dao by lazy { PendingScrobblesDb.getDb(context).getEditsDao() }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    lateinit var itemClickListener: ItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHEdits {
        val inflater = LayoutInflater.from(parent.context)
        return VHEdits(inflater.inflate(R.layout.list_item_edits, parent, false), itemClickListener)
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
                    fragmentContent.edits_empty.visibility = View.VISIBLE
                    fragmentContent.edits_empty.text = fragmentContent.context.getString(R.string.not_found)
                    fragmentContent.edits_list.visibility = View.GONE
                }
            } else {
                handler.post {
                    fragmentContent.edits_empty.visibility = View.GONE
                    fragmentContent.edits_list.visibility = View.VISIBLE
                }
            }
            handler.post { notifyDataSetChanged() }
        }
    }

    fun getItem(pos: Int) = editsList[filteredIndices[pos]]

    fun remove(pos: Int) {
        editsList.removeAt(filteredIndices.removeAt(pos))
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
                    fragmentContent.edits_empty.visibility = View.VISIBLE
                    fragmentContent.edits_empty.text = fragmentContent.context.getString(R.string.n_edits, 0)
                    fragmentContent.edits_list.visibility = View.GONE
                    fragmentContent.search_term.visibility = View.GONE
                } else {
                    fragmentContent.edits_empty.visibility = View.GONE
                    fragmentContent.edits_list.visibility = View.VISIBLE
                    if (edits.size > 7)
                        fragmentContent.search_term.visibility = View.VISIBLE
                }
                notifyDataSetChanged() }
        }
    }

    fun clear() = filteredIndices.clear()

    override fun getItemCount() = filteredIndices.size

    class VHEdits(view: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(view) {
        private val vTrack = view.edits_track
        private val vAlbum = view.edits_album
        private val vArtist = view.edits_artist
        private val vDel = view.edits_delete
        private val vImg = view.edits_img

        init {
            itemView.setOnClickListener { itemClickListener.onItemClick(it, adapterPosition) }
        }

        fun setItemData(e: Edit) {
            vTrack.text = e.track
            if (e.album.isNotBlank()) {
                vAlbum.visibility = View.VISIBLE
                vAlbum.text = "(" + e.album + ")"
            } else
                vAlbum.visibility = View.GONE
            vArtist.text = e.artist
            vDel.setOnClickListener {
                itemClickListener.onItemClick(it, adapterPosition)
            }
            if (e.legacyHash != null)
                vImg.visibility = View.INVISIBLE
            else
                vImg.visibility = View.VISIBLE
        }
    }
}
