package com.arn.scrobble.charts

import android.os.Bundle
import android.view.View
import com.arn.scrobble.PagerBaseFragment
import com.arn.scrobble.R
import com.arn.scrobble.Stuff


class ChartsPagerFragment : PagerBaseFragment() {

    private val typeToTab =
        mapOf(Stuff.TYPE_ARTISTS to 0, Stuff.TYPE_ALBUMS to 1, Stuff.TYPE_TRACKS to 2)
    private var backStackChecked = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        backStackChecked = false
        tabMeta = arrayOf(
            R.string.artists to R.drawable.vd_mic,
            R.string.albums to R.drawable.vd_album,
            R.string.tracks to R.drawable.vd_note
        )
        adapter = ChartsPagerAdapter(childFragmentManager)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        if (!backStackChecked) {
            val tabIdx = typeToTab[arguments?.getInt(Stuff.ARG_TYPE)] ?: 0
            initTabs(tabIdx)
            backStackChecked = true
        }
        super.onStart()
    }
}