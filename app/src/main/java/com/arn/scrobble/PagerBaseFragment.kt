package com.arn.scrobble

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.arn.scrobble.databinding.ContentPagerBinding
import com.arn.scrobble.databinding.NavTabBinding
import com.arn.scrobble.ui.UiUtils.setTitle
import com.google.android.material.tabs.TabLayout


open class PagerBaseFragment : Fragment(), TabLayout.OnTabSelectedListener {

    lateinit var tabMeta: Array<Pair<Int, Int>>
    lateinit var adapter: FragmentStatePagerAdapter
    protected var _binding: ContentPagerBinding? = null
    protected val binding
        get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ContentPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //https://stackoverflow.com/questions/12490963/replacing-viewpager-with-fragment-then-navigating-back
        if (!view.isInTouchMode)
            view.requestFocus()
        binding.pager.offscreenPageLimit = adapter.count - 1
        binding.pager.adapter = adapter
        val tabBar = (activity as MainActivity).binding.coordinatorMain.tabBar
        tabBar.setupWithViewPager(binding.pager, false)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        setTitle(0)
        (activity as MainActivity).binding.coordinatorMain.tabBar.addOnTabSelectedListener(this)
    }

    protected fun initTabs(selectedTab: Int) {
        val tabBar = (activity as MainActivity).binding.coordinatorMain.tabBar
        for (i in 0 until tabBar.tabCount) {
            val tabBinding = NavTabBinding.inflate(layoutInflater)
            tabBinding.tabLabel.text = resources.getString(tabMeta[i].first)
            tabBinding.tabIcon.setImageResource(tabMeta[i].second)
            tabBinding.tabIcon.imageTintList = tabBar.tabIconTint
            if (selectedTab == i) {
                tabBinding.tabLabel.visibility = View.VISIBLE
            }
            tabBar.getTabAt(i)?.customView = tabBinding.root
        }
        tabBar.getTabAt(selectedTab)?.select()
    }

    override fun onStop() {
        (activity as MainActivity).binding.coordinatorMain.tabBar.removeOnTabSelectedListener(this)
        super.onStop()
    }

    override fun onDestroy() {
        (activity as MainActivity?)?.binding?.coordinatorMain?.tabBar?.removeOnTabSelectedListener(
            this
        )
        super.onDestroy()
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        TransitionManager.beginDelayedTransition(
            tab.customView as ViewGroup,
            AutoTransition()
                .setDuration(200)
                .setOrdering(AutoTransition.ORDERING_TOGETHER)
        )
        tab.customView!!.findViewById<TextView>(R.id.tab_label)?.visibility = View.GONE
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        TransitionManager.beginDelayedTransition(
            tab.customView as ViewGroup,
            AutoTransition()
                .setDuration(200)
                .setOrdering(AutoTransition.ORDERING_TOGETHER)
        )
        tab.customView!!.findViewById<TextView>(R.id.tab_label)?.visibility = View.VISIBLE
    }
}