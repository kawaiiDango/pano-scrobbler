package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import io.ktor.client.HttpClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

data class BillingClientData(
    val proProductId: String,
    val appName: String,
    val publicKeyBase64: String,
    val apkSignature: String,
    val httpClient: HttpClient,
    val serverUrl: String,
    val lastcheckTime: Flow<Long>,
    val setLastcheckTime: suspend (Long) -> Unit,
    val receipt: Flow<Pair<String?, String?>>,
    val setReceipt: suspend (String?, String?) -> Unit,
)

abstract class BaseBillingRepository(
    protected val application: Application,
    protected val clientData: BillingClientData,
) {
    protected var reconnectCount = 0
    protected var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    protected val scope = GlobalScope

    protected abstract val _proProductDetails: MutableStateFlow<MyProductDetails?>
    abstract val proProductDetails: StateFlow<MyProductDetails?>
    protected val _licenseState = MutableStateFlow<LicenseState?>(null)
    val licenseState = _licenseState.filterNotNull().stateIn(scope, SharingStarted.Eagerly, null)

    val isLicenseValid: Boolean
        get() = licenseState.value == LicenseState.VALID

    init {
        scope.launch {
            clientData.receipt
                .distinctUntilChanged()
                .mapLatest { (r, s) ->
                    if (r == null)
                        LicenseState.NO_LICENSE
                    else if (verifyPurchase(r, s ?: ""))
                        LicenseState.VALID
                    else
                        LicenseState.NO_LICENSE
                }.collect {
                    _licenseState.value = it
                }
        }
    }

    open fun initBillingClient() {
    }

    abstract fun startDataSourceConnections()
    abstract fun endDataSourceConnections()
    abstract suspend fun queryPurchasesAsync()
    abstract fun verifyPurchase(data: String, signature: String): Boolean
    abstract fun launchPlayBillingFlow(activity: Activity)

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

        handler.postDelayed(::startDataSourceConnections, reconnectMilliseconds)
    }

    companion object {
        const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
        private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
        private const val RECONNECT_MAX_TIMES = 10
        val handler = Handler(Looper.getMainLooper())
    }
}