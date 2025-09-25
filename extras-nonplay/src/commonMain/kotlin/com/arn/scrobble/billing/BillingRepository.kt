package com.arn.scrobble.billing

import com.arn.scrobble.api.license.LicenseChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class BillingRepository(
    context: Any?,
    clientData: BillingClientData,
) : BaseBillingRepository(
    context,
    clientData
) {

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
                "$5 or more",
            )

        _proProductDetails.value = productDetails
    }

    override fun startDataSourceConnections() {
    }

    override fun endDataSourceConnections() {
    }

    override suspend fun queryPurchasesAsync() {
        val (receipt, _) = clientData.receipt.first()
        receipt ?: return
        checkAndStoreLicense(receipt)
    }

    override suspend fun checkAndStoreLicense(receipt: String) {
        if (verifyPurchase(receipt, null)) {
            if (_licenseState.value != LicenseState.VALID || System.currentTimeMillis() - clientData.lastcheckTime.first() > CHECK_EVERY) {
                LicenseChecker.checkLicenseOnline(
                    client = clientData.httpClient,
                    url = clientData.serverUrl,
                    did = clientData.deviceIdentifier(),
                    token = receipt
                ).onSuccess {
                    when (it.code) {
                        0 -> {
                            if (it.message == "valid") {
                                _licenseState.emit(LicenseState.VALID)
                                clientData.setReceipt(receipt, null)
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
                }.onFailure { e ->
                    _licenseState.emit(LicenseState.NETWORK_ERROR)
                    e.printStackTrace()
                }
            }
        } else {
            delay(2000)
            _licenseState.emit(LicenseState.REJECTED)
            clientData.setReceipt(null, null)
        }
    }


    override fun verifyPurchase(data: String, signature: String?) =
        LicenseChecker.validateJwt(
            data,
            clientData.proProductId,
            clientData.publicKeyBase64
        )

    override fun launchPlayBillingFlow(activity: Any) {
    }

}