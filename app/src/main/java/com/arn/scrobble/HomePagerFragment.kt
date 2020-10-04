package com.arn.scrobble

import android.content.Context
import android.graphics.Rect
import android.os.Build
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





class HomePagerFragment: Fragment(), ViewPager.OnPageChangeListener, TabLayout.OnTabSelectedListener {

    private val tabMeta = arrayOf(
            Pair(R.string.recents, R.drawable.vd_history),
            Pair(R.string.loved, R.drawable.vd_heart),
            Pair(R.string.friends, R.drawable.vd_friends)
    )

    private var backStackChecked = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //https://stackoverflow.com/questions/12490963/replacing-viewpager-with-fragment-then-navigating-back
        if (!view.isInTouchMode)
            view.requestFocus()
        (view as ViewPager).addOnPageChangeListener(this)
        view.offscreenPageLimit = 2
        view.adapter = HomePagerAdapter(childFragmentManager, 3)
        val tabBar = activity!!.ctl.tab_bar
        tabBar.setupWithViewPager(view.pager, false)
        backStackChecked = false
        setGestureExclusions(true)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        setGestureExclusions(false)
        super.onDestroyView()
    }

    fun setGestureExclusions(set: Boolean){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !resources.getBoolean(R.bool.is_rtl)) {
            val list = if (set)
                listOf(Rect(0, 0, 100, resources.displayMetrics.heightPixels))
            else
                listOf()
            view?.systemGestureExclusionRects = list
            //fuck gestures
        }
    }

    override fun onStart() {
        super.onStart()
        val tabBar = activity!!.ctl.tab_bar
        if (!backStackChecked) { //dont invoke if coming from background/app switching
            val lastTab = if (activity!!.intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) == Stuff.DL_NOW_PLAYING)
                0
            else
                context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                        .getInt(Stuff.PREF_ACTIVITY_LAST_TAB, 0)
            tabBar.getTabAt(lastTab)?.select()
            for (i in 0 until tabBar.tabCount) {
                val tab = tabBar.getTabAt(i)!!
                if (i == lastTab)
                    tab.setText(tabMeta[i].first)
                else
                    tab.setIcon(tabMeta[i].second)
            }
            (activity as Main).onBackStackChanged()
            backStackChecked = true
        }
        tabBar.addOnTabSelectedListener(this)
    }

    override fun onStop() {
        activity?.ctl?.tab_bar?.removeOnTabSelectedListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        pager?.removeOnPageChangeListener(this)
        super.onDestroy()
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        (activity as Main).onBackStackChanged()
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
        if (activity!!.intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) == 0) {
            context ?: return
            context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(Stuff.PREF_ACTIVITY_LAST_TAB, tab.position)
                    .apply()
        }
    }
}