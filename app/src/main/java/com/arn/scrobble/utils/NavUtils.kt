package com.arn.scrobble.utils

import android.animation.ObjectAnimator
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuItemCompat
import androidx.core.view.children
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import coil.load
import com.arn.scrobble.App
import com.arn.scrobble.BasePagerFragment
import com.arn.scrobble.HomePagerFragment
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.OptionsMenuMetadata
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.databinding.HeaderNavBinding
import com.arn.scrobble.ui.InitialsDrawable
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.arn.scrobble.ui.UiUtils.slide
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.getSingle
import com.arn.scrobble.utils.Stuff.putSingle
import kotlinx.coroutines.launch


object NavUtils {

    fun updateHeaderWithDrawerData(
        headerNavBinding: HeaderNavBinding,
        mainNotifierViewModel: MainNotifierViewModel
    ) {
        val accountType = Scrobblables.current?.userAccount?.type

        if (accountType == null || mainNotifierViewModel.drawerData.value == null) {
            headerNavBinding.root.visibility = View.INVISIBLE
            return
        } else {
            headerNavBinding.root.visibility = View.VISIBLE
        }

        val currentUser = mainNotifierViewModel.currentUser
        val username = currentUser.name
        val navNumEntriesList = listOf(
            headerNavBinding.navNumArtists,
            headerNavBinding.navNumAlbums,
            headerNavBinding.navNumTracks
        )

        val displayText = when {
            App.prefs.demoMode -> "nobody"
            accountType == AccountType.LASTFM -> username
            else -> Scrobblables.getString(accountType) + ": " + username
        }

        headerNavBinding.navName.text = displayText
        val drawerData = mainNotifierViewModel.drawerData.value!!

        if (drawerData.scrobblesToday >= 0) {
            headerNavBinding.navNumScrobblesToday.isVisible = true
            headerNavBinding.navNumScrobblesToday.text =
                App.context.resources.getQuantityString(
                    R.plurals.num_scrobbles_today,
                    drawerData.scrobblesToday,
                    drawerData.scrobblesToday.format()
                )
        } else {
            headerNavBinding.navNumScrobblesToday.isVisible = false
        }

        if (drawerData.scrobblesTotal > 0) {
            headerNavBinding.navNumScrobblesTotal.isVisible = true
            headerNavBinding.navNumScrobblesTotal.text = drawerData.scrobblesTotal.format()
        } else {
            headerNavBinding.navNumScrobblesTotal.isVisible = false
        }

        if (drawerData.artistCount >= 0) {
            navNumEntriesList.forEach { it.isVisible = true }
            headerNavBinding.navNumArtists.text = drawerData.artistCount.format()
            headerNavBinding.navNumAlbums.text = drawerData.albumCount.format()
            headerNavBinding.navNumTracks.text = drawerData.trackCount.format()
        } else {
            navNumEntriesList.forEach { it.isVisible = false }
        }

        val profilePicUrl =
            if (mainNotifierViewModel.currentUser.isSelf)
                drawerData.profilePicUrl
            else
                currentUser.largeImage
        if (headerNavBinding.navProfilePic.getTag(R.id.img_url) != profilePicUrl + username) // todo prevent flash
            headerNavBinding.navProfilePic.load(profilePicUrl) {
                allowHardware(false)
                error(
                    InitialsDrawable(
                        headerNavBinding.root.context,
                        username,
                        colorFromHash = false
                    )
                )
                listener(
                    onSuccess = { _, _ ->
                        headerNavBinding.navProfilePic.setTag(
                            R.id.img_url,
                            profilePicUrl + username
                        )
                    },
                    onError = { _, _ ->
                        headerNavBinding.navProfilePic.setTag(
                            R.id.img_url,
                            profilePicUrl + username
                        )
                    }
                )
            }
    }

    fun setProfileSwitcher(
        headerNavBinding: HeaderNavBinding,
        navController: NavController,
        mainNotifierViewModel: MainNotifierViewModel
    ) {
        headerNavBinding.navProfileLinks.setOnClickListener { anchor ->
            val currentAccount = Scrobblables.current?.userAccount ?: return@setOnClickListener
            val currentUser = mainNotifierViewModel.currentUser

            val prefs = App.prefs
            val popup = PopupMenu(headerNavBinding.root.context, anchor)

            popup.menu.add(1, -2, 0, R.string.profile)
                .apply { setIcon(R.drawable.vd_open_in_new) }
            popup.menu.add(1, -1, 0, R.string.reports)
                .apply { setIcon(R.drawable.vd_open_in_new) }

            if (mainNotifierViewModel.currentUser.isSelf) {
                Scrobblables.all.forEachIndexed { idx, it ->
                    if (it != Scrobblables.current)
                        popup.menu.add(
                            2,
                            idx,
                            0,
                            Scrobblables.getString(it.userAccount.type) + ": " + it.userAccount.user.name
                        ).apply { setIcon(R.drawable.vd_swap_horiz) }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                popup.menu.setGroupDividerEnabled(true)
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    -2 -> Stuff.openInBrowser(currentUser.url)
                    -1 -> {
                        when (currentAccount.type) {
                            AccountType.LASTFM -> Stuff.openInBrowser("https://www.last.fm/user/${currentUser.name}/listening-report/week")
                            AccountType.LIBREFM -> Stuff.openInBrowser("https://libre.fm/user/${currentUser.name}/stats")
                            AccountType.GNUFM -> Stuff.openInBrowser("${currentAccount.apiRoot}/stats")
                            AccountType.LISTENBRAINZ -> Stuff.openInBrowser("https://listenbrainz.org/user/${currentUser.name}/reports")
                            AccountType.CUSTOM_LISTENBRAINZ -> Stuff.openInBrowser("${currentAccount.apiRoot}user/${currentUser.name}/reports")
                        }
                    }

                    else -> {
                        val changed = prefs.currentAccountIdx != menuItem.itemId
                        if (changed) {
                            prefs.currentAccountIdx = menuItem.itemId
                            setProfileSwitcher(
                                headerNavBinding,
                                navController,
                                mainNotifierViewModel
                            )

                            mainNotifierViewModel.clearDrawerData()
                            navController.popBackStack(R.id.myHomePagerFragment, true)
                            navController.navigate(R.id.myHomePagerFragment)

//                            mainNotifierViewModel.loadCurrentUserDrawerData()
                        }
                    }
                }
                true
            }
            popup.showWithIcons()
        }
    }

    fun BasePagerFragment.scrollToTop() {
        val currentFragment = adapter.instantiateItem(
            binding.pager,
            binding.pager.currentItem
        ) as Fragment
        val scrollableView = currentFragment.view
            ?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
            ?.get(0)

        if (scrollableView is RecyclerView)
            scrollableView.smoothScrollToPosition(0)
        else if (scrollableView is ConstraintLayout) // for frame_charts_list
            (scrollableView.children.find { it is RecyclerView } as? RecyclerView)
                ?.smoothScrollToPosition(0)
        else if (scrollableView != null)
            ObjectAnimator.ofInt(scrollableView, "scrollY", 0).start()
    }

    fun BasePagerFragment.setupWithNavUi() {
        val activityBinding = (activity as? MainActivity)?.binding ?: return
        val menus = listOf(
            activityBinding.bottomNav.menu,
            activityBinding.sidebarNav.menu
        )

        // hack to prevent a visual glitch
        // todo actually make it work
        val idOffset = 10 // optionsMenuViewModel.hashCode()
        val activityViewModel by activityViewModels<MainNotifierViewModel>()

        if (this is HomePagerFragment) {
            activityViewModel.currentUser =
                arguments?.getSingle() ?: Scrobblables.currentScrobblableUser!!
        }

        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        source.lifecycleScope.launch {
                            activityViewModel.destroyEventPending.acquire()
                            val shouldShowUser = this@setupWithNavUi is HomePagerFragment

                            adapter.tabMetadata.forEachIndexed { index, metadata ->
                                menus.forEach {
                                    it.add(0, index + idOffset, 0, metadata.titleRes).apply {
                                        setIcon(metadata.iconRes)
                                        isCheckable = true
                                    }
                                }
                            }

                            if (optionsMenuRes != 0) {
                                activityBinding.bottomNav.setTag(
                                    R.id.should_show_user,
                                    shouldShowUser
                                )

                                val moreMenu = activityBinding.bottomNav.menu.add(
                                    0,
                                    R.id.more_menu + idOffset,
                                    0,
                                    R.string.more
                                )
                                moreMenu.isCheckable = false
                                if (shouldShowUser) {
                                    if (App.prefs.demoMode) {
                                        moreMenu.setIcon(R.drawable.vd_user)
                                    } else {
                                        UiUtils.loadSmallUserPic(
                                            activityBinding.bottomNav.context,
                                            mainNotifierViewModel.currentUser,
                                        ) {
                                            it.colorFilter =
                                                ColorMatrixColorFilter(ColorMatrix().apply {
                                                    setSaturation(0f)
                                                })
                                            MenuItemCompat.setIconTintMode(
                                                moreMenu,
                                                PorterDuff.Mode.DST
                                            )
                                            moreMenu.icon = it
                                        }
                                    }
                                } else {
                                    moreMenu.setIcon(R.drawable.vd_more_horiz)
                                }
                                activityBinding.sidebarNav.inflateMenu(optionsMenuRes)
                            }

                            activityBinding.bottomNav.setOnItemSelectedListener { menuItem ->

                                activityBinding.appBar.setTag(
                                    R.id.app_bar_can_change_size,
                                    true
                                )

                                val itemId = menuItem.itemId - idOffset
                                if (itemId in adapter.tabMetadata.indices) {
                                    binding.pager.setCurrentItem(itemId, true)
                                    true
                                } else if (itemId == R.id.more_menu) {

                                    val args = Bundle().putSingle(
                                        OptionsMenuMetadata(
                                            optionsMenuRes,
                                            activityBinding.bottomNav.getTag(R.id.should_show_user) as? Boolean
                                                ?: false
                                        ),
                                    )
                                    findNavController().navigate(
                                        R.id.optionsMenuDialogFragment,
                                        args
                                    )
                                    false
                                } else
                                    false
                            }

                            activityBinding.bottomNav.setOnItemReselectedListener {
                                scrollToTop()
                            }

                            activityBinding.sidebarNav.setNavigationItemSelectedListener { menuItem ->
                                val page = menuItem.itemId - idOffset

                                if (page in adapter.tabMetadata.indices) {
                                    menuItem.isChecked = true
                                    val reselected = page == binding.pager.currentItem
                                    if (reselected)
                                        scrollToTop()
                                    else
                                        binding.pager.setCurrentItem(page, true)
                                    true
                                } else {
                                    optionsMenuViewModel.onMenuItemSelected(
                                        activityBinding.sidebarNav,
                                        menuItem.itemId
                                    )
                                    false
                                }
                            }

                            val onPageChangeListener = object : OnPageChangeListener {

                                override fun onPageScrolled(
                                    position: Int,
                                    positionOffset: Float,
                                    positionOffsetPixels: Int
                                ) {
                                }

                                override fun onPageSelected(position: Int) {
                                    if (position in adapter.tabMetadata.indices) {
                                        activityBinding.bottomNav.selectedItemId =
                                            idOffset + position
                                        activityBinding.sidebarNav.setCheckedItem(idOffset + position)

                                        if (this@setupWithNavUi is HomePagerFragment) {
                                            if (findNavController().currentDestination?.id == R.id.myHomePagerFragment)
                                                prefs.lastHomePagerTab = position
                                        }
                                    }
                                }

                                override fun onPageScrollStateChanged(state: Int) {}
                            }

                            binding.pager.addOnPageChangeListener(onPageChangeListener)
                            onPageChangeListener.onPageSelected(binding.pager.currentItem)
                            
                            activityBinding.appBar.setTag(
                                R.id.app_bar_can_change_size,
                                true
                            )

                            arguments?.getInt(Stuff.ARG_TAB, -1)
                                ?.coerceAtMost(adapter.count - 1)
                                ?.let {
                                    if (it >= 0) {
                                        binding.pager.setCurrentItem(it, false)
                                        arguments?.remove(Stuff.ARG_TAB)
                                    }
                                }

                            if (!UiUtils.isTabletUi) {
                                activityBinding.bottomNav.visibility = View.VISIBLE
                                activityBinding.bottomNav.slide()
                            } else
                                mainNotifierViewModel.loadCurrentUserDrawerData()
                        }
                    }

                    Lifecycle.Event.ON_DESTROY -> {
                        menus.forEach { it.clear() }
                        activityBinding.bottomNav.setOnItemSelectedListener(null)
                        activityBinding.sidebarNav.setNavigationItemSelectedListener(null)
                        activityBinding.bottomNav.slide(false)
                        activityBinding.bottomNav.visibility = View.GONE
                        source.lifecycle.removeObserver(this)

                        if (activityViewModel.destroyEventPending.availablePermits == 0)
                            activityViewModel.destroyEventPending.release()
                    }

                    else -> {}
                }
            }

        })
    }

}
