package com.arn.scrobble

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.arn.scrobble.pref.MainPrefs


class HomePagerFragment : BasePagerFragment() {

    private val prefs by lazy { MainPrefs(requireContext()) }
    override val optionsMenuRes = R.menu.nav_menu

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = HomePagerAdapter(this)

        binding.pager.addOnPageChangeListener(object : OnPageChangeListener {
            // https://stackoverflow.com/a/34675705/1067596

            var first = true

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (first && positionOffset == 0f && positionOffsetPixels == 0) {
                    onPageSelected(position)
                    first = false
                }
            }

            override fun onPageSelected(position: Int) {
                if (findNavController().currentDestination?.id == R.id.myHomePagerFragment)
                    prefs.lastHomePagerTab = position

                expandAppBar(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        }
        )

        super.onViewCreated(view, savedInstanceState)

        optionsMenuViewModel.menuEvent.observe(viewLifecycleOwner) {
            optionsMenuSelected(it)
        }


    }

    fun expandAppBar(tabPosition: Int) {
        val appBar = (activity as MainActivity).binding.appBar

        val expand = tabPosition == 0

        appBar.updateHeight(expand)

        if (expand && !appBar.isExpanded)
            appBar.setExpanded(true, true)
    }

    private fun optionsMenuSelected(menuItemId: Int) {
        val navController = findNavController()
        when (menuItemId) {
            R.id.nav_rec -> navController.navigate(R.id.recFragment)
            R.id.nav_search -> navController.navigate(R.id.searchFragment)
            R.id.nav_settings -> navController.navigate(R.id.prefFragment)
            R.id.nav_report -> BugReportUtils.mailLogs()
            R.id.nav_pro -> navController.navigate(R.id.billingFragment)
        }
    }
}