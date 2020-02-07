package com.arn.scrobble

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.content_pager.*
import kotlinx.android.synthetic.main.content_pager.view.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*





class PagerFragment: Fragment(), ViewPager.OnPageChangeListener, TabLayout.OnTabSelectedListener {

    private val tabMeta = arrayOf(
            Pair(R.string.recents, R.drawable.vd_history),
            Pair(R.string.loved, R.drawable.vd_heart),
            Pair(R.string.friends, R.drawable.vd_friends)
    )

    private var backStackChecked = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_pager, container, false)

        //https://stackoverflow.com/questions/12490963/replacing-viewpager-with-fragment-then-navigating-back
        if (!view.isInTouchMode)
            view.requestFocus()
        view.pager.addOnPageChangeListener(this)
        view.pager.adapter = PagerAdapter(childFragmentManager, 3)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabBar = activity!!.ctl.tab_bar
        tabBar.setupWithViewPager(view.pager, false)

        tabBar.getTabAt(0)!!.setText(tabMeta[0].first)
        for (i in 1 until tabBar.tabCount)
            tabBar.getTabAt(i)!!.setIcon(tabMeta[i].second)

//        for (i in 0 until tabBar.tabCount)
//            tabBar.getTabAt(i)!!.setIcon(tabMeta[i].second)

        tabBar.addOnTabSelectedListener(this)
        backStackChecked = false
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        if (!backStackChecked) { //dont invoke if coming from background/app switching
            if (activity!!.intent?.getIntExtra(Stuff.DEEP_LINK_KEY, 0) == 0) {
                val lastTab = context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                        .getInt(Stuff.PREF_ACTIVITY_LAST_TAB, 0)
                activity!!.ctl.tab_bar.getTabAt(lastTab)?.select()
            }
            Main.checkBackStack(activity as Main)
            backStackChecked = true
        }
    }

    override fun onStop() {
        if (userVisibleHint && activity!!.intent?.getIntExtra(Stuff.DEEP_LINK_KEY, 0) == 0) {
            val pref = context?.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
            val pager = activity?.pager
            if (pager != null && pref != null)
                pref.edit().putInt(Stuff.PREF_ACTIVITY_LAST_TAB, pager.currentItem).apply()
        }
        super.onStop()
    }

    override fun onDestroy() {
        pager?.removeOnPageChangeListener(this)
        activity?.ctl?.tab_bar?.removeOnTabSelectedListener(this)
        super.onDestroy()
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        Main.checkBackStack(activity!! as Main)
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        tab.text = null
        tab.setIcon(tabMeta[tab.position].second)
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        tab.setText(tabMeta[tab.position].first)
        tab.icon = null
    }
}