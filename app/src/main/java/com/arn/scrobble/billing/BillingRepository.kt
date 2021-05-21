package com.arn.scrobble.billing

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Tokens
import com.arn.scrobble.pref.MultiPreferences
import java.util.*


class BillingRepository private constructor(private val application: Application) :
        PurchasesUpdatedListener, BillingClientStateListener {

    // Breaking change in billing v4: callbacks don't run on main thread, always use ld.postValue()

    private lateinit var playStoreBillingClient: BillingClient
    private val pref by lazy { MultiPreferences(application) }
    val proStatusLd by lazy { MutableLiveData(pref.getBoolean(Stuff.PREF_PRO_STATUS, false)) }
    val proPendingSinceLd by lazy { MutableLiveData(0L) }
    val proSkuDetailsLd by lazy {
        val json = pref.getString(Stuff.PREF_PRO_SKU_JSON, null)
        val ld = MutableLiveData<SkuDetails>()
        if (json != null) {
            val skud = SkuDetails(json)
            if (skud.sku == Tokens.PRO_SKU_NAME)
                ld.value = skud
        }
        ld
    }

    fun startDataSourceConnections() {
        playStoreBillingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases() // required or app will crash
            .setListener(this)
            .build()
        connectToPlayBillingService()
    }

    fun endDataSourceConnections() {
        playStoreBillingClient.endConnection()
    }

    private fun connectToPlayBillingService(): Boolean {
        if (!playStoreBillingClient.isReady) {
            playStoreBillingClient.startConnection(this)
            return true
        }
        return false
    }

    /**
     * This is the callback for when connection to the Play [BillingClient] has been successfully
     * established. It might make sense to get [SkuDetails] and [Purchases][Purchase] at this point.
     */
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                querySkuDetailsAsync(BillingClient.SkuType.INAPP, INAPP_SKUS)
                queryPurchasesAsync()
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Log.d(LOG_TAG, "onBillingSetupFinished BILLING_UNAVAILABLE")
                //Some apps may choose to make decisions based on this knowledge.
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
        connectToPlayBillingService()
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
     * [BillingClient.acknowledgePurchaseAsync] is called. You should keep receipts in the local
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
        playStoreBillingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) {
            billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases.toHashSet())
            }
        }
    }

    private fun processPurchases(purchasesResult: Set<Purchase>) {
                val validPurchases = HashSet<Purchase>(purchasesResult.size)
                purchasesResult.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (Security.verifyPurchase(application, purchase)) {
                            validPurchases.add(purchase)
                        }
                    } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                        if (Tokens.PRO_SKU_NAME in purchase.skus) {
                            Log.d(
                                LOG_TAG,
                                "Received a pending purchase from " + Stuff.myRelativeTime(
                                    application,
                                    purchase.purchaseTime
                                )
                            )
                            proPendingSinceLd.postValue(purchase.purchaseTime)
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
//                Log.d(LOG_TAG, "processPurchases purchases in the lcl db ${testing.size}")
//                localCacheBillingClient.purchaseDao().insert(*validPurchases.toTypedArray())
                acknowledgeNonConsumablePurchasesAsync(validPurchases)
        }


    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchaseAsync] inside your app.
     */
    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: Set<Purchase>) {
        if (nonConsumables.isEmpty() || !nonConsumables.any { Tokens.PRO_SKU_NAME in it.skus  }) {
            revokePro()
        }

        nonConsumables.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase
                    .purchaseToken).build()
            playStoreBillingClient.acknowledgePurchase(params) { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        disburseNonConsumableEntitlement(purchase)
                    }
                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        if (Tokens.PRO_SKU_NAME in purchase.skus) {
                            revokePro()
                        }
                    }
                    else -> {
                        Log.d(LOG_TAG, "acknowledgeNonConsumablePurchasesAsync response is ${billingResult.debugMessage}")
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
        if (Tokens.PRO_SKU_NAME in purchase.skus) {
            pref.putBoolean(Stuff.PREF_PRO_STATUS, true)
            if (proStatusLd.value != true) {
                proStatusLd.postValue(true)
                application.sendBroadcast(Intent(NLService.iTHEME_CHANGED))
            }
            proPendingSinceLd.postValue(0)
        }
    }

    private fun revokePro() {
        pref.putBoolean(Stuff.PREF_PRO_STATUS, false)
        if (proStatusLd.value != false)
            proStatusLd.postValue(false)
    }

    private fun findProSku(skudList: List<SkuDetails>?): SkuDetails? {
        val skud = skudList?.firstOrNull { it.sku == Tokens.PRO_SKU_NAME }
        return if (skud != null && application.getString(R.string.app_name) in skud.title &&
            application.getString(R.string.app_name) in skud.description &&
            skud.sku != skud.title && skud.sku != skud.description
        )
            skud
        else if (skud != null) {
            pref.remove(Stuff.PREF_PRO_SKU_JSON)
            proSkuDetailsLd.postValue(null)
            revokePro()
            null
        }
        else null
    }

    /**
     * Presumably a set of SKUs has been defined on the Google Play Developer Console. This
     * method is for requesting a (improper) subset of those SKUs. Hence, the method accepts a list
     * of product IDs and returns the matching list of SkuDetails.
     *
     * The result is passed to [onSkuDetailsResponse]
     */
    private fun querySkuDetailsAsync(
            @BillingClient.SkuType skuType: String,
            skuList: List<String>) {
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(skuType)
            .build()
        playStoreBillingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    findProSku(skuDetailsList)?.let {
                        pref.putString(Stuff.PREF_PRO_SKU_JSON, it.originalJson)
                        proSkuDetailsLd.postValue(it)
                    }
                }
                else -> {
                    Log.e(LOG_TAG, billingResult.debugMessage)
                }
            }
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [onPurchasesUpdated]
     */
    fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        val skud = findProSku(listOf(skuDetails))
        if (skud != null) {
            val purchaseParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skud)
                .build()
            playStoreBillingClient.launchBillingFlow(activity, purchaseParams)
        }
    }

    /**
     * This method is called by the [playStoreBillingClient] when new purchases are detected.
     * The purchase list in this method is not the same as the one in
     * [queryPurchases][BillingClient.queryPurchases]. Whereas queryPurchases returns everything
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
                connectToPlayBillingService()
            }
            else -> {
                Log.i(LOG_TAG, billingResult.debugMessage)
            }
        }
    }

    companion object {
        private const val LOG_TAG = Stuff.TAG
        private val INAPP_SKUS = listOf(Tokens.PRO_SKU_NAME)

        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(application: Application): BillingRepository =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: BillingRepository(application)
                                    .also { INSTANCE = it }
                }
    }
}

