package com.arn.scrobble

import android.os.Build
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.navigation.NavController
import coil.load
import com.arn.scrobble.databinding.HeaderNavBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.InitialsDrawable
import com.arn.scrobble.ui.UiUtils.memoryCacheKey
import com.arn.scrobble.ui.UiUtils.showWithIcons
import de.umass.lastfm.ImageSize
import java.text.NumberFormat

object NavHeaderUtils {

    fun updateHeaderWithDrawerData(
        headerNavBinding: HeaderNavBinding,
        mainNotifierViewModel: MainNotifierViewModel
    ) {
        val accountType = Scrobblables.current?.userAccount?.type

        if (accountType == null) {
            headerNavBinding.root.isVisible = false
            return
        } else
            headerNavBinding.root.isVisible = true

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
        headerNavBinding.navNumFlow.isVisible = true

        if (drawerData.scrobblesToday >= 0) {
            headerNavBinding.navNumScrobblesToday.text =
                App.context.resources.getQuantityString(
                    R.plurals.num_scrobbles_today,
                    drawerData.scrobblesToday,
                    nf.format(drawerData.scrobblesToday)
                )
        }

        if (drawerData.scrobblesTotal > 0)
            headerNavBinding.navNumScrobblesTotal.text = nf.format(drawerData.scrobblesTotal)

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
                            mainNotifierViewModel.popUser()
                            mainNotifierViewModel.pushUser(Scrobblables.current!!.userAccount.user)
                            setProfileSwitcher(
                                headerNavBinding,
                                navController,
                                mainNotifierViewModel
                            )

                            navController.navigate(R.id.myHomePagerFragment)

                            mainNotifierViewModel.loadCurrentUserDrawerData()
                        }
                    }
                }
                true
            }
            popup.showWithIcons()
        }
    }

}