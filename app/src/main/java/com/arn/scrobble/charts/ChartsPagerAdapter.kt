package com.arn.scrobble.charts


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class ChartsPagerAdapter(fm: FragmentManager, private var tabCount: Int):
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> ArtistChartsFragment()
            1 -> AlbumChartsFragment()
            else -> TrackChartsFragment()
        }
    }

    override fun getCount() = tabCount
}