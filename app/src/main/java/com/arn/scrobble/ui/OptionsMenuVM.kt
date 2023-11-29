package com.arn.scrobble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class OptionsMenuVM : ViewModel() {
    private val _menuEvent = MutableSharedFlow<Pair<NavigationView, Int>>()
    val menuEvent = _menuEvent.asSharedFlow()

    fun onMenuItemSelected(navView: NavigationView, menuItemId: Int) {
        viewModelScope.launch {
            _menuEvent.emit(navView to menuItemId)
        }
    }
}