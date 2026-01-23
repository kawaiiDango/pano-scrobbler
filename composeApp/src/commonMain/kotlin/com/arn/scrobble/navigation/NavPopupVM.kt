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

            Scrobblables.current
                ?.loadDrawerData(user.name)
                ?.also { ddr ->
                    ddr.onSuccess { dd ->
                        if (user.isSelf) {
                            PlatformStuff.mainPrefs.updateData {
                                it.copy(drawerData = it.drawerData + (it.currentAccountType to dd))
                            }
                        }
                        emit(dd)
                    }
                }

        }.stateIn(viewModelScope, SharingStarted.Lazily, initialDrawerData)
}