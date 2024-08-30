package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*


class BillingRepository(
    application: Application,
    clientData: BillingClientData,
) : BaseBillingRepository(
    application,
    clientData
),
    PurchasesUpdatedListener, BillingClientStateListener {

    // Breaking change in billing v4: callbacks don't run on main thread, always use LiveData.postValue()

    // how long before the data source tries to reconnect to Google play
    override val _proProductDetails by lazy { MutableStateFlow<MyProductDetails?>(null) }
    override val proProductDetails by lazy { _proProductDetails.asStateFlow() }
    private val TAG = BillingRepository::class.simpleName!!
    private lateinit var playStoreBillingClient: BillingClient
    private var proProductDetailsList: List<ProductDetails>? = null
    val PENDING_PURCHASE_NOTIFY_THRESHOLD = 15 * 1000L


    override fun initBillingClient() {
        playStoreBillingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            ) // required or app will crash
            .setListener(this)
            .build()
    }

    override fun startDataSourceConnections() {
        playStoreBillingClient.startConnection(this)
    }

    override fun endDataSourceConnections() {
        playStoreBillingClient.endConnection()
    }


    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                handler.removeCallbacksAndMessages(null)
                reconnectCount = 0
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                fetchProductDetails(clientData.proProductId)

                // this runs in a separate thread anyways
                runBlocking {
                    queryPurchasesAsync()
                }
            }

            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Timber.w("BILLING_UNAVAILABLE")
                //Some apps may choose to make decisions based on this knowledge.
            }

            BillingClient.BillingResponseCode.ERROR -> {
                Timber.tag(TAG)
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
    override suspend fun queryPurchasesAsync() {
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
                if (verifyPurchase(purchase.originalJson, purchase.signature)) {
                    clientData.setReceipt(purchase.originalJson, purchase.signature)
                    validPurchases.add(purchase)
                }
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                if (clientData.proProductId in purchase.products) {
                    // This doesn't go away after the slow card gets declined. So, only notify recent purchases
                    if (System.currentTimeMillis() - purchase.purchaseTime < PENDING_PURCHASE_NOTIFY_THRESHOLD)
                        _licenseState.tryEmit(LicenseState.PENDING)
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

    override fun verifyPurchase(data: String, signature: String) =
        Security.verifyPurchase(
            data,
            signature,
            clientData.publicKeyBase64,
        ) &&
                Security.signature(application, clientData.apkSignature) != null


    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchase] inside your app.
     */
    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: Set<Purchase>) {
        if (nonConsumables.isEmpty() || !nonConsumables.any { clientData.proProductId in it.products }) {
            _licenseState.tryEmit(LicenseState.NO_LICENSE)
            clientData.setReceipt(null, null)
            return
        }

        nonConsumables.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(
                purchase
                    .purchaseToken
            ).build()
            playStoreBillingClient.acknowledgePurchase(params) { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        if (clientData.proProductId in purchase.products) {
                            _licenseState.tryEmit(LicenseState.VALID)
                        }
                    }

                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        if (clientData.proProductId in purchase.products) {
                            _licenseState.tryEmit(LicenseState.NO_LICENSE)
                            clientData.setReceipt(null, null)
                        }
                    }
                }
            }

        }
    }

    private fun findProProduct(): ProductDetails? {
        val productDetails =
            proProductDetailsList?.firstOrNull { it.productId == clientData.proProductId }

        return if (productDetails != null && clientData.appName in productDetails.title &&
            clientData.appName in productDetails.name &&
            productDetails.productId != productDetails.title && productDetails.productId != productDetails.name
        )
            productDetails
        else if (productDetails != null) {
            _proProductDetails.tryEmit(null)
            _licenseState.tryEmit(LicenseState.NO_LICENSE)
            clientData.setReceipt(null, null)
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
                    proProductDetailsList = productDetailsList
                    findProProduct()?.let {
                        _proProductDetails.tryEmit(
                            MyProductDetails(
                                it.productId,
                                it.title,
                                it.name,
                                it.description,
                                it.oneTimePurchaseOfferDetails!!.formattedPrice
                            )
                        )
                    }
                }

                else -> {
                    Timber.tag(TAG)
                        .d(billingResult.debugMessage)
                }
            }
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [onPurchasesUpdated]
     */
    override fun launchPlayBillingFlow(activity: Activity) {
        findProProduct()?.let { productDetails ->
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
                runBlocking {
                    queryPurchasesAsync()
                }
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                retryBillingConnectionWithExponentialBackoff()
            }

            else -> {
                Timber.tag(TAG).d(billingResult.debugMessage)
            }
        }
    }

//    companion object {
//        @Volatile
//        private var INSTANCE: BillingRepository? = null
//
//        fun getInstance(application: Application): BillingRepository =
//            INSTANCE ?: synchronized(this) {
//                INSTANCE
//                    ?: BillingRepository(application)
//                        .also { INSTANCE = it }
//            }
//
//    }

}

