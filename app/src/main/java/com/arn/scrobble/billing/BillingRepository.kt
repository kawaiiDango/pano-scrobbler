package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.arn.scrobble.App
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Tokens
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.*
import kotlin.math.min


class BillingRepository private constructor(private val application: Application) :
    PurchasesUpdatedListener, BillingClientStateListener {

    // Breaking change in billing v4: callbacks don't run on main thread, always use LiveData.postValue()

    // how long before the data source tries to reconnect to Google play
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    private lateinit var playStoreBillingClient: BillingClient
    private val prefs = App.prefs
    private val _proStatus by lazy { MutableStateFlow(prefs.proStatus) }
    val proStatus by lazy { _proStatus.asStateFlow() }
    private val _proPendingSince by lazy { MutableStateFlow(0L) }
    val proPendingSince by lazy { _proPendingSince.asStateFlow() }
    private val _proProductDetails by lazy { MutableStateFlow<ProductDetails?>(null) }
    val proProductDetails by lazy { _proProductDetails.asStateFlow() }
    private var reconnectCount = 0

    fun startDataSourceConnections() {
        playStoreBillingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases() // required or app will crash
            .setListener(this)
            .build()
        playStoreBillingClient.startConnection(this)
    }

    fun endDataSourceConnections() {
        playStoreBillingClient.endConnection()
    }

    @Synchronized
    private fun retryBillingConnectionWithExponentialBackoff() {
        if (reconnectCount >= RECONNECT_MAX_TIMES) {
            return
        }

        reconnectMilliseconds = min(
            reconnectMilliseconds * 2,
            RECONNECT_TIMER_MAX_TIME_MILLISECONDS
        )
        reconnectCount++

        handler.postDelayed(
            { playStoreBillingClient.startConnection(this@BillingRepository) },
            reconnectMilliseconds
        )
    }


    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                handler.removeCallbacksAndMessages(null)
                reconnectCount = 0
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                fetchProductDetails(Tokens.PRO_PRODUCT_ID)
                queryPurchasesAsync()
            }

            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Stuff.logW("BILLING_UNAVAILABLE")
                //Some apps may choose to make decisions based on this knowledge.
            }

            BillingClient.BillingResponseCode.ERROR -> {
                Timber.tag(LOG_TAG)
                    .e(RuntimeException("BILLING_ERROR " + billingResult.debugMessage))
            }

            else -> {
                //do nothing. Someone else will connect it through retry policy.
                //May choose to send to server though
            }
        }
    }

    /**
     * This method is called when the app has inadvertently disconnected from the [BillingClient].
     * An attempt should be made to reconnect using a retry policy. Note the distinction between
     * [endConnection][BillingClient.endConnection] and disconnected:
     * - disconnected means it's okay to try reconnecting.
     * - endConnection means the [playStoreBillingClient] must be re-instantiated and then start
     *   a new connection because a [BillingClient] instance is invalid after endConnection has
     *   been called.
     **/
    override fun onBillingServiceDisconnected() {
        retryBillingConnectionWithExponentialBackoff()
    }

    /**
     * BACKGROUND
     *
     * Google Play Billing refers to receipts as [Purchases][Purchase]. So when a user buys
     * something, Play Billing returns a [Purchase] object that the app then uses to release the
     * [Entitlement] to the user. Receipts are pivotal within the [BillingRepository]; but they are
     * not part of the repo’s public API, because clients don’t need to know about them. When
     * the release of entitlements occurs depends on the type of purchase. For consumable products,
     * the release may be deferred until after consumption by Google Play; for non-consumable
     * products and subscriptions, the release may be deferred until after
     * [BillingClient.acknowledgePurchase] is called. You should keep receipts in the local
     * cache for augmented security and for making some transactions easier.
     *
     * THIS METHOD
     *
     * [This method][queryPurchasesAsync] grabs all the active purchases of this user and makes them
     * available to this app instance. Whereas this method plays a central role in the billing
     * system, it should be called at key junctures, such as when user the app starts.
     *
     * Because purchase data is vital to the rest of the app, this method is called each time
     * the [BillingViewModel] successfully establishes connection with the Play [BillingClient]:
     * the call comes through [onBillingSetupFinished]. Recall also from Figure 4 that this method
     * gets called from inside [onPurchasesUpdated] in the event that a purchase is "already
     * owned," which can happen if a user buys the item around the same time
     * on a different device.
     */
    fun queryPurchasesAsync() {
        if (!playStoreBillingClient.isReady)
            return

        val queryPurchasesParams = QueryPurchasesParams
            .newBuilder()
            .setProductType(ProductType.INAPP)
            .build()

        playStoreBillingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases.toHashSet())
            }
        }
    }

    private fun processPurchases(purchasesResult: Set<Purchase>) {
        val validPurchases = HashSet<Purchase>(purchasesResult.size)
        purchasesResult.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (Security.verifyPurchase(purchase)) {
                    validPurchases.add(purchase)
                }
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                if (Tokens.PRO_PRODUCT_ID in purchase.products) {
                    _proPendingSince.tryEmit(purchase.purchaseTime)
                }
            }
        }
        /*
          As is being done in this sample, for extra reliability you may store the
          receipts/purchases to a your own remote/local database for until after you
          disburse entitlements. That way if the Google Play Billing library fails at any
          given point, you can independently verify whether entitlements were accurately
          disbursed. In this sample, the receipts are then removed upon entitlement
          disbursement.
         */
//                val testing = localCacheBillingClient.purchaseDao().getPurchases()
//                Timber.tag(LOG_TAG).d("processPurchases purchases in the lcl db ${testing.size}")
//                localCacheBillingClient.purchaseDao().insert(*validPurchases.toTypedArray())
        acknowledgeNonConsumablePurchasesAsync(validPurchases)
    }


    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchase] inside your app.
     */
    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: Set<Purchase>) {
        if (nonConsumables.isEmpty() || !nonConsumables.any { Tokens.PRO_PRODUCT_ID in it.products }) {
            revokePro()
        }

        nonConsumables.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(
                purchase
                    .purchaseToken
            ).build()
            playStoreBillingClient.acknowledgePurchase(params) { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        disburseNonConsumableEntitlement(purchase)
                    }

                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        if (Tokens.PRO_PRODUCT_ID in purchase.products) {
                            revokePro()
                        }
                    }
                }
            }

        }
    }

    /**
     * This is the final step, where purchases/receipts are converted to premium contents.
     * In this sample, once the entitlement is disbursed the receipt is thrown out.
     */
    private fun disburseNonConsumableEntitlement(purchase: Purchase) {
        if (Tokens.PRO_PRODUCT_ID in purchase.products) {
            prefs.proStatus = true
            if (!_proStatus.value) {
                _proStatus.tryEmit(true)
                application.sendBroadcast(
                    Intent(NLService.iTHEME_CHANGED_S)
                        .setPackage(application.packageName),
                    NLService.BROADCAST_PERMISSION
                )
            }
            _proPendingSince.tryEmit(0)
        }
    }

    private fun revokePro() {
        prefs.proStatus = false
        if (_proStatus.value)
            _proStatus.tryEmit(false)
    }

    private fun findProProduct(productDetailsList: List<ProductDetails>?): ProductDetails? {
        val productDetails =
            productDetailsList?.firstOrNull { it.productId == Tokens.PRO_PRODUCT_ID }

        return if (productDetails != null && application.getString(R.string.app_name) in productDetails.title &&
            application.getString(R.string.app_name) in productDetails.name &&
            productDetails.productId != productDetails.title && productDetails.productId != productDetails.name
        )
            productDetails
        else if (productDetails != null) {
            _proProductDetails.tryEmit(null)
            revokePro()
            null
        } else null
    }


    private fun fetchProductDetails(
        productId: String
    ) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()

        playStoreBillingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    findProProduct(productDetailsList)?.let {
                        _proProductDetails.tryEmit(it)
                    }
                }

                else -> {
                    Timber.tag(LOG_TAG).d(billingResult.debugMessage)
                }
            }
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [onPurchasesUpdated]
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        findProProduct(listOf(productDetails))?.let { productDetails ->
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            playStoreBillingClient.launchBillingFlow(activity, flowParams)
        }
    }

    /**
     * This method is called by the [playStoreBillingClient] when new purchases are detected.
     * The purchase list in this method is not the same as the one in
     * [queryPurchases][BillingClient.queryPurchasesAsync]. Whereas queryPurchases returns everything
     * this user owns, [onPurchasesUpdated] only returns the items that were just now purchased or
     * billed.
     *
     * The purchases provided here should be passed along to the secure server for
     * [verification](https://developer.android.com/google/play/billing/billing_library_overview#Verify)
     * and safekeeping. And if this purchase is consumable, it should be consumed, and the secure
     * server should be told of the consumption. All that is accomplished by calling
     * [queryPurchasesAsync].
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // will handle server verification, consumables, and updating the local cache
                purchases?.apply { processPurchases(this.toSet()) }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // item already owned? call queryPurchasesAsync to verify and process all such items
                queryPurchasesAsync()
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                retryBillingConnectionWithExponentialBackoff()
            }

            else -> {
                Timber.tag(LOG_TAG).d(billingResult.debugMessage)
            }
        }
    }

    companion object {
        private const val LOG_TAG = Stuff.TAG
        private val INAPP_PRODUCT_IDS = listOf(Tokens.PRO_PRODUCT_ID)

        private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
        private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
        private const val RECONNECT_MAX_TIMES = 10

        @Volatile
        private var INSTANCE: BillingRepository? = null
        private val handler = Handler(Looper.getMainLooper())

        fun getInstance(application: Application): BillingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: BillingRepository(application)
                        .also { INSTANCE = it }
            }
    }
}

