package com.arn.scrobble.pending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.pending.db.PendingScrobble
import com.arn.scrobble.ui.VHPending

/**
 * Created by arn on 21/09/2017.
 */
class PendingScrAdapter : RecyclerView.Adapter<VHPending>() {
    private val pendingList = mutableListOf<PendingScrobble>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHPending {
        val inflater = LayoutInflater.from(parent.context)
        return VHPending(inflater.inflate(R.layout.list_item_recents, parent, false))
    }

    override fun onBindViewHolder(holder:VHPending, position: Int) {
        holder.setItemData(pendingList[position])
    }

    fun addAll(ps: List<PendingScrobble>) {
        pendingList.addAll(ps)
        notifyDataSetChanged()
    }

    fun clear() = pendingList.clear()

    override fun getItemCount() = pendingList.size
}
