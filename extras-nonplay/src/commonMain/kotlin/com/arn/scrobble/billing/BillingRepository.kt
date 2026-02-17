package com.arn.scrobble.billing

import com.arn.scrobble.api.license.LicenseChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration.Companion.days

private const val PUBLIC_KEY_BASE64 =
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnElQD+PNdex6IZ1nq58KDJPz40GBgOIbUs3GrbaPsONcEy8+AEhZmpPDcVB/e931pExsGPdRrjd2cplJ8pUXvxBG5knyJv7EPO3VUnppbipqYhaSe9bH4nK5kuNROB/J3mggVMxZmgoDe2QHacrNbnfjS96pFc58MAjQPPCn6TAXA1H3WajvNcRnplBYK7N0ap/YT1dbMato4fl/0iT1J57bDz+J+w/DcewOOg7YPWxVN+p9WZyLKwgQ8y/1QybEi9IYfIw3INqVS11vx5f+79ZkY+xGAM9JHm7T71dDZc4rJPibUnnQ+R5J2jFz564wdio6i1zpKwUpNQgYbfpkPQIDAQAB"

class BillingRepository(
    receipt: Flow<Pair<String?, String?>>,
    private val lastCheckTime: Flow<Long>,
    private val setLastcheckTime: suspend (Long) -> Unit,
    private val setReceipt: suspend (String?, String?) -> Unit,

    private val httpPost: suspend (url: String, body: String) -> String,
    private val deviceIdentifier: () -> String,
    private val openInBrowser: (url: String) -> Unit,
    context: Any?,
) : BaseBillingRepository(receipt) {

    override val formattedPrice = flowOf("$5 or more")
    override val purchaseMethods = listOf(
        PurchaseMethod(
            displayName = "Ko-fi",
            displayDesc = "Uses Paypal",
            link = "https://ko-fi.com/kawaiiDango"
        ),
        PurchaseMethod(
            displayName = "BuyMeACoffee",
            displayDesc = "Uses Stripe",
            link = "https://buymeacoffee.com/kawaiidango"
        )
    )

    override val needsActivationCode = true
    private val serverUrl = "https://license-sever.kawaiidango.workers.dev"

    override fun initBillingClient() {
    }

    override fun startDataSourceConnections() {
    }

    override fun endDataSourceConnections() {
    }

    override suspend fun queryPurchasesAsync() {
        val (receipt, _) = receipt.first()
        receipt ?: return
        checkAndStoreLicense(receipt)
    }

    override suspend fun checkAndStoreLicense(receipt: String) {
        if (verifyPurchase(receipt, null)) {
            if (licenseState.value != LicenseState.VALID ||
                System.currentTimeMillis() - lastCheckTime.first() > checkEveryDays.days.inWholeMilliseconds
            ) {
                LicenseChecker.checkLicenseOnline(
                    httpPost = httpPost,
                    url = "$serverUrl/license/verify",
                    did = deviceIdentifier(),
                    token = receipt
                ).onSuccess {
                    when (it.code) {
                        0 -> {
                            if (it.message == "valid") {
                                setReceipt(receipt, null)
                            } else {
                                _licenseError.emit(LicenseError.REJECTED)
                                setReceipt(null, null)
                            }
                        }

                        1 -> {
                            _licenseError.emit(LicenseError.REJECTED)
                            setReceipt(null, null)
                        }

                        2 -> {
                            _licenseError.emit(LicenseError.MAX_DEVICES_REACHED)
                            setReceipt(null, null)
                        }
                    }

                    setLastcheckTime(System.currentTimeMillis())
                }.onFailure { e ->
                    e.printStackTrace()
                    _licenseError.emit(LicenseError.NETWORK_ERROR)
                }
            }
        } else {
            delay(2000)
            _licenseError.emit(LicenseError.REJECTED)
            setReceipt(null, null)
        }
    }


    override fun verifyPurchase(data: String, signature: String?) =
        LicenseChecker.validateJwt(
            data,
            productId,
            PUBLIC_KEY_BASE64
        )

    override fun launchBillingFlow(purchaseMethod: PurchaseMethod, activity: Any?) {
        purchaseMethod.link?.let { openInBrowser(it) }
    }

}