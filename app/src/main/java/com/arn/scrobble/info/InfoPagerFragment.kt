package com.arn.scrobble.info

import android.os.Bundle
import android.view.View
import com.arn.scrobble.R
import com.arn.scrobble.main.BasePagerFragment

class InfoPagerFragment : BasePagerFragment() {

    override val optionsMenuRes = R.menu.into_extra_full_menu

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = InfoPagerAdapter(this)
        super.onViewCreated(view, savedInstanceState)
    }
}