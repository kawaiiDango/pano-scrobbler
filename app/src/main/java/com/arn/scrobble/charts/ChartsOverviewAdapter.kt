package com.arn.scrobble.charts

import android.view.View
import com.arn.scrobble.Main
import com.arn.scrobble.R
import kotlinx.android.synthetic.main.frame_charts_list.view.*


class ChartsOverviewAdapter(rootView: View): ChartsAdapter(rootView) {
    
    override val forceDimensions = true

    override fun populate(){
        if (viewModel.chartsData.isEmpty()) {
            if (itemCount == 0) {
                if (!Main.isOnline)
                    fragmentContent.charts_status?.text = fragmentContent.context.getString(R.string.unavailable_offline)
                else
                    fragmentContent.charts_status?.text = fragmentContent.context.getString(emptyTextRes)
                fragmentContent.charts_status?.visibility = View.VISIBLE
                fragmentContent.charts_progress?.visibility = View.GONE
            }
        } else {
            fragmentContent.charts_status?.visibility = View.GONE
            fragmentContent.charts_progress?.visibility = View.GONE
        }

        notifyDataSetChanged()
    }

}
