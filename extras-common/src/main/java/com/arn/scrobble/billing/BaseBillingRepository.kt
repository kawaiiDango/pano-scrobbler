package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

data class BillingClientData(
    val proProductId: String,
    val appName: String,
    val publicKeyBase64: String,
    val apkSignature: String,
    val httpClient: HttpClient,
    val serverUrl: String,
    val getLastcheckTime: () -> Long,
    val setLastcheckTime: (Long) -> Unit,
    val getReceipt: () -> Pair<String?, String?>,
    val setReceipt: (String?, String?) -> Unit,
)

abstract class BaseBillingRepository(
    protected val application: Application,
    protected val clientData: BillingClientData,
) {
    protected var reconnectCount = 0
    protected var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    protected abstract val _proProductDetails: MutableStateFlow<MyProductDetails?>
    abstract val proProductDetails: StateFlow<MyProductDetails?>

    protected val _licenseState by lazy {
        val (r, s) = clientData.getReceipt()
        MutableStateFlow(
            if (r == null)
                LicenseState.NO_LICENSE
            else if (verifyPurchase(r, s ?: ""))
                LicenseState.VALID
            else
                LicenseState.NO_LICENSE
        )
    }
    val licenseState = _licenseState.asStateFlow()

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

    // tmp fix

    fun refreshLicenseState() {
        val (r, s) = clientData.getReceipt()
        _licenseState.value =
            if (r == null)
                LicenseState.NO_LICENSE
            else if (verifyPurchase(r, s ?: ""))
                LicenseState.VALID
            else
                LicenseState.NO_LICENSE
    }

    companion object {
        const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
        private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
        private const val RECONNECT_MAX_TIMES = 10
        val handler = Handler(Looper.getMainLooper())
    }
}