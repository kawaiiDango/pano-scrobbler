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
            if (_licenseState.value != LicenseState.VALID || System.currentTimeMillis() - clientData.getLastcheckTime() > CHECK_EVERY) {
                LicenseChecker.checkLicenseOnline(
                    clientData.httpClient,
                    clientData.serverUrl,
                    receipt
                ).onSuccess {
                    when (it.code) {
                        0 -> {
                            if (it.message == "valid") {
                                _licenseState.emit(LicenseState.VALID)
                            } else {
                                _licenseState.emit(LicenseState.REJECTED)
                                clientData.setReceipt(null, null)
                            }
                        }

                        1 -> {
                            _licenseState.emit(LicenseState.REJECTED)
                            clientData.setReceipt(null, null)
                        }

                        2 -> {
                            _licenseState.emit(LicenseState.MAX_DEVICES_REACHED)
                            clientData.setReceipt(null, null)
                        }
                    }

                    clientData.setLastcheckTime(System.currentTimeMillis())
                }
            }
        } else {
            _licenseState.tryEmit(LicenseState.NO_LICENSE)
            clientData.setReceipt(null, null)
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