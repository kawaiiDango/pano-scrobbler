package com.arn.scrobble.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class BillingViewModel : ViewModel() {

    private val repository = Stuff.billingRepository

    val proProductDetails = repository.proProductDetails

    val licenseState = repository.licenseState

    init {
        repository.initBillingClient()
        repository.startDataSourceConnections()
    }

    fun checkAndStoreLicense(receipt: String) {
        viewModelScope.launch {
            repository.checkAndStoreLicense(receipt)
        }
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