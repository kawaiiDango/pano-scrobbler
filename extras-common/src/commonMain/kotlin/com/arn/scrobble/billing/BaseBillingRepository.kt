package com.arn.scrobble.billing

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.min


abstract class BaseBillingRepository(
    protected val androidApplication: Any?,
    protected val clientData: BillingClientData,
) {
    private var reconnectCount = 0
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
    private var reconnectJob: Job? = null

    protected val scope = GlobalScope

    protected abstract val _proProductDetails: MutableStateFlow<MyProductDetails?>
    abstract val proProductDetails: StateFlow<MyProductDetails?>
    protected val _licenseState = MutableStateFlow(
        clientData.receipt.value.let { (r, s) ->
            getLicenseState(r, s)
        }
    )
    val licenseState = _licenseState.asStateFlow()

    val isLicenseValid: Boolean
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
    abstract fun launchPlayBillingFlow(activity: Any)

    protected fun retryBillingConnectionWithExponentialBackoff() {
        if (reconnectCount >= RECONNECT_MAX_TIMES) {
            return
        }

        reconnectMilliseconds = min(
            reconnectMilliseconds * 2,
            RECONNECT_TIMER_MAX_TIME_MILLISECONDS
        )
        reconnectCount++

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectMilliseconds)
            startDataSourceConnections()
        }
    }

    protected fun resetReconnectTimer() {
        reconnectCount = 0
        reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
        reconnectJob?.cancel()
    }

    companion object {
        const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
        private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
        private const val RECONNECT_MAX_TIMES = 10
    }
}