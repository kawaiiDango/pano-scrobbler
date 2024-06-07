package com.arn.scrobble.charts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arn.scrobble.R
import com.arn.scrobble.main.BasePagerFragment
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.google.android.material.transition.MaterialSharedAxis


class ChartsPagerFragment : BasePagerFragment() {
    override val optionsMenuRes = if (Stuff.isTv)
        R.menu.grid_size_menu
    else
        R.menu.charts_menu


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChartsPagerAdapter(this)
        super.onViewCreated(view, savedInstanceState)
    }
}