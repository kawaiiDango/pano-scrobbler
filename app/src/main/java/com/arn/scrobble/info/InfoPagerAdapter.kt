package com.arn.scrobble.info


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class InfoPagerAdapter(fm: FragmentManager):
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val tabCount = 3

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> TrackExtraFragment()
            1 -> AlbumExtraFragment()
            2 -> ArtistExtraFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    override fun getCount() = tabCount
}