package com.arn.scrobble

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.content_pager.*


class HomePagerFragment: PagerBaseFragment(), ViewPager.OnPageChangeListener {

    private var backStackChecked = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tabMeta = arrayOf(
                Pair(R.string.recents, R.drawable.vd_history),
                Pair(R.string.loved, R.drawable.vd_heart),
                Pair(R.string.friends, R.drawable.vd_friends),
                Pair(R.string.menu_charts, R.drawable.vd_charts)
        )
        adapter = HomePagerAdapter(childFragmentManager)
        if (arguments?.getString(Stuff.ARG_USERNAME) == null)
            setGestureExclusions(true)
        backStackChecked = false
        (view as ViewPager).addOnPageChangeListener(this)
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
        if (!backStackChecked) { //dont invoke if coming from background/app switching
            val lastTab = if (arguments != null && arguments!!.getInt(Stuff.ARG_TYPE, -1) != -1)
                arguments!!.getInt(Stuff.ARG_TYPE)
            else if (arguments?.getString(Stuff.ARG_USERNAME) != null)
                (view as ViewPager).currentItem
            else if(activity!!.intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) == Stuff.DL_NOW_PLAYING)
                0
            else
                context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                        .getInt(Stuff.PREF_ACTIVITY_LAST_TAB, 0)
            initTabs(lastTab)
            (activity as Main).onBackStackChanged()
            backStackChecked = true
        }
        super.onStart()
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

    override fun onTabSelected(tab: TabLayout.Tab) {
        super.onTabSelected(tab)
        if (activity!!.intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) == 0 &&
                arguments?.getString(Stuff.ARG_USERNAME) == null) {
            context ?: return
            context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(Stuff.PREF_ACTIVITY_LAST_TAB, tab.position)
                    .apply()
        }
    }
}