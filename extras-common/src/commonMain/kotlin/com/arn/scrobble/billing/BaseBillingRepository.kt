package com.arn.scrobble.billing

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn


abstract class BaseBillingRepository(
    protected val context: Any?,
    protected val clientData: BillingClientData,
    protected val openInBrowser: (url: String) -> Unit,
) {
    protected val scope = GlobalScope

    protected abstract val _proProductDetails: MutableStateFlow<MyProductDetails?>
    abstract val proProductDetails: StateFlow<MyProductDetails?>
    protected val _licenseError = MutableSharedFlow<LicenseError>()
    val licenseError = _licenseError.asSharedFlow()
    abstract val purchaseMethods: List<PurchaseMethod>
    abstract val needsActivationCode: Boolean
    val licenseState = clientData.receipt
        .mapLatest { (receipt, signature) ->
            if (receipt == null) {
                LicenseState.NO_LICENSE
            } else if (verifyPurchase(receipt, signature)) {
                LicenseState.VALID
            } else {
                LicenseState.NO_LICENSE
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, LicenseState.UNKNOWN)

    abstract fun initBillingClient()

    abstract fun startDataSourceConnections()
    abstract fun endDataSourceConnections()
    abstract suspend fun queryPurchasesAsync()
    abstract suspend fun checkAndStoreLicense(receipt: String)
    protected abstract fun verifyPurchase(data: String, signature: String?): Boolean
    abstract fun launchBillingFlow(purchaseMethod: PurchaseMethod, activity: Any?)
}