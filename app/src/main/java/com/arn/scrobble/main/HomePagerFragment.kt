package com.arn.scrobble.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.transition.MaterialSharedAxis


class HomePagerFragment : BasePagerFragment() {

    val prefs = App.prefs
    override val optionsMenuRes = R.menu.nav_menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Scrobblables.currentScrobblableUser == null) {
            findNavController().navigate(R.id.onboardingFragment, null, navOptions {
                popUpTo(R.id.myHomePagerFragment) { inclusive = true }
            })
            return
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.Z)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null && findNavController().currentDestination?.id == R.id.myHomePagerFragment)
            arguments = bundleOf(Stuff.ARG_TAB to prefs.lastHomePagerTab)

        adapter = HomePagerAdapter(
            this,
            Scrobblables.current?.userAccount?.type ?: AccountType.LASTFM
        )

        super.onViewCreated(view, savedInstanceState)

        collectLatestLifecycleFlow(optionsMenuViewModel.menuEvent) { (navView, menuId) ->
            optionsMenuSelected(navView, menuId)
        }
    }

    private fun optionsMenuSelected(navView: NavigationView, menuItemId: Int) {
        val navController = findNavController()
        when (menuItemId) {
            R.id.nav_rec -> navController.navigate(R.id.recFragment)
            R.id.nav_search -> navController.navigate(R.id.searchFragment)
            R.id.nav_settings -> navController.navigate(R.id.prefFragment)
            R.id.nav_help -> {
                PopupMenu(
                    requireContext(),
                    navView.findViewById(R.id.nav_help)
                ).apply {
                    inflate(R.menu.help_menu)

                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.nav_faq -> {
                                val args = Bundle().apply {
                                    putString(Stuff.ARG_URL, getString(R.string.faq_link))
                                }
                                findNavController().navigate(R.id.webViewFragment, args)
                            }

                            R.id.nav_bug_report -> {
                                BugReportUtils.mailLogs()
                            }
                        }
                        false
                    }
                    showWithIcons()
                }
            }

            R.id.nav_pro -> navController.navigate(R.id.billingFragment)
            R.id.nav_do_index -> {
                if (prefs.lastMaxIndexTime == null) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.do_index_desc)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            navController.navigate(R.id.indexingDialogFragment)
                        }
                        .show()
                } else {
                    navController.navigate(R.id.indexingDialogFragment)
                }
            }
        }
    }
}