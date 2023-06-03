package com.arn.scrobble.ui

import androidx.lifecycle.ViewModel
import com.google.android.material.navigation.NavigationView
import com.hadilq.liveevent.LiveEvent

class OptionsMenuVM : ViewModel() {
    val menuEvent = LiveEvent<Pair<NavigationView, Int>>()
}