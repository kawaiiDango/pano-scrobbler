package com.arn.scrobble.ui

import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent

class OptionsMenuVM: ViewModel() {
    val menuEvent = LiveEvent<Int>()

    fun emit(menuId: Int) {
        menuEvent.value = menuId
    }
}