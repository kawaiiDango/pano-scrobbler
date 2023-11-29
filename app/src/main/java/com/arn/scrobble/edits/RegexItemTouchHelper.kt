package com.arn.scrobble.edits

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.ui.UiUtils.setDragAlpha
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch


class RegexItemTouchHelper(
    adapter: RegexEditsAdapter,
    viewModel: RegexEditsVM,
    viewLifecycleOwner: LifecycleOwner
) : ItemTouchHelper(object : SimpleCallback(0, UP or DOWN) {

    private var changed = false
    private lateinit var cachedList: MutableList<RegexEdit>

    init {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.regexes.filterNotNull().collectLatest {
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

        val movedItem = cachedList.removeAt(fromPosition)
        cachedList.add(toPosition, movedItem)

        viewModel.tmpUpdateAll(cachedList)
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
            UP or DOWN,
            0
        )

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.setDragAlpha(false)
        if (changed) {
            changed = false
            val regexes = cachedList.mapIndexed { index, regex ->
                regex.copy(order = index)
            }
            viewModel.upsertAll(regexes)
        }
    }
})