package com.arn.scrobble.info

import android.os.Bundle
import android.view.View
import com.arn.scrobble.BasePagerFragment
import com.arn.scrobble.R

class InfoPagerFragment : BasePagerFragment() {

    override val optionsMenuRes = R.menu.into_extra_full_menu

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = InfoPagerAdapter(this)
        super.onViewCreated(view, savedInstanceState)
    }
}