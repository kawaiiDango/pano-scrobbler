package com.arn.scrobble.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.main.App
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class BillingViewModel : ViewModel() {

    private val repository = App.billingRepository

    val proStatus: StateFlow<Boolean> = repository.proStatus

    val proProductDetails: StateFlow<MyProductDetails?> = repository.proProductDetails
    val proPendingSince: StateFlow<Long> = repository.proPendingSince

    init {
        repository.initBillingClient()
        repository.startDataSourceConnections()
    }

    fun queryPurchasesAsync() {
        viewModelScope.launch {
            delay(500)
            repository.queryPurchasesAsync()
        }
    }

    override fun onCleared() {
        repository.endDataSourceConnections()
        super.onCleared()
    }

    fun makePlayPurchase(activity: Activity) {
        repository.launchPlayBillingFlow(activity)
    }
}