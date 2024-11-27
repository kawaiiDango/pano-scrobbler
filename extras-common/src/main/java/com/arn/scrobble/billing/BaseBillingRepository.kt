package com.arn.scrobble.billing

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
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
    protected val _licenseState = MutableStateFlow<LicenseState?>(null)
    val licenseState = _licenseState
        .filterNotNull()
        .stateIn(scope, SharingStarted.Eagerly, null)

    val isLicenseValid: Boolean
        get() = licenseState.value == LicenseState.VALID

    init {
        scope.launch {
            clientData.receipt
                .distinctUntilChanged()
                .mapLatest { (r, s) ->
                    if (r == null)
                        LicenseState.NO_LICENSE
                    else if (verifyPurchase(r, s))
                        LicenseState.VALID
                    else
                        LicenseState.NO_LICENSE
                }.collect {
                    _licenseState.value = it
                }
        }
    }

    abstract fun initBillingClient()

    abstract fun startDataSourceConnections()
    abstract fun endDataSourceConnections()
    abstract suspend fun queryPurchasesAsync()
    abstract suspend fun checkAndStoreLicense(receipt: String)
    protected abstract fun verifyPurchase(data: String, signature: String?): Boolean
    abstract fun launchPlayBillingFlow(activity: Any)

    @Synchronized
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