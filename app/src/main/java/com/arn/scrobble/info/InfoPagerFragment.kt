package com.arn.scrobble.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arn.scrobble.R
import com.arn.scrobble.main.BasePagerFragment
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.google.android.material.transition.MaterialSharedAxis

class InfoPagerFragment : BasePagerFragment() {

    override val optionsMenuRes = R.menu.grid_size_menu

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.Z)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = InfoPagerAdapter(this)
        super.onViewCreated(view, savedInstanceState)
    }
}