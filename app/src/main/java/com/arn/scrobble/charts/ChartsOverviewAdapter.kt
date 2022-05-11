package com.arn.scrobble.charts

import android.transition.Fade
import android.transition.TransitionManager
import android.view.View
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.FrameChartsListBinding


class ChartsOverviewAdapter(rootViewBinding: FrameChartsListBinding) :
    ChartsAdapter(rootViewBinding) {

    override val isHorizontalList = true

    override fun populate() {
        if (viewModel.chartsData.isEmpty()) {
            if (itemCount == 0) {
                if (!Stuff.isOnline)
                    binding.chartsStatus.text =
                        binding.root.context.getString(R.string.unavailable_offline)
                else
                    binding.chartsStatus.text = binding.root.context.getString(emptyTextRes)
                TransitionManager.beginDelayedTransition(binding.root, Fade())
                binding.chartsStatus.visibility = View.VISIBLE
                binding.chartsProgress.hide()
                binding.chartsList.visibility = View.INVISIBLE
            }
        } else {
            if (binding.chartsList.visibility != View.VISIBLE) {
                TransitionManager.beginDelayedTransition(binding.root, Fade())
                binding.chartsList.visibility = View.VISIBLE
            }

            binding.chartsStatus.visibility = View.GONE
            binding.chartsProgress.hide()
        }
        notifyDataSetChanged()
    }

}
