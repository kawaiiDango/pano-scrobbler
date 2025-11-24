package com.arn.scrobble.billing

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "BillingRepository"
private const val PUBLIC_KEY_BASE64 =
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmdJDoSt28Ps1zqsHlMgIXqnLxDOKyT+qUl4dV8eto7RL0B58DrtiUYC0LlhaM+ilx+ClPbNYlYT9VI0u2Yk0/f0uIpy4W8Hxxv5P2/nlwyEzBPd8dvEtFi4c6YB+wA0dwokhVVSLb6S3XyCZ2ONmozwZZ8RT3B+/Zs3ZdnkZDqiYDyA9lQVReCcM/lHSXQpst8zcNo00DzXG+3ptVpa3fnNhWjm+kgqjntzAV+cT53D8Qc53sHpmqQG84pFzDhiQoNH2bCy+IDs0iP40Wdjj1mzm7N0RZ2gxFawZrUwWAhvHrgXWXOV+Vhd3upqZWAhBMeeV4K/4GAR7EOwTib1ngwIDAQAB"

class BillingRepository(
    context: Context,
    clientData: BillingClientData,
    openInBrowser: (url: String) -> Unit
) : BaseBillingRepository(
    context,
    clientData,
    openInBrowser
) {

    override val _proProductDetails by lazy { MutableStateFlow<MyProductDetails?>(null) }
    override val proProductDetails by lazy { _proProductDetails.asStateFlow() }
    override val purchaseMethods = listOf(
        PurchaseMethod(
            displayName = "Google Play",
            link = null
        ),
    )
    override val needsActivationCode = false
    private lateinit var playStoreBillingClient: BillingClient
    private var proProductDetailsList: List<ProductDetails>? = null
    private val PENDING_PURCHASE_NOTIFY_THRESHOLD = 60 * 1000L


    val billingClientStateListener: BillingClientStateListener =
        object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        fetchProductDetails(clientData.proProductId)

                        // this runs in a separate thread anyways
                        runBlocking {
                            queryPurchasesAsync()
                        }
                    }

                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                        Logger.w(TAG) { "BILLING_UNAVAILABLE" }
                        //Some apps may choose to make decisions based on this knowledge.
                    }

                    BillingClient.BillingResponseCode.ERROR -> {
                        Logger.d(TAG) { billingResult.debugMessage }
                    }

                    else -> {
                        //do nothing. Someone else will connect it through retry policy.
                        //May choose to send to server though
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // now handled by enableAutoServiceReconnection()
            }
        }

    val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // will handle server verification, consumables, and updating the local cache
                purchases?.let { scope.launch { processPurchases(it.toSet()) } }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // item already owned? call queryPurchasesAsync to verify and process all such items
                runBlocking {
                    queryPurchasesAsync()
                }
            }

            else -> {
                Logger.d(TAG) { billingResult.debugMessage }
            }
        }
    }

    override fun initBillingClient() {
        playStoreBillingClient = BillingClient.newBuilder(context as Context)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            ) // required or app will crash
            .enableAutoServiceReconnection()
            .setListener(purchasesUpdatedListener)
            .build()
    }

    override fun startDataSourceConnections() {
        playStoreBillingClient.startConnection(billingClientStateListener)
    }

    override fun endDataSourceConnections() {
        playStoreBillingClient.endConnection()
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
                scope.launch {
                    processPurchases(purchases.toHashSet())
                }
            }
        }
    }

    private suspend fun processPurchases(purchasesResult: Set<Purchase>) {
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
                        _licenseState.value = LicenseState.PENDING
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

    override fun verifyPurchase(data: String, signature: String?): Boolean {
        val s = signature ?: return false
        return Security.verifyPurchase(
            data,
            s,
            PUBLIC_KEY_BASE64,
        )
    }


    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchase] inside your app.
     */
    private suspend fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: Set<Purchase>) {
        if (nonConsumables.isEmpty() || !nonConsumables.any { clientData.proProductId in it.products }) {
            if (_licenseState.value == LicenseState.VALID) {
                _licenseState.value = LicenseState.NO_LICENSE
                clientData.setReceipt(null, null)
            }
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
                            _licenseState.value = LicenseState.VALID
                        }
                    }

                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        if (clientData.proProductId in purchase.products) {
                            _licenseState.value = LicenseState.NO_LICENSE
                            scope.launch {
                                clientData.setReceipt(null, null)
                            }
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
            _proProductDetails.value = null
            _licenseState.value = LicenseState.NO_LICENSE
//            clientData.setReceipt(null, null)
            null
        } else null
    }


    private fun fetchProductDetails(
        productId: String,
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

        playStoreBillingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsResult ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    proProductDetailsList = productDetailsResult.productDetailsList
                    findProProduct()?.let {
                        _proProductDetails.value =
                            MyProductDetails(
                                it.productId,
                                it.title,
                                it.name,
                                it.description,
                                it.oneTimePurchaseOfferDetails!!.formattedPrice
                            )
                    }
                }

                else -> {
                    Logger.d(TAG) { billingResult.debugMessage }
                }
            }
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [onPurchasesUpdated]
     */
    override fun launchBillingFlow(purchaseMethod: PurchaseMethod, activity: Any) {
        if (purchaseMethod.link == null) {
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

                playStoreBillingClient.launchBillingFlow(activity as Activity, flowParams)
            }
        }
    }

    override suspend fun checkAndStoreLicense(receipt: String) {
    }
}

