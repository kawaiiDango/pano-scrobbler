package com.arn.scrobble

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController


class HomePagerFragment : BasePagerFragment() {

    val prefs = App.prefs
    override val optionsMenuRes = R.menu.nav_menu

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = HomePagerAdapter(this)

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