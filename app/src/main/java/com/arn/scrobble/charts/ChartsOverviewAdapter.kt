package com.arn.scrobble.charts

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.databinding.FrameChartsListBinding
import com.arn.scrobble.utils.Stuff


class ChartsOverviewAdapter(
    lifecycleOwner: LifecycleOwner,
    rootViewBinding: FrameChartsListBinding
) :
    ChartsAdapter(lifecycleOwner, rootViewBinding) {

    override val isHorizontalList = true

    override fun populate(newList: List<MusicEntry>, scrollToFirst: Boolean) {
        progressVisible(false)

        submitList(newList) {
            if (scrollToFirst)
                binding.chartsList.scrollToPosition(0)
        }

        if (newList.isEmpty()) {
            if (!Stuff.isOnline)
                binding.chartsStatus.text =
                    binding.root.context.getString(R.string.unavailable_offline)
            else
                binding.chartsStatus.text = binding.root.context.getString(emptyTextRes)
            TransitionManager.beginDelayedTransition(binding.root, Fade())
            binding.chartsStatus.visibility = View.VISIBLE
        } else {
            if (binding.chartsList.visibility != View.VISIBLE) {
                TransitionManager.beginDelayedTransition(binding.root, Fade())
            }

            binding.chartsStatus.visibility = View.GONE
        }
    }

}
