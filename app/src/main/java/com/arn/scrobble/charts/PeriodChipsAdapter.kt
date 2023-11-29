package com.arn.scrobble.charts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ListItemChipPeriodBinding
import com.google.android.material.chip.Chip

class PeriodChipsAdapter(
    private val viewModel: BaseChartsVM,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PeriodChipsAdapter.PeriodChipVH>() {

    var periodSelectedIdx = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodChipVH {
        val inflater = LayoutInflater.from(parent.context)
        val chip = ListItemChipPeriodBinding.inflate(inflater, parent, false).root.apply {
            id = View.generateViewId()
        }
        return PeriodChipVH(chip)
    }

    override fun onBindViewHolder(holder: PeriodChipVH, position: Int) {
        holder.setData(viewModel.timePeriods.value[position]!!)
    }

    override fun getItemCount() = viewModel.timePeriods.value.size

    fun resetSelection() {
        periodSelectedIdx = -1
    }

    fun refreshSelection(position: Int) {
        if (position in 0 until itemCount) {
            notifyItemChanged(periodSelectedIdx)
            periodSelectedIdx = position
            notifyItemChanged(position)
        }
    }

    inner class PeriodChipVH(private val chip: Chip) : RecyclerView.ViewHolder(chip) {

        init {
            chip.setOnClickListener {
                refreshSelection(absoluteAdapterPosition)
                onClick(absoluteAdapterPosition)
                viewModel.timePeriods.value[absoluteAdapterPosition]?.let {
                    viewModel.setSelectedPeriod(it)
                }
            }
        }

        fun setData(timePeriod: TimePeriod) {
            chip.text = timePeriod.name
            val checkedFuture = timePeriod == viewModel.selectedPeriod.value
            if (checkedFuture)
                periodSelectedIdx = absoluteAdapterPosition
            if (chip.isChecked != checkedFuture)
                chip.isChecked = checkedFuture
        }
    }

}