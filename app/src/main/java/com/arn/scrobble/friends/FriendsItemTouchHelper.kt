package com.arn.scrobble.friends

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Stuff.setDragAlpha
import com.arn.scrobble.pref.MainPrefs
import java.util.*


open class FriendsItemTouchHelper(
    adapter: FriendsAdapter,
    viewModel: FriendsVM,
) : ItemTouchHelper(object : SimpleCallback(UP or DOWN or LEFT or RIGHT, 0) {

    private var changed = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition

        val isTargetPinned = (target as? FriendsAdapter.VHUser)?.isPinned ?: false

        if (!isTargetPinned) {
            return false
        }

        val pinnedFriendsList = viewModel.pinnedFriends
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(pinnedFriendsList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(pinnedFriendsList, i, i - 1)
            }
        }

        adapter.notifyItemMoved(fromPosition, toPosition)
        changed = true
        return true
    }

    override fun onSelectedChanged(
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int
    ) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ACTION_STATE_DRAG) {
            viewHolder?.setDragAlpha(true)
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
        makeMovementFlags(
            if ((viewHolder as? FriendsAdapter.VHUser)?.isPinned == true)
                UP or DOWN or LEFT or RIGHT
            else
                0,
            0
        )

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.setDragAlpha(false)

        if (changed) {
            changed = false
            viewModel.pinnedFriends.forEachIndexed { index, pinnedFriend ->
                pinnedFriend.order = index
            }
            viewModel.savePinnedFriends()
        }
    }

})