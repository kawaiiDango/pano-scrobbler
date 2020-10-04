package com.arn.scrobble.charts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.content_pager.*
import kotlinx.android.synthetic.main.content_pager.view.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*





class ChartsPagerFragment: Fragment(), TabLayout.OnTabSelectedListener {

    private val tabMeta = arrayOf(
            Pair(R.string.artists, R.drawable.vd_mic),
            Pair(R.string.albums, R.drawable.vd_album),
            Pair(R.string.tracks, R.drawable.vd_note)
    )

    private var backStackChecked = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //https://stackoverflow.com/questions/12490963/replacing-viewpager-with-fragment-then-navigating-back
        if (!view.isInTouchMode)
            view.requestFocus()
        view as ViewPager
        view.offscreenPageLimit = 2
        view.adapter = ChartsPagerAdapter(childFragmentManager, 3)
        val tabBar = activity!!.ctl.tab_bar
        tabBar.setupWithViewPager(view.pager, false)
        backStackChecked = false
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onStart() {
        super.onStart()
        val tabBar = activity!!.ctl.tab_bar
        Stuff.setTitle(activity, R.string.menu_charts)
        if (!backStackChecked) { //dont invoke if coming from background/app switching
            val lastTab = context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
                    .getInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_TAB, 0)

            tabBar.getTabAt(lastTab)?.select()
            for (i in 0 until tabBar.tabCount) {
                val tab = tabBar.getTabAt(i)!!
                if (i == lastTab)
                    tab.setText(tabMeta[i].first)
                else
                    tab.setIcon(tabMeta[i].second)
            }
            backStackChecked = true
        }
        tabBar.addOnTabSelectedListener(this)
    }

    override fun onStop() {
        val pref = context?.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
        val pager = activity?.pager
        if (pager != null && pref != null)
            pref.edit().putInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_TAB, pager.currentItem).apply()
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
        tab.text = null
        tab.setIcon(tabMeta[tab.position].second)
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        tab.setText(tabMeta[tab.position].first)
        tab.icon = null
    }
}