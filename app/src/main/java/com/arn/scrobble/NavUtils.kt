package com.arn.scrobble

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import coil.load
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.databinding.HeaderNavBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.InitialsDrawable
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.memoryCacheKey
import com.arn.scrobble.ui.UiUtils.showWithIcons
import com.arn.scrobble.ui.UiUtils.slide
import de.umass.lastfm.ImageSize
import kotlinx.coroutines.launch
import java.text.NumberFormat


object NavUtils {

    fun updateHeaderWithDrawerData(
        headerNavBinding: HeaderNavBinding,
        mainNotifierViewModel: MainNotifierViewModel
    ) {
        val accountType = Scrobblables.current?.userAccount?.type

        if (accountType == null) {
            headerNavBinding.root.isVisible = false
            return
        } else {
            headerNavBinding.root.isVisible = true
        }

        val currentUser = mainNotifierViewModel.currentUser
        val username = currentUser.name
        val navNumEntriesList = listOf(
            headerNavBinding.navNumArtists,
            headerNavBinding.navNumAlbums,
            headerNavBinding.navNumTracks
        )

        val displayText = when {
            Stuff.DEMO_MODE -> "nobody"
            accountType == AccountType.LASTFM -> username
            else -> Scrobblables.getString(accountType) + ": " + username
        }

        headerNavBinding.navName.text = displayText
        val nf = NumberFormat.getInstance()

        val drawerData = mainNotifierViewModel.drawerData.value ?: return

        if (drawerData.scrobblesToday >= 0) {
            headerNavBinding.navNumScrobblesToday.isVisible = true
            headerNavBinding.navNumScrobblesToday.text =
                App.context.resources.getQuantityString(
                    R.plurals.num_scrobbles_today,
                    drawerData.scrobblesToday,
                    nf.format(drawerData.scrobblesToday)
                )
        } else {
            headerNavBinding.navNumScrobblesToday.isVisible = false
        }

        if (drawerData.scrobblesTotal > 0) {
            headerNavBinding.navNumScrobblesTotal.isVisible = true
            headerNavBinding.navNumScrobblesTotal.text = nf.format(drawerData.scrobblesTotal)
        } else {
            headerNavBinding.navNumScrobblesTotal.isVisible = false
        }

        if (drawerData.artistCount >= 0) {
            navNumEntriesList.forEach { it.isVisible = true }
            headerNavBinding.navNumArtists.text = nf.format(drawerData.artistCount)
            headerNavBinding.navNumAlbums.text = nf.format(drawerData.albumCount)
            headerNavBinding.navNumTracks.text = nf.format(drawerData.trackCount)
        } else {
            navNumEntriesList.forEach { it.isVisible = false }
        }

        val profilePicUrl = currentUser.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
        if (headerNavBinding.navProfilePic.tag != profilePicUrl + username) // prevent flash
            headerNavBinding.navProfilePic.load(profilePicUrl) {
                allowHardware(false)
                placeholderMemoryCacheKey(headerNavBinding.navProfilePic.memoryCacheKey)
                error(
                    InitialsDrawable(
                        headerNavBinding.root.context,
                        username,
                        colorFromHash = false
                    )
                )
                listener(
                    onSuccess = { _, _ ->
                        headerNavBinding.navProfilePic.tag = profilePicUrl + username
                    },
                    onError = { _, _ ->
                        headerNavBinding.navProfilePic.tag = profilePicUrl + username
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

            val prefs = MainPrefs(App.context)
            val popup = PopupMenu(headerNavBinding.root.context, anchor)

            popup.menu.add(1, -2, 0, R.string.profile)
                .apply { setIcon(R.drawable.vd_open_in_new) }
            popup.menu.add(1, -1, 0, R.string.reports)
                .apply { setIcon(R.drawable.vd_open_in_new) }

            if (mainNotifierViewModel.userIsSelf) {
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

                            mainNotifierViewModel.drawerData.value = null
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
                                    UiUtils.loadSmallUserPic(
                                        activityBinding.bottomNav.context,
                                        mainNotifierViewModel.currentUser
                                    ) {
                                        MenuItemCompat.setIconTintMode(
                                            moreMenu,
                                            PorterDuff.Mode.DST
                                        )
                                        moreMenu.icon = it
                                    }
                                } else {
                                    moreMenu.setIcon(R.drawable.vd_more_horiz)
                                }
                                activityBinding.sidebarNav.inflateMenu(optionsMenuRes)
                            }

                            activityBinding.bottomNav.setOnItemSelectedListener { menuItem ->
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

                            activityBinding.sidebarNav.setNavigationItemSelectedListener { menuItem ->
                                val page = menuItem.itemId - idOffset

                                if (page in adapter.tabMetadata.indices) {
                                    menuItem.isChecked = true
                                    binding.pager.setCurrentItem(page, true)
                                    true
                                } else {
                                    optionsMenuViewModel.menuEvent.value = menuItem.itemId
                                    false
                                }
                            }

                            binding.pager.addOnPageChangeListener(object : OnPageChangeListener {
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
                                    if (position in adapter.tabMetadata.indices) {
                                        activityBinding.bottomNav.selectedItemId =
                                            idOffset + position
                                        activityBinding.sidebarNav.setCheckedItem(idOffset + position)

                                        if (this@setupWithNavUi is HomePagerFragment) {
                                            if (findNavController().currentDestination?.id == R.id.myHomePagerFragment)
                                                prefs.lastHomePagerTab = position

                                            expandAppBar(position)
                                        }
                                    }
                                }

                                override fun onPageScrollStateChanged(state: Int) {}
                            }
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
                        activityViewModel.destroyEventPending.release()
                    }

                    else -> {}
                }
            }

        })
    }

}
