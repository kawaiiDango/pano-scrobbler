package com.arn.scrobble.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NavPopupVM(
    user: UserCached,
    initialDrawerData: DrawerData,
) : ViewModel() {
    val drawerData =
        flow {
            if (user.isSelf) {
                PlatformStuff.mainPrefs.data.map {
                    it.drawerData[it.currentAccountType]
                }.first()?.let { emit(it) }
            }

            delay(2000)

            val scrobblable = Scrobblables.current
            scrobblable
                ?.loadDrawerData(user.name)
                ?.also { ddr ->
                    ddr.onSuccess { dd ->
                        if (user.isSelf) {
                            PlatformStuff.mainPrefs.updateData { p ->
                                p.copy(
                                    drawerData = p.drawerData + (p.currentAccountType to dd),
                                    scrobbleAccounts = p.scrobbleAccounts.map {
                                        if (it.type == p.currentAccountType &&
                                            dd.profilePicUrl != null &&
                                            dd.profilePicUrl != it.user.largeImage
                                        ) {
                                            val u = it.user.copy(largeImage = dd.profilePicUrl)
                                            it.copy(user = u)
                                        } else {
                                            it
                                        }
                                    }
                                )
                            }
                        }
                        emit(dd)
                    }
                }

        }.stateIn(viewModelScope, SharingStarted.Lazily, initialDrawerData)
}