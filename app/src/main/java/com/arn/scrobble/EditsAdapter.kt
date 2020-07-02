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
import kotlinx.android.synthetic.main.list_item_edits.view.*

/**
 * Created by arn on 21/09/2017.
 */
class EditsAdapter(context: Context) : RecyclerView.Adapter<EditsAdapter.VHEdits>(), ItemClickListener {
    private val editsList = mutableListOf<Edit>()
    private val dao by lazy { PendingScrobblesDb.getDb(context).getEditsDao() }
    private val emptyText = context.getString(R.string.n_edits, 0)
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHEdits {
        val inflater = LayoutInflater.from(parent.context)
        return VHEdits(inflater.inflate(R.layout.list_item_edits, parent, false), this)
    }

    override fun onBindViewHolder(holder:VHEdits, position: Int) {
        holder.setItemData(editsList[position])
    }

    fun loadAll() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val edits = dao.all.reversed()
            editsList.addAll(edits)
            handler.post {
                if (editsList.isEmpty()){
                    val e = Edit()
                    e.track = emptyText
                    editsList.add(e)
                }
                notifyDataSetChanged() }
        }
    }

    fun clear() = editsList.clear()

    override fun getItemCount() = editsList.size

    override fun onItemClick(view: View, position: Int) {
        if (position != -1) {
            //needed if the user quickly taps on the same item before the animation is done
            val item = editsList.removeAt(position)
            notifyItemRemoved(position)
            AsyncTask.THREAD_POOL_EXECUTOR.execute {
                dao.delete(item)
            }
        }
    }

    class VHEdits(view: View, private val itemClickListener: ItemClickListener) : RecyclerView.ViewHolder(view) {
        private val vTrack = view.edits_track
        private val vAlbum = view.edits_album
        private val vArtist = view.edits_artist
        private val vDel = view.edits_delete

        fun setItemData(e: Edit) {
            vTrack.text = e.track
            if (e.album.isNotBlank())
                vAlbum.text = "("+e.album+")"
            else
                vAlbum.text = ""
            vArtist.text = e.artist
            if (e.artist == "")
                vDel.visibility = View.INVISIBLE
            else
                vDel.setOnClickListener {
                    val pos = adapterPosition
                    itemClickListener.onItemClick(it, pos)
                }
        }
    }
}
