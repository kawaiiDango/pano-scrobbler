package com.arn.scrobble.utils

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import coil3.load
import coil3.request.error
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.databinding.HeaderNavBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.main.BasePagerFragment
import com.arn.scrobble.main.HomePagerFragment
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.main.OptionsMenuMetadata
import com.arn.scrobble.ui.InitialsDrawable
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.getSingle
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.arn.scrobble.utils.UiUtils.slide


object NavUtils {

    fun updateHeaderWithDrawerData(
        headerNavBinding: HeaderNavBinding,
        mainNotifierViewModel: MainNotifierViewModel
    ) {
        val accountType = Scrobblables.current?.userAccount?.type

        if (accountType == null || mainNotifierViewModel.drawerData.value == null) {
            headerNavBinding.navName.text =
                headerNavBinding.root.context.getString(R.string.app_name)
            return
        }

        Scrobblables.currentScrobblableUser?.let {
            mainNotifierViewModel.initializeCurrentUser(it)
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
            val currentAccount = Scrobblables.current?.userAccount

            // show FAQ in the dropdown as a way to prevent a focus bug on TV
            if (currentAccount == null) {
                val popup = PopupMenu(headerNavBinding.root.context, anchor)
                popup.inflate(R.menu.faq_menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.nav_faq -> {
                            val args = Bundle().apply {
                                putString(
                                    Stuff.ARG_URL,
                                    headerNavBinding.root.context.getString(R.string.faq_link)
                                )
                            }
                            navController.navigate(R.id.webViewFragment, args)
                        }
                    }
                    true
                }
                popup.showWithIcons()

                return@setOnClickListener
            }

            Scrobblables.currentScrobblableUser?.let {
                mainNotifierViewModel.initializeCurrentUser(it)
            }

            val currentUser = mainNotifierViewModel.currentUser

            val prefs = App.prefs
            val popup = PopupMenu(headerNavBinding.root.context, anchor)

            popup.menu.add(1, -2, 0, R.string.profile)
                .apply { setIcon(R.drawable.vd_open_in_new) }

            if (!Stuff.isTv) {
                popup.menu.add(1, -1, 0, R.string.reports)
                    .apply { setIcon(R.drawable.vd_open_in_new) }
            }

//            if (currentUser.isSelf || Stuff.isTv) {
//                popup.menu.add(1, -3, 0, R.string.scrobble_services)
//                    .apply { setIcon(R.drawable.vd_accounts) }
//            }

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
                    -3 -> {
                        val args = Bundle().apply { putBoolean(Stuff.ARG_SCROLL_TO_ACCOUNTS, true) }
                        navController.navigate(R.id.prefFragment, args)
                    }

                    -2 -> {
                        Stuff.openInBrowser(headerNavBinding.root.context, currentUser.url)
                    }

                    -1 -> {
                        val url = when (currentAccount.type) {
                            AccountType.LASTFM -> "https://www.last.fm/user/${currentUser.name}/listening-report/week"
                            AccountType.LIBREFM -> "https://libre.fm/user/${currentUser.name}/stats"
                            AccountType.GNUFM -> "${currentAccount.apiRoot}/stats"
                            AccountType.LISTENBRAINZ -> "https://listenbrainz.org/user/${currentUser.name}/reports"
                            AccountType.CUSTOM_LISTENBRAINZ -> "${currentAccount.apiRoot}user/${currentUser.name}/reports"
                            AccountType.PLEROMA -> "${currentAccount.apiRoot}/users/${currentUser.name}"
                            AccountType.MALOJA,
                            AccountType.FILE -> currentAccount.apiRoot!!
                        }

                        Stuff.openInBrowser(headerNavBinding.root.context, url)
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

                            mainNotifierViewModel.loadDrawerDataCached()
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
            activityViewModel.setCurrentUser(
                arguments?.getSingle() ?: Scrobblables.currentScrobblableUser!!
            )
        }

        fun clearMenus() {
            menus.forEach { it.clear() }
            activityBinding.bottomNav.setOnItemSelectedListener(null)
            activityBinding.sidebarNav.setNavigationItemSelectedListener(null)
            activityBinding.bottomNav.slide(false)
            activityBinding.bottomNav.visibility = View.GONE
//            source.lifecycle.removeObserver(this)
        }

        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        // clear previous menus if any

                        if (activityBinding.bottomNav.menu.size() > 0) {
                            activityBinding.bottomNav.setTag(R.id.destroyed_early, true)
                            clearMenus()
                        }
//                            activityViewModel.destroyEventPending.acquire()
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
                                        activityViewModel.drawerData.value,
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

                            if (optionsMenuRes != 0)
                                activityBinding.sidebarNav.inflateMenu(optionsMenuRes)

                            if (!App.prefs.proStatus)
                                activityBinding.sidebarNav.menu.findItem(R.id.nav_pro)?.isVisible =
                                    true

                        }

                        activityBinding.bottomNav.setOnItemSelectedListener { menuItem ->

                            activityBinding.appBar.setTag(
                                R.id.app_bar_can_change_size,
                                true
                            )

                            val itemId = menuItem.itemId - idOffset
                            when (itemId) {
                                in adapter.tabMetadata.indices -> {
                                    if (isReady)
                                        binding.pager.setCurrentItem(itemId, true)
                                    true
                                }

                                R.id.more_menu -> {

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
                                }

                                else -> false
                            }
                        }

//                        activityBinding.bottomNav.setOnItemReselectedListener {
//                            scrollToTop()
//                        }

                        activityBinding.sidebarNav.setNavigationItemSelectedListener { menuItem ->
                            val page = menuItem.itemId - idOffset

                            if (page in adapter.tabMetadata.indices) {
                                menuItem.isChecked = true
                                val reselected = page == binding.pager.currentItem
                                if (!reselected && isReady)
//                                    scrollToTop()
//                                else
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

                    Lifecycle.Event.ON_DESTROY -> {
                        if (activityBinding.bottomNav.getTag(R.id.destroyed_early) != true &&
                            activityBinding.bottomNav.menu.size() > 0
                        ) {
                            clearMenus()
                        }

                        activityBinding.bottomNav.setTag(R.id.destroyed_early, false)

                        source.lifecycle.removeObserver(this)

//                        if (activityViewModel.destroyEventPending.availablePermits == 0)
//                            activityViewModel.destroyEventPending.release()
                    }

                    else -> {}
                }
            }

        })
    }

}
