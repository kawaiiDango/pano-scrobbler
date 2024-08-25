package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import com.arn.scrobble.api.license.LicenseChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingRepository(
    application: Application,
    clientData: BillingClientData,
) : BaseBillingRepository(
    application,
    clientData
) {

    // Breaking change in billing v4: callbacks don't run on main thread, always use LiveData.postValue()

    // how long before the data source tries to reconnect to Google play

    override val _proProductDetails by lazy { MutableStateFlow<MyProductDetails?>(null) }
    override val proProductDetails by lazy { _proProductDetails.asStateFlow() }
    private val _proPendingSince by lazy { MutableStateFlow(0L) }
    override val proPendingSince by lazy { _proPendingSince.asStateFlow() }
    private val CHECK_EVERY = 1000L * 60 * 60 * 24 // 1 day


    override fun initBillingClient() {
        val productDetails =
            MyProductDetails(
                clientData.proProductId,
                "${clientData.appName} Supporter",
                "${clientData.appName} Supporter",
                "Become a ${clientData.appName} supporter",
                "$6 or more",
            )

        _proProductDetails.value = productDetails
    }

    override fun startDataSourceConnections() {
    }

    override fun endDataSourceConnections() {
    }


    override suspend fun queryPurchasesAsync() {
        val (receipt, _) = clientData.getReceipt()
        if (receipt != null && verifyPurchase(receipt, "")) {
            if (!proStatus.value || System.currentTimeMillis() - clientData.getLastcheckTime() > CHECK_EVERY) {
                LicenseChecker.checkLicenseOnline(
                    clientData.httpClient,
                    clientData.serverUrl,
                    receipt
                ).onSuccess {
                    updateProStatus(it)

                    if (!it) {
                        clientData.setReceipt(null, null)
                    }
                    clientData.setLastcheckTime(System.currentTimeMillis())
                }
            }
        } else {
            updateProStatus(false)
        }
    }


    override fun verifyPurchase(data: String, signature: String) =
        LicenseChecker.validateJwt(
            data,
            clientData.proProductId,
            clientData.publicKeyBase64
        )

    override fun launchPlayBillingFlow(activity: Activity) {
    }

}