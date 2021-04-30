package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.android.billingclient.api.SkuDetails


class BillingViewModel(application: Application) : AndroidViewModel(application) {

    val proStatus: LiveData<Boolean>
    val proSkuDetails: LiveData<SkuDetails>
    val proPendingSince: LiveData<Long>

    private val repository = BillingRepository.getInstance(application).apply {
        startDataSourceConnections()
        proStatus = proStatusLd
        proSkuDetails = proSkuDetailsLd
        proPendingSince = proPendingSinceLd
    }

    fun queryPurchases() = repository.queryPurchasesAsync()

    override fun onCleared() {
        super.onCleared()
        repository.endDataSourceConnections()
    }

    fun makePurchase(activity: Activity, skuDetails: SkuDetails) {
        repository.launchBillingFlow(activity, skuDetails)
    }
}