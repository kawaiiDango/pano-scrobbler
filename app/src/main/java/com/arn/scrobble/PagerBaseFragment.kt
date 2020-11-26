package com.arn.scrobble

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.content_pager.view.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*
import kotlinx.android.synthetic.main.nav_tab.view.*


open class PagerBaseFragment: Fragment(), TabLayout.OnTabSelectedListener {

    lateinit var tabMeta: Array<Pair<Int, Int>>
    lateinit var adapter: FragmentStatePagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //https://stackoverflow.com/questions/12490963/replacing-viewpager-with-fragment-then-navigating-back
        if (!view.isInTouchMode)
            view.requestFocus()
        view as ViewPager
        view.offscreenPageLimit = adapter.count - 1
        view.adapter = adapter
        val tabBar = activity!!.ctl.tab_bar
        tabBar.setupWithViewPager(view.pager, false)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, 0)
        activity!!.ctl.tab_bar.addOnTabSelectedListener(this)
    }

    protected fun initTabs(selectedTab: Int) {
        val tabBar = activity!!.ctl.tab_bar
        for (i in 0 until tabBar.tabCount) {
            val tab = LayoutInflater.from(context).inflate(R.layout.nav_tab, null) as LinearLayout
            tab.tab_label.text = resources.getString(tabMeta[i].first)
            tab.tab_icon.setImageResource(tabMeta[i].second)
            tab.tab_icon.imageTintList = tabBar.tabIconTint
            if (selectedTab == i) {
                tab.tab_label.visibility = View.VISIBLE
            }
            tabBar.getTabAt(i)?.customView = tab
        }
        tabBar.getTabAt(selectedTab)?.select()
    }

    override fun onStop() {
        activity!!.ctl.tab_bar.removeOnTabSelectedListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        activity?.ctl?.tab_bar?.removeOnTabSelectedListener(this)
        super.onDestroy()
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        TransitionManager.beginDelayedTransition(tab.customView as ViewGroup)
        tab.customView?.tab_label?.visibility = View.GONE
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        TransitionManager.beginDelayedTransition(tab.customView as ViewGroup)
        tab.customView?.tab_label?.visibility = View.VISIBLE
    }
}