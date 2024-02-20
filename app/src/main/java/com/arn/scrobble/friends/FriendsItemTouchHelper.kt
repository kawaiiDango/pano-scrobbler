package com.arn.scrobble.friends

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.ui.UiUtils.setDragAlpha
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


open class FriendsItemTouchHelper(
    adapter: FriendsAdapter,
    viewModel: FriendsVM,
    viewLifecycleOwner: LifecycleOwner
) : ItemTouchHelper(object : SimpleCallback(UP or DOWN or LEFT or RIGHT, 0) {

    private var changed = false
    private lateinit var cachedList: MutableList<FriendsVM.FriendsItemHolder>

    init {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.friendsCombined.filterNotNull().collectLatest {
                cachedList = it.toMutableList()
            }
        }
    }

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

        val movedItem = cachedList.removeAt(fromPosition)
        cachedList.add(toPosition, movedItem)

//        adapter.notifyItemMoved(fromPosition, toPosition)
        adapter.submitList(cachedList)
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
            val newList = cachedList
                .filter { it.isPinned }
                .map { it.user }
                .mapIndexed { index, pinnedFriend ->
                    pinnedFriend.copy(order = index)
                }.sortedBy { it.order }
            viewModel.savePinnedFriends(newList)
        }
    }

})