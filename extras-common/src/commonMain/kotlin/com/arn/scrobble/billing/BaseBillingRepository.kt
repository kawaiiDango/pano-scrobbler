package com.arn.scrobble.billing

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


abstract class BaseBillingRepository(
    protected val context: Any?,
    protected val clientData: BillingClientData,
    protected val openInBrowser: (url: String) -> Unit,
) {
    protected val scope = GlobalScope

    protected abstract val _proProductDetails: MutableStateFlow<MyProductDetails?>
    abstract val proProductDetails: StateFlow<MyProductDetails?>
    protected val _networkError = MutableSharedFlow<Unit>()
    val networkError = _networkError.asSharedFlow()
    abstract val purchaseMethods: List<PurchaseMethod>
    abstract val needsActivationCode: Boolean
    protected val _licenseState = MutableStateFlow(
        clientData.receipt.value.let { (r, s) ->
            getLicenseState(r, s)
        }
    )
    val licenseState = _licenseState.asStateFlow()

    inline val isLicenseValid: Boolean
        get() = licenseState.value == LicenseState.VALID

    init {
        scope.launch {
            clientData.receipt
                .collectLatest { (r, s) ->
                    _licenseState.value = getLicenseState(r, s)
                }
        }
    }

    private fun getLicenseState(receipt: String?, signature: String?): LicenseState {
        return if (receipt == null) {
            LicenseState.NO_LICENSE
        } else if (verifyPurchase(receipt, signature)) {
            LicenseState.VALID
        } else {
            LicenseState.NO_LICENSE
        }
    }

    abstract fun initBillingClient()

    abstract fun startDataSourceConnections()
    abstract fun endDataSourceConnections()
    abstract suspend fun queryPurchasesAsync()
    abstract suspend fun checkAndStoreLicense(receipt: String)
    protected abstract fun verifyPurchase(data: String, signature: String?): Boolean
    abstract fun launchBillingFlow(purchaseMethod: PurchaseMethod, activity: Any)
}