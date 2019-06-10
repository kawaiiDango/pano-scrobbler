package com.arn.scrobble


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class PagerAdapter(fm: FragmentManager, private var tabCount: Int): FragmentStatePagerAdapter(fm) {

    //Overriding method getItem
    override fun getItem(position: Int): Fragment {
        //Returning the current tabs
        return when (position) {
            0 -> RecentsFragment()
            1 -> LovesFragment()
            else -> FriendsFragment()
//            else -> null
        }
    }

    override fun getCount(): Int {
        return tabCount
    }
}