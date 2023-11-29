package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.StateFlow


class BillingViewModel(application: Application) : AndroidViewModel(application) {

    val proStatus: StateFlow<Boolean>
    val proProductDetails: StateFlow<ProductDetails?>
    val proPendingSince: StateFlow<Long>

    private val repository = BillingRepository.getInstance(application).also {
        it.startDataSourceConnections()
        proStatus = it.proStatus
        proProductDetails = it.proProductDetails
        proPendingSince = it.proPendingSince
    }

    fun queryPurchases() = repository.queryPurchasesAsync()

    override fun onCleared() {
        super.onCleared()
        repository.endDataSourceConnections()
    }

    fun makePurchase(activity: Activity, productDetails: ProductDetails) {
        repository.launchBillingFlow(activity, productDetails)
    }
}