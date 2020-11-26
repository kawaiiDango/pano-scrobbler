package com.arn.scrobble


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.arn.scrobble.charts.ChartsOverviewFragment

class HomePagerAdapter(fm: FragmentManager):
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val tabCount = 4

    //Overriding method getItem
    override fun getItem(position: Int): Fragment {
        //Returning the current tabs
        return when (position) {
            0 -> RecentsFragment()
            1 -> LovesFragment()
            2 -> FriendsFragment()
            else -> ChartsOverviewFragment()
        }
    }

    override fun getCount() = tabCount
}