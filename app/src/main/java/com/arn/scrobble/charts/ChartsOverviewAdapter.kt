package com.arn.scrobble.charts

import android.view.View
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.databinding.FrameChartsListBinding


class ChartsOverviewAdapter(rootViewBinding: FrameChartsListBinding): ChartsAdapter(rootViewBinding) {
    
    override val forceDimensions = true
    override val roundCorners = true

    override fun populate(){
        if (viewModel.chartsData.isEmpty()) {
            if (itemCount == 0) {
                if (!Main.isOnline)
                    binding.chartsStatus.text = binding.root.context.getString(R.string.unavailable_offline)
                else
                    binding.chartsStatus.text = binding.root.context.getString(emptyTextRes)
                binding.chartsStatus.visibility = View.VISIBLE
                binding.chartsProgress.visibility = View.GONE
            }
        } else {
            binding.chartsStatus.visibility = View.GONE
            binding.chartsProgress.visibility = View.GONE
        }

        notifyDataSetChanged()
    }

}
