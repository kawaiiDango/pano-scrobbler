package com.arn.scrobble.info

import android.os.Bundle
import android.view.View
import com.arn.scrobble.PagerBaseFragment
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.google.android.material.transition.MaterialSharedAxis


class InfoPagerFragment : PagerBaseFragment() {

    private val typeToTab =
        mapOf(Stuff.TYPE_ARTISTS to 2, Stuff.TYPE_ALBUMS to 1, Stuff.TYPE_TRACKS to 0)
    private var backStackChecked = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        backStackChecked = false
        tabMeta = arrayOf(
            R.string.top_tracks to R.drawable.vd_note,
            R.string.top_albums to R.drawable.vd_album,
            R.string.similar_artists to R.drawable.vd_mic
        )
        adapter = InfoPagerAdapter(childFragmentManager)
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