package com.arn.scrobble.charts

import android.os.Bundle
import android.view.View
import com.arn.scrobble.BasePagerFragment
import com.arn.scrobble.R


class ChartsPagerFragment : BasePagerFragment() {
    override val optionsMenuRes = R.menu.charts_menu

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChartsPagerAdapter(this)
        super.onViewCreated(view, savedInstanceState)
    }
}