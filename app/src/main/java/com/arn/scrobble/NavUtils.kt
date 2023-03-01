package com.arn.scrobble

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.slide
import kotlinx.coroutines.launch


object NavUtils {
    const val OPTIONS_MENU_ID = 100

    fun BasePagerFragment.setupWithNavUi() {
        val activityBinding = (activity as? MainActivity)?.binding ?: return
        val menus = listOf(
            activityBinding.bottomNav.menu,
            activityBinding.sidebarNav.menu
        )

        // hack to prevent a visual glitch
        // referring to a viewmodel keeps the state on config changes
        val idOffset = optionsMenuViewModel.hashCode()

        val activityViewModel by activityViewModels<MainNotifierViewModel>()

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
                                val moreMenu = activityBinding.bottomNav.menu.add(
                                    0,
                                    OPTIONS_MENU_ID + idOffset,
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
                                } else if (itemId == OPTIONS_MENU_ID) {

                                    val args = Bundle().putSingle(
                                        OptionsMenuMetadata(optionsMenuRes, shouldShowUser)
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
                                    optionsMenuViewModel.emit(menuItem.itemId)
                                    false
                                }
                            }

                            binding.pager.addOnPageChangeListener(object : OnPageChangeListener {

                                override fun onPageScrolled(
                                    position: Int,
                                    positionOffset: Float,
                                    positionOffsetPixels: Int
                                ) {}

                                override fun onPageSelected(position: Int) {
                                    if (position in adapter.tabMetadata.indices) {
                                        activityBinding.bottomNav.selectedItemId =
                                            idOffset + position
                                        activityBinding.sidebarNav.setCheckedItem(idOffset + position)
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
                                        activityBinding.bottomNav.selectedItemId = it + idOffset
//                                    activityBinding.sidebarNav.setCheckedItem(it)
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
