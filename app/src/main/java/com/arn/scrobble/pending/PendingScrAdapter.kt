package com.arn.scrobble.pending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ListItemRecentsBinding
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.ui.ItemClickListener

/**
 * Created by arn on 21/09/2017.
 */
class PendingScrAdapter(private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val psList = mutableListOf<PendingScrobble>()
    private val plList = mutableListOf<PendingLove>()
    var isShowingAlbums = false

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PENDING_SCROBBLE ->
                VHPendingScrobble(
                    ListItemRecentsBinding.inflate(inflater, parent, false),
                    itemClickListener,
                    isShowingAlbums
                )
            TYPE_PENDING_LOVE ->
                VHPendingLove(ListItemRecentsBinding.inflate(inflater, parent, false), itemClickListener)
            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder:RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VHPendingScrobble -> holder.setItemData(psList[position - plList.size])
            is VHPendingLove -> holder.setItemData(plList[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < plList.size -> TYPE_PENDING_LOVE
            else -> TYPE_PENDING_SCROBBLE
        }
    }

    fun addAll(pendingListData: PendingListData) {
        if (pendingListData.plCount > 0)
            plList.addAll(pendingListData.plList)
        if (pendingListData.psCount > 0)
            psList.addAll(pendingListData.psList)
        notifyDataSetChanged()
    }

    fun getPending(pos: Int): Any {
        return if (getItemViewType(pos) == TYPE_PENDING_LOVE)
            plList[pos]
        else
            psList[pos - plList.size]
    }

    fun remove(pos: Int) {
        if (getItemViewType(pos) == TYPE_PENDING_LOVE)
            plList.removeAt(pos)
        else
            psList.removeAt(pos - plList.size)
        notifyItemRemoved(pos)
    }

    fun clear() {
        plList.clear()
        psList.clear()
    }

    override fun getItemCount() = plList.size + psList.size
}

private const val TYPE_PENDING_SCROBBLE = 1
private const val TYPE_PENDING_LOVE = 2
